package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.FaultType;
import FireFightingDroneSwarm.Events.EventLogger;
import FireFightingDroneSwarm.FireIncidentSubsystem.*;
import FireFightingDroneSwarm.DroneSubsystem.DroneStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the Scheduler class, focusing on task buffering,
 * zone calculations, and UDP communication.
 */
class SchedulerTest {
    private EventLogger logger;

    /**
     * Initializes a shared EventLogger before each test to prevent NullPointerExceptions.
     */
    @BeforeEach
    void setUp() {
        logger = new EventLogger(1000);
    }

    /**
     * Tests the getZoneCenter() method.
     * Verifies that:
     * - an exception is thrown if zone data has not been initialized
     * - the center of a valid zone is calculated correctly
     * - an exception is thrown for an invalid zone ID
     */
    @Test
    void TestGetZoneCenter() {
        Scheduler s = new Scheduler(10, logger);
        assertThrows(IllegalStateException.class, () -> s.getZoneCenter(1));

        Map<Integer, Zone> zoneMap = new HashMap<>();
        zoneMap.put(1, new Zone(1, new int[]{0, 0}, new int[]{700, 600}));
        s.setZoneIDs(zoneMap);

        double[] center = s.getZoneCenter(1);
        assertEquals(350.0, center[0]);
        assertEquals(300.0, center[1]);

        assertThrows(IllegalArgumentException.class, () -> s.getZoneCenter(999));
    }

    /**
     * Tests the Scheduler get() method when all tasks have been sent.
     * Verifies that:
     * - the first get() returns the queued event
     * - the second get() returns null once the buffer is empty
     * - the Scheduler marks all tasks as processed when both flags are true
     */
    @Test
    void TestGet() throws Exception {
        Scheduler scheduler = new Scheduler(5, logger);

        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0, 0}, new int[]{10, 10}));
        scheduler.setZoneIDs(zones);

        FireEvent e1 = new FireEvent(1, TaskType.FIRE_DETECTED, LocalTime.of(13, 0), Severity.LOW, FaultType.NONE, 1);
        scheduler.put(e1);
        scheduler.setAllTasksSent(true);

        // First get() should return the actual event put into the buffer
        FireEvent first = scheduler.get();
        assertNotNull(first);
        assertEquals(1, first.getFireID());

        // Now buffer is empty AND allTasksSent is true → should return null and set processed flag
        FireEvent second = scheduler.get();
        assertNull(second);
        assertTrue(scheduler.getAllTasksProcessed());
    }

    @Test
    void testSetAllTasksSentUpdate() {
        Scheduler s = new Scheduler(5, logger);
        s.setAllTasksSent(true);
        assertTrue(s.getAllTasksSent());
    }

    @Test
    void testZoneDistanceCalc() {
        Scheduler s = new Scheduler(10, logger);
        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0,0}, new int[]{6,8}));
        s.setZoneIDs(zones);

        assertEquals(5.0, s.calculateZoneDistance(1, 0), 1e-9);
    }
}