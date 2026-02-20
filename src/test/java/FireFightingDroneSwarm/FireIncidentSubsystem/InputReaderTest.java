package FireFightingDroneSwarm.FireIncidentSubsystem;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class InputReaderTest {

    @Test
    void TestParseCoordinates() {
        InputReader r = new InputReader("sample_event_file.csv", "sample_zone_file.csv");

        assertArrayEquals(new int[]{700, 600}, r.parseCoordinates("(700;600)"));
        assertArrayEquals(new int[]{0, 0}, r.parseCoordinates("(0;0)"));
        assertArrayEquals(new int[]{-5, 12}, r.parseCoordinates("(-5;12)"));
    }

    @Test
    void TestParseZoneFiles() {
        InputReader r = new InputReader("sample_event_file.csv", "sample_zone_file.csv");

        ArrayList<Zone> zones = r.parseZoneFile();
        assertNotNull(zones);
        assertFalse(zones.isEmpty(), "Zones list should not be empty");

        // Basic sanity: IDs should be positive and coords arrays size 2
        for (Zone z : zones) {
            assertTrue(z.getID() > 0);
            assertEquals(2, z.getStartCoordinates().length);
            assertEquals(2, z.getEndCoordinates().length);
        }
    }

    @Test
    void GetParseEventFile() {
        InputReader r = new InputReader("sample_event_file.csv", "sample_zone_file.csv");

        ArrayList<FireEvent> events = r.parseEventFile();
        assertNotNull(events);
        assertFalse(events.isEmpty(), "Events list should not be empty");

        for (FireEvent e : events) {
            assertTrue(e.getZoneID() > 0);
            assertNotNull(e.getTimestamp());
            assertNotNull(e.getSeverity());
            assertNotNull(e.getTaskType(), "TaskType should not be null (check trimming/parsing)");
        }
    }

}