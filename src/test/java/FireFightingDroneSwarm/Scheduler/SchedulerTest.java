package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerTest {

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

    @Test
    void testBlockedZones() throws InterruptedException, ExecutionException, TimeoutException {
        Scheduler scheduler = new Scheduler(1);

        FireEvent e1 = new FireEvent(1, TaskType.FIRE_DETECTED, LocalTime.of(13, 0), Severity.LOW);
        FireEvent e2 = new FireEvent(2, TaskType.FIRE_DETECTED, LocalTime.of(13, 1), Severity.MODERATE);

        scheduler.put(e1); // buffer full

        java.util.concurrent.atomic.AtomicBoolean putFinished = new java.util.concurrent.atomic.AtomicBoolean(false);

        Thread t = new Thread(() -> {
            try {
                scheduler.put(e2); // should block until get() happens
                putFinished.set(true);
            } catch (InterruptedException ignored) {}
        });

        t.start();

        // Give the thread a moment to try putting (it should be blocked)
        Thread.sleep(100);
        assertFalse(putFinished.get(), "put(e2) should be blocked while buffer is full");

        // Now free space
        FireEvent got = scheduler.get();
        assertEquals(1, got.getZoneID());

        // Now it should finish
        t.join(500);
        assertTrue(putFinished.get(), "put(e2) should finish after get() frees space");

        // And e2 should now be next
        FireEvent got2 = scheduler.get();
        assertEquals(2, got2.getZoneID());
    }

    @Test
    void TestGet() throws Exception {
        Scheduler scheduler = new Scheduler(5);

        // Put one event so get() can actually remove it (and then see buffer empty)
        FireEvent e1 = new FireEvent(1, TaskType.FIRE_DETECTED, LocalTime.of(13, 0), Severity.LOW);
        scheduler.put(e1);

        // Now declare no more tasks will arrive
        scheduler.setAllTasksSent(true);

        // First get returns the event
        FireEvent first = scheduler.get();
        assertNotNull(first);
        assertEquals(1, first.getZoneID());

        // Now buffer is empty AND allTasksSent is true → scheduler marks processed
        FireEvent second = scheduler.get();
        assertNull(second);
        assertTrue(scheduler.getAllTasksProcessed());
    }

    @Test
    void setAllTasksSent_updatesFlag() {
        Scheduler s = new Scheduler(5);
        s.setAllTasksSent(true);
        assertTrue(s.getAllTasksSent());
    }

    @Test
    void setDrone_doesNotThrow() {
        Scheduler s = new Scheduler(5);
        s.setDrone(null); // should not crash
    }


}