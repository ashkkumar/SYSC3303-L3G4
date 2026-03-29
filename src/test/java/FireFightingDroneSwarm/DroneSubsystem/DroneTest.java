package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.Events.EventLogger;
import FireFightingDroneSwarm.FireIncidentSubsystem.*;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Drone functionality, including state transitions,
 * fault handling, and UDP status reporting.
 */
class DroneTest {
    private static EventLogger logger;

    /**
     * Sets up a static logger used across all drone tests.
     */
    @BeforeAll
    static void setupLogger() {
        logger = new EventLogger(1000);
    }

    /**
     * Test drone that disables real sleeping so tests run instantly.
     */
    static class TestDrone extends Drone {
        public TestDrone(int id) {
            super(id, null, null, logger);
        }

        @Override
        protected void sleep(int ms) {
            // Overridden to return immediately
        }
    }

    /**
     * Verifies that the overridden sleep method in TestDrone returns immediately.
     */
    @Test
    void testSleep() {
        TestDrone drone = new TestDrone(1);
        long start = System.currentTimeMillis();
        drone.sleep(2000);
        long end = System.currentTimeMillis();

        // Should take effectively 0ms if overridden
        assertTrue(end - start < 100, "Sleep should be overridden and return immediately");
    }

    @Test
    void testGetStatus() {
        Drone drone = new Drone(1, null, null, logger);
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testTransition() {
        Drone drone = new Drone(1, null, null, logger);
        // Illegal move: IDLE cannot jump straight to ARRIVED
        drone.transition(DroneStatus.ARRIVED);
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testNormalMissionCompletesSuccessfully() throws Exception {
        TestDrone drone = new TestDrone(4);
        drone.currentTask = new FireEvent(5, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.LOW, FaultType.NONE, 11);
        drone.zoneCenter = new double[]{200.0, 0.0};

        drone.executeTask();

        assertEquals(DroneStatus.IDLE, drone.getStatus());
        assertEquals(80, drone.getWaterTank()); // Assuming 20 units consumed
    }
}