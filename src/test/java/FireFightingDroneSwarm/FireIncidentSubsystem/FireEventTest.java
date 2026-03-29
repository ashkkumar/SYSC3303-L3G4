package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.DroneSubsystem.FaultType;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FireEventTest {

    /**
     * Tests that getZoneID() returns the correct zone identifier
     * provided when the FireEvent object is created.
     */
    @Test
    void TestGetZoneID() {
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED,
                LocalTime.of(14, 3, 15), Severity.HIGH, FaultType.NONE, 4);

        assertEquals(3, e.getZoneID());
    }

    /**
     * Tests that getTaskType() returns the correct TaskType
     * associated with the FireEvent.
     */
    @Test
    void TestGetTaskType() {
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED,
                LocalTime.of(14, 3, 15), Severity.HIGH, FaultType.NONE, 5);

        assertEquals(TaskType.FIRE_DETECTED, e.getTaskType());
    }

    /**
     * Tests that getTimestamp() returns the same LocalTime value
     * that was assigned to the FireEvent during construction.
     */
    @Test
    void TestGetTimestamp() {
        LocalTime t = LocalTime.of(14, 3, 15);
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED, t, Severity.HIGH, FaultType.NONE, 6);

        assertEquals(t, e.getTimestamp());
    }

    /**
     * Tests that getSeverity() returns the correct Severity level
     * associated with the FireEvent.
     */
    @Test
    void TestGetSeverity() {
        FireEvent e = new FireEvent(3, TaskType.FIRE_DETECTED,
                LocalTime.of(14, 3, 15), Severity.HIGH, FaultType.NONE, 7);

        assertEquals(Severity.HIGH, e.getSeverity());
    }

    /**
     * Tests that the toString() method returns a string containing
     * the key fields of the FireEvent including the zone ID,
     * task type, and severity level.
     */
    @Test
    void TestToString() {
        FireEvent e = new FireEvent(7, TaskType.DRONE_REQUESTED,
                LocalTime.of(14, 10), Severity.MODERATE, FaultType.NONE, 8);

        String s = e.toString();

        assertTrue(s.contains("zoneID=7"));
        assertTrue(s.contains("DRONE_REQUESTED"));
        assertTrue(s.contains("MODERATE"));
    }
}

