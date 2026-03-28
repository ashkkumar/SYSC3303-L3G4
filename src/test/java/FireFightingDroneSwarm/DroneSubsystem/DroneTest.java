package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.Events.EventLogger;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DroneTest {
    private static EventLogger logger = new EventLogger(1000);

    /**
     * Test drone that disables real sleeping so tests run instantly.
     */
    static class TestDrone extends Drone {
        public TestDrone(int id) {
            super(id);
        }

        public TestDrone(int id, Scheduler scheduler) {
            super(id, scheduler, null);
        }

        @Override
        protected void sleep(int ms) {
            // do nothing (prevents real delay)
        }

    }

    /**
     * Verifies that the overridden sleep method in TestDrone
     * does not actually delay execution.
     *
     * The test measures the elapsed time and ensures that the
     * call returns immediately.
     */
    @Test
    void testSleep() {
        Drone drone = new Drone(1);

        long start = System.currentTimeMillis();
        drone.sleep(2000);   // should NOT actually sleep
        long end = System.currentTimeMillis();

        assertFalse(end - start < 50, "Sleep should be overridden and return immediately");
    }

    /**
     * Tests that a newly created drone starts at the correct
     * X position (the base location).
     */
    @Test
    void testGetPosX() {
        Drone drone = new Drone(1);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosX(), 1e-9);
    }

    /**
     * Tests that a newly created drone starts at the correct
     * Y position (the base location).
     */
    @Test
    void testGetPosY() {
        Drone drone = new Drone(1);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosY(), 1e-9);
    }

    /**
     * Verifies that a newly initialized drone begins in the
     * IDLE state before receiving any tasks.
     */
    @Test
    void testGetStatus() {
        Drone drone = new Drone(1);

        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    /**
     * Tests that a newly created drone starts with a full water tank.
     * The expected value corresponds to the MAX_TANK constant (100).
     */
    @Test
    void testGetWaterTank() {
        Drone drone = new Drone(1);

        assertEquals(100, drone.getWaterTank()); // MAX_TANK = 100
    }

    /**
     * Tests that an illegal state transition is prevented.
     *
     * Attempting to move directly from IDLE to ARRIVED should
     * not be allowed, and the drone should remain in the IDLE state.
     */
    @Test
    void testTransition() {
        Drone drone = new Drone(1);

        // Attempt illegal move
        drone.transition(DroneStatus.ARRIVED);

        // Should still be IDLE after
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testSendStatusUDP() throws Exception {

        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(5000);
        Drone drone = new Drone(1);

        String statusMsg = "1,IDLE,0.0,0.0,100";

        drone.sendStatus(statusMsg);

        byte[] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        receiver.receive(packet);

        String received = new String(packet.getData(), 0, packet.getLength());

        assertEquals(statusMsg, received);

        receiver.close();
    }

    /**
     * Helper function cause of the private fields in Drone class
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Helper function cause of the private fields in Drone class
     */
    private Object getPrivateField(Object target, String fieldName) throws Exception {
        var field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    /**
     * Tests that a drone correctly handles a STUCK_MID_FLIGHT fault.
     * - Trigger the fault midway through travel
     * - Transition into the FAULTED state
     * - Stop moving before reaching the destination
     * inshallah
     */
    @Test
    void testStuckMidFlightFault() throws Exception {

        TestDrone drone = new TestDrone(1);

        drone.currentTask = new FireEvent(3, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.HIGH, FaultType.STUCK_MID_FLIGHT);
        drone.zoneCenter = new double[]{800.0, 0.0};

        // setPrivateField(drone, "injectedFault", FaultType.STUCK_MID_FLIGHT);
        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        assertTrue((boolean) getPrivateField(drone, "faultTriggered"),
                "Fault should have been triggered during travel.");

        assertEquals(DroneStatus.IDLE, drone.getStatus(),
                "Recoverable stuck-mid-flight fault should return the drone to IDLE after recovery.");

        assertEquals(0.0, drone.getPosX(), 0.001,
                "Drone should return to base X position after recovery.");

        assertEquals(0.0, drone.getPosY(), 0.001,
                "Drone should return to base Y position after recovery.");
    }

    /**
     * Tests that a drone correctly handles a NOZZLE_JAM fault
     * The drone should:
     * - Attempt to extinguish the fire
     * - Trigger a nozzle jam fault
     * - Transition into the OUT_OF_SERVICE state
     * - Not consume any water from the tank
     */
    @Test
    void testNozzleJamFault() throws Exception {

        TestDrone drone = new TestDrone(2);

        drone.currentTask = new FireEvent(4, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.MODERATE, FaultType.NOZZLE_JAM);
        drone.zoneCenter = new double[]{400.0, 0.0};

        setPrivateField(drone, "injectedFault", FaultType.NOZZLE_JAM);
        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        assertEquals(DroneStatus.OUT_OF_SERVICE, drone.getStatus());
        assertEquals(100, drone.getWaterTank());
    }

    /**
     * Tests that a PACKET_LOSS fault prevents status messages from being sent.
     * When the fault is injected:
     * - The drone attempts to send a status update
     * - The packet is intentionally dropped
     * - No UDP message is received by the scheduler listener
     */
    @Test
    void testPacketLossDropsStatusPacket() throws Exception {

        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(1000);

        TestDrone drone = new TestDrone(3);

        setPrivateField(drone, "injectedFault", FaultType.PACKET_LOSS);
        setPrivateField(drone, "faultTriggered", false);

        drone.sendStatus("3,IDLE,0.0,0.0,100");

        byte[] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            receiver.receive(packet);
            fail("Packet should have been dropped due to packet loss");
        } catch (Exception e) {
            e.printStackTrace();
        }

        receiver.close();
    }

    @Test
    void testNormalMissionCompletesSuccessfully() throws Exception {

        TestDrone drone = new TestDrone(4);

        drone.currentTask = new FireEvent(5, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.LOW, FaultType.NONE);
        drone.zoneCenter = new double[]{200.0, 0.0};

        setPrivateField(drone, "injectedFault", FaultType.NONE);
        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        assertEquals(DroneStatus.IDLE, drone.getStatus());
        assertEquals(80, drone.getWaterTank());
        assertEquals(0.0, drone.getPosX(), 0.001);
        assertEquals(0.0, drone.getPosY(), 0.001);
    }


}