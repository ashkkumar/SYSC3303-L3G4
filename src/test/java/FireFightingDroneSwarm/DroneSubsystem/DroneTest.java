package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.Events.LogManager;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class DroneTest {
    // LogManager might need a mock or a dummy instance
    private static LogManager dummyLogger = null;

    /**
     * Test drone that disables real sleeping so tests run instantly.
     */
    static class TestDrone extends Drone {
        public TestDrone(int id) {
            super(id);
        }

        public TestDrone(int id, Scheduler scheduler) {
            super(id, scheduler, null, null);
        }

        @Override
        protected void sleep(int ms) {
            // do nothing (prevents real delay)
        }
    }

    @Test
    void testSleep() {
        // Use TestDrone to verify the override works
        TestDrone drone = new TestDrone(1);

        long start = System.currentTimeMillis();
        drone.sleep(2000);
        long end = System.currentTimeMillis();

        // If sleep is overridden, it should take almost 0ms.
        // We assert it took less than 100ms.
        assertTrue((end - start) < 100, "Sleep should be overridden and return immediately");
    }

    @Test
    void testGetPosX() {
        Drone drone = new Drone(1);
        assertEquals(0.0, drone.getPosX(), 1e-9);
    }

    @Test
    void testGetPosY() {
        Drone drone = new Drone(1);
        assertEquals(0.0, drone.getPosY(), 1e-9);
    }

    @Test
    void testGetStatus() {
        Drone drone = new Drone(1);
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testGetWaterTank() {
        Drone drone = new Drone(1);
        assertEquals(15, drone.getWaterTank());
    }

    @Test
    void testTransition() {
        Drone drone = new Drone(1);
        // Attempt illegal move (IDLE -> ARRIVED is blocked by switch case)
        drone.transition(DroneStatus.ARRIVED);
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testSendStatusUDP() throws Exception {
        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(2000);

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
     * Helper to access private fields in Drone class
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getPrivateField(Object target, String fieldName) throws Exception {
        var field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    @Test
    void testStuckMidFlightFault() throws Exception {
        TestDrone drone = new TestDrone(1);

        // Setup task with fault
        drone.currentTask = new FireEvent(3, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.HIGH, FaultType.STUCK_MID_FLIGHT, 1);
        drone.zoneCenter = new double[]{800.0, 0.0};

        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        assertTrue((boolean) getPrivateField(drone, "faultTriggered"), "Fault should have been triggered.");
        // Per Drone.java logic: if STUCK_MID_FLIGHT hits, it transitions to FAULTED, sleeps, then returns false.
        // The executeTask() catch then sends it to RETURNING -> REFILLING -> IDLE.
        assertEquals(DroneStatus.IDLE, drone.getStatus());
        assertEquals(0.0, drone.getPosX(), 0.001);
    }

    @Test
    void testNozzleJamFault() throws Exception {
        TestDrone drone = new TestDrone(2);

        drone.currentTask = new FireEvent(4, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.MODERATE, FaultType.NOZZLE_JAM, 1);
        drone.zoneCenter = new double[]{400.0, 0.0};

        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        // Nozzle Jam leads to OUT_OF_SERVICE in extinguish()
        assertEquals(DroneStatus.OUT_OF_SERVICE, drone.getStatus());
        assertEquals(15, drone.getWaterTank(), "Water should not be consumed if nozzle is jammed.");
    }

    @Test
    void testPacketLossDropsStatusPacket() throws Exception {
        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(1000);

        TestDrone drone = new TestDrone(3);
        drone.currentTask = new FireEvent(3, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.LOW, FaultType.PACKET_LOSS, 3);

        setPrivateField(drone, "injectedFault", FaultType.PACKET_LOSS);

        // This call should trigger the fault check and return early
        drone.sendStatus("3,IDLE,0.0,0.0,100");

        byte[] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        assertThrows(java.net.SocketTimeoutException.class, () -> {
            receiver.receive(packet);
        }, "Packet should have been dropped due to packet loss fault");

        receiver.close();
    }

    @Test
    void testNormalMissionCompletesSuccessfully() throws Exception {
        TestDrone drone = new TestDrone(4);

        drone.currentTask = new FireEvent(5, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.LOW, FaultType.NONE, 1);
        drone.zoneCenter = new double[]{200.0, 0.0};

        drone.executeTask();

        assertEquals(DroneStatus.IDLE, drone.getStatus());
        // 100 - 20 (LOW severity) + Refill = 100
        assertEquals(15, drone.getWaterTank());
        assertEquals(0.0, drone.getPosX(), 0.001);
    }
}