package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DroneTest {

    /**
     * Test drone that disables real sleeping so tests run instantly.
     */
    static class TestDrone extends Drone {
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
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        long start = System.currentTimeMillis();
        drone.sleep(2000);   // should NOT actually sleep
        long end = System.currentTimeMillis();

        assertTrue(end - start < 50, "Sleep should be overridden and return immediately");
    }

    /**
     * Tests that a newly created drone starts at the correct
     * X position (the base location).
     */
    @Test
    void testGetPosX() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosX(), 1e-9);
    }

    /**
     * Tests that a newly created drone starts at the correct
     * Y position (the base location).
     */
    @Test
    void testGetPosY() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosY(), 1e-9);
    }

    /**
     * Verifies that a newly initialized drone begins in the
     * IDLE state before receiving any tasks.
     */
    @Test
    void testGetStatus() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    /**
     * Tests that a newly created drone starts with a full water tank.
     * The expected value corresponds to the MAX_TANK constant (100).
     */
    @Test
    void testGetWaterTank() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        assertEquals(100, drone.getWaterTank()); // MAX_TANK = 100
    }

    /**
     * Tests that a drone can successfully execute a fire task.
     *
     * The test creates a simple zone, assigns a FireEvent task
     * to the drone, and verifies that the drone completes the
     * task and returns to the IDLE state.
     */
    @Test
    void testExecuteTask() throws Exception {

        Scheduler scheduler = new Scheduler(10);

        // Create a simple zone so getZoneCenter works
        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0,0}, new int[]{10,10}));
        scheduler.setZoneIDs(zones);

        TestDrone drone = new TestDrone(1, scheduler);

        // Inject a task
        drone.currentTask = new FireEvent(
                1,
                TaskType.FIRE_DETECTED,
                LocalTime.of(13,0),
                Severity.LOW
        );

        drone.executeTask();

        // Final state should be IDLE
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    /**
     * Tests that an illegal state transition is prevented.
     *
     * Attempting to move directly from IDLE to ARRIVED should
     * not be allowed, and the drone should remain in the IDLE state.
     */
    @Test
    void testTransition() {

        Scheduler scheduler = new Scheduler(5);
        Drone drone = new Drone(1, scheduler, null);

        // Attempt illegal move
        drone.transition(DroneStatus.ARRIVED);

        // Should still be IDLE after
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Nested
    class UdpTests {
        private DatagramSocket testReceiverSocket;
        private Drone drone;

        @BeforeEach
        void setup() throws SocketException {
            testReceiverSocket = new DatagramSocket(5000);
            testReceiverSocket.setSoTimeout(1000);
            Scheduler scheduler = new Scheduler(5);
            drone = new Drone(1, scheduler, null);
        }

        @AfterEach
        void tearDown() {
            testReceiverSocket.close();
        }

        @Test
        void testSendStatus() throws IOException {
            String testStatus = "FLYING";
            drone.sendStatus(testStatus);
            byte[] buffer = new byte[1024];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);

            // 3. Capture the packet
            testReceiverSocket.receive(receivedPacket);
            String receivedData = new String(receivedPacket.getData(), 0, receivedPacket.getLength());

            // 4. Assertions
            assertEquals(testStatus, receivedData);
        }
    }
}