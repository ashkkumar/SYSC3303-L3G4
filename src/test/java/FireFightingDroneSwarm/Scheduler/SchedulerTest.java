package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.FaultType;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.DroneSubsystem.DroneStatus;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

    /**
     * Tests the getZoneCenter() method
     * Verifies that:
     * - an exception is thrown if zone data has not been initialized
     * - the center of a valid zone is calculated correctly
     * - an exception is thrown for an invalid zone ID
     */
    @Test
    void TestGetZoneCenter() {
        Scheduler s = new Scheduler(10);
        assertThrows(IllegalStateException.class, () -> s.getZoneCenter(1));

        Scheduler s1 = new Scheduler(10);

        Map<Integer, Zone> zoneMap = new HashMap<>();
        zoneMap.put(1, new Zone(1, new int[]{0, 0}, new int[]{700, 600}));
        s.setZoneIDs(zoneMap);

        double[] center = s.getZoneCenter(1);
        assertEquals(350.0, center[0]);
        assertEquals(300.0, center[1]);

        Scheduler s2 = new Scheduler(10);

        Map<Integer, Zone> zoneMap2 = new HashMap<>();
        zoneMap2.put(1, new Zone(1, new int[]{0, 0}, new int[]{700, 600}));
        s.setZoneIDs(zoneMap);

        assertThrows(IllegalArgumentException.class, () -> s.getZoneCenter(999));
    }

    /**
     * Tests the Scheduler get() method when all tasks have been sent.
     * Verifies that:
     * - the first get() returns the queued event
     * - the second get() returns null once the buffer is empty
     * - the Scheduler marks all tasks as processed when appropriate
     *
     * @throws Exception if an unexpected error occurs during the test
     */
    @Test
    void TestGet() throws Exception {
        Scheduler scheduler = new Scheduler(5);

        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0, 0}, new int[]{10, 10}));
        scheduler.setZoneIDs(zones);

        // Put one event so get() can actually remove it (and then see buffer empty)
        FireEvent e1 = new FireEvent(1, TaskType.FIRE_DETECTED, LocalTime.of(13, 0), Severity.LOW, FaultType.NONE, 1);
        scheduler.put(e1);

        // Now declare no more tasks will arrive
        scheduler.setAllTasksSent(true);

        // First get returns the event
        FireEvent first = scheduler.get();
        assertNotNull(first);

        // Now buffer is empty AND allTasksSent is true → scheduler marks processed
        FireEvent second = scheduler.get();
        assertNull(second);
        assertTrue(scheduler.getAllTasksProcessed());
    }

    /**
     * Tests that setAllTasksSent(true) correctly updates the internal flag.
     */
    @Test
    void testSetAllTasksSentUpdate() {
        Scheduler s = new Scheduler(5);
        s.setAllTasksSent(true);
        assertTrue(s.getAllTasksSent());
    }

    /**
     * Tests the calculateZoneDistance() method.
     * Verifies that the Scheduler correctly computes the Euclidean distance
     * from the base location to the center of a zone.
     */
    @Test
    void testZoneDistanceCalc() {
        Scheduler s = new Scheduler(10);

        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0,0}, new int[]{6,8})); // center (3,4)
        s.setZoneIDs(zones);

        // distance from base (0,0) to (3,4) = 5
        assertEquals(5.0, s.calculateZoneDistance(1, 0), 1e-9);
    }

    /**
     * Test that the scheduler correctly sends a packet to a drone.
     * @throws Exception upon socket timeout
     */
    @Test
    void testAssignDroneEvenSendsUDPPacket() throws Exception {
        Scheduler scheduler = new Scheduler(10);

        // Setup zones
        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0, 0}, new int[]{100, 100}));
        zones.put(2, new Zone(2, new int[]{200, 200}, new int[]{300, 300}));
        scheduler.setZoneIDs(zones);

        // Setup mock drone listener socket
        DatagramSocket testDroneSocket = new DatagramSocket(0); // random free port
        int dronePort = testDroneSocket.getLocalPort();

        DroneState drone = new DroneState(
                1,
                DroneStatus.IDLE,
                0, 0,
                100,
                InetAddress.getLocalHost(),
                dronePort
        );

        scheduler.addDroneState(1, drone);
        FireEvent event = new FireEvent(
                1,
                TaskType.FIRE_DETECTED,
                LocalTime.now(),
                Severity.HIGH,
                FaultType.NONE,
                1
        );
        scheduler.put(event);
        scheduler.assignDroneEvent();

        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        testDroneSocket.setSoTimeout(2000);
        testDroneSocket.receive(packet);

        // Verify packet contents
        assertNotNull(packet);
        assertEquals(3, packet.getData()[0]); // message type 3 = assignment
        assertEquals(1, packet.getData()[1]); // zoneID
        assertEquals(Severity.HIGH.ordinal(), packet.getData()[2]);

        testDroneSocket.close();
    }
}