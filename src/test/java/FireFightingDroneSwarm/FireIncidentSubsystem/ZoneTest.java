package FireFightingDroneSwarm.FireIncidentSubsystem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZoneTest {

    /**
     * Tests that the getID() method returns the correct zone ID
     * that was provided during object construction.
     */
    @Test
    void getID() {
        Zone z = new Zone(1, new int[]{0, 0}, new int[]{700, 600});
        assertEquals(1, z.getID());
    }

    /**
     * Tests that the getStartCoordinates() method returns the
     * correct start coordinate array for the zone.
     */
    @Test
    void TestGetStartCoordinates() {
        Zone z = new Zone(1, new int[]{0, 0}, new int[]{700, 600});
        assertArrayEquals(new int[]{0, 0}, z.getStartCoordinates());
    }

    /**
     * Tests that the getEndCoordinates() method returns the
     * correct end coordinate array for the zone.
     */
    @Test
    void TestGetEndCoordinates() {
        Zone z = new Zone(1, new int[]{0, 0}, new int[]{700, 600});
        assertArrayEquals(new int[]{700, 600}, z.getEndCoordinates());
    }

    /**
     * Tests that the toString() method includes the correct zone ID
     * in the generated string representation.
     */
    @Test
    void TestToString() {
        Zone z = new Zone(2, new int[]{0, 600}, new int[]{650, 1500});
        String s = z.toString();
        assertTrue(s.contains("Zone ID 2"));
    }

    /**
     * Tests that the toString() method includes the correct start
     * coordinates in the string representation.
     */
    @Test
    void TestToStringStartCoordinates() {
        Zone z = new Zone(2, new int[]{0, 600}, new int[]{650, 1500});
        String s = z.toString();
        assertTrue(s.contains("[0, 600]"));
    }

    /**
     * Tests that the toString() method includes the correct end
     * coordinates in the string representation.
     */
    @Test
    void TestToStringEndCoordinates() {
        Zone z = new Zone(2, new int[]{0, 600}, new int[]{650, 1500});
        String s = z.toString();
        assertTrue(s.contains("[650, 1500]"));
    }

}