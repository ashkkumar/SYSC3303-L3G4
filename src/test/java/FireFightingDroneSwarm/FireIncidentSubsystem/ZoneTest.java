package FireFightingDroneSwarm.FireIncidentSubsystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZoneTest {

    @Test
    void getID() {
        Zone z = new Zone(1, new int[]{0, 0}, new int[]{700, 600});
        assertEquals(1, z.getID());
    }

    @Test
    void TestGetStartCoordinates() {
        Zone z = new Zone(1, new int[]{0, 0}, new int[]{700, 600});
        assertArrayEquals(new int[]{0, 0}, z.getStartCoordinates());
    }

    @Test
    void TestGetEndCoordinates() {
        Zone z = new Zone(1, new int[]{0, 0}, new int[]{700, 600});
        assertArrayEquals(new int[]{700, 600}, z.getEndCoordinates());
    }

    @Test
    void TestToString() {
        Zone z = new Zone(2, new int[]{0, 600}, new int[]{650, 1500});
        String s = z.toString();
        assertTrue(s.contains("Zone ID 2"));
    }

    @Test
    void TestToStringStartCoordinates() {
        Zone z = new Zone(2, new int[]{0, 600}, new int[]{650, 1500});
        String s = z.toString();
        assertTrue(s.contains("[0, 600]"));
    }

    @Test
    void TestToStringEndCoordinates() {
        Zone z = new Zone(2, new int[]{0, 600}, new int[]{650, 1500});
        String s = z.toString();
        assertTrue(s.contains("[650, 1500]"));
    }

}