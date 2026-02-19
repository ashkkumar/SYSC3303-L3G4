package FireFightingDroneSwarm.FireIncidentSubsystem;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FireEventTest {

    @Test
    void TestGetZoneID() {
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED,
                LocalTime.of(14, 3, 15), Severity.HIGH);

        assertEquals(3, e.getZoneID());
    }

    @Test
    void TestGetTaskType() {
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED,
                LocalTime.of(14, 3, 15), Severity.HIGH);

        assertEquals(TaskType.FIRE_DETECTED, e.getTaskType());
    }

    @Test
    void TestGetTimestamp() {
        LocalTime t = LocalTime.of(14, 3, 15);
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED, t, Severity.HIGH);

        assertEquals(t, e.getTimestamp());
    }

    @Test
    void TestGetSeverity() {
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED,
                LocalTime.of(14, 3, 15), Severity.HIGH);

        assertEquals(Severity.HIGH, e.getSeverity());
    }

    @Test
    void TestToString() {
        FireEvent e = new FireEvent(7, TaskType.DRONE_REQUESTED,
                LocalTime.of(14, 10), Severity.MODERATE);

        String s = e.toString();

        assertTrue(s.contains("zoneID=7"));
        assertTrue(s.contains("DRONE_REQUESTED"));
        assertTrue(s.contains("MODERATE"));
    }
}

