package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.DroneSubsystem.FaultType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IncidentReporter}.
 * Validates UDP packet serialization, coordinate splitting, and zone lookups.
 */
class IncidentReporterTest {

    /**
     * Tests that the separateBytes() helper correctly splits
     * a two-byte integer value into high and low bytes for network transmission.
     * * @see IncidentReporter#separateBytes(byte[], int, int)
     */
    @Test
    void testSeparateBytes() throws Exception {
        // Using sample files provided in project structure
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_multiple.csv", "sample_zone_multiple.csv")
        );

        byte[] data = new byte[2];
        Method separateBytes = IncidentReporter.class.getDeclaredMethod(
                "separateBytes", byte[].class, int.class, int.class
        );
        separateBytes.setAccessible(true);

        int testValue = 1600;
        separateBytes.invoke(reporter, data, 0, testValue);

        // Verify Big-Endian split
        assertEquals((byte) (testValue >> 8), data[0], "High byte should match bits 8-15");
        assertEquals((byte) (testValue & 0xFF), data[1], "Low byte should match bits 0-7");
    }

    /**
     * Tests that getZoneById() returns the correct Zone object from the parsed input.
     * Requires the InputReader to have successfully loaded zone ID 3.
     */
    @Test
    void testGetZoneById() throws Exception {
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_multiple.csv", "sample_zone_multiple.csv")
        );

        Method getZoneById = IncidentReporter.class.getDeclaredMethod("getZoneById", int.class);
        getZoneById.setAccessible(true);

        // This assumes zone 3 exists in your sample_zone_multiple.csv
        Zone zone = (Zone) getZoneById.invoke(reporter, 3);

        assertNotNull(zone, "Zone object should be found in the internal list");
        assertEquals(3, zone.getID(), "Returned zone ID must match requested ID");
    }

    /**
     * Verifies that getZoneById() returns null safely when an invalid ID is requested.
     */
    @Test
    void testGetZoneByIdInvalid() throws Exception {
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_multiple.csv", "sample_zone_multiple.csv")
        );

        Method getZoneById = IncidentReporter.class.getDeclaredMethod("getZoneById", int.class);
        getZoneById.setAccessible(true);

        Zone zone = (Zone) getZoneById.invoke(reporter, 999);
        assertNull(zone, "Requesting a non-existent zone ID should return null");
    }

    /**
     * Tests that allEventsSent() sends a single-byte UDP packet with message type 2.
     * * @see IncidentReporter#allEventsSent()
     */
    @Test
    void testAllEventsSentPacket() throws Exception {
        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(2000);

        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_multiple.csv", "sample_zone_multiple.csv")
        );

        Method allEventsSent = IncidentReporter.class.getDeclaredMethod("allEventsSent");
        allEventsSent.setAccessible(true);

        allEventsSent.invoke(reporter);

        byte[] buf = new byte[16];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        receiver.receive(packet);

        assertEquals(1, packet.getLength(), "Termination packet should be exactly 1 byte");
        assertEquals(2, buf[0], "Message type should be 2 (ALL_EVENTS_SENT)");

        receiver.close();
    }

    /**
     * Tests that sendEvent() transmits a 15-byte packet containing event and zone data.
     * Validates the byte-level mapping for Type, ID, Severity, Task, and Fault.
     * * @see IncidentReporter#sendEvent(FireEvent)
     */
    @Test
    void testSendEventPacket() throws Exception {
        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(5000);

        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_multiple.csv", "sample_zone_multiple.csv")
        );

        // Create a test event targeting zone 3
        FireEvent event = new FireEvent(
                3,
                TaskType.FIRE_DETECTED,
                LocalTime.of(13, 0),
                Severity.LOW,
                FaultType.NONE,
                123 // FireID
        );

        Method sendEvent = IncidentReporter.class.getDeclaredMethod("sendEvent", FireEvent.class);
        sendEvent.setAccessible(true);

        sendEvent.invoke(reporter, event);

        // IncidentReporter uses a 15-byte buffer for fire events
        byte[] buf = new byte[32];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        receiver.receive(packet);

        assertEquals(15, packet.getLength(), "Fire event packet should be exactly 15 bytes");
        assertEquals(1, buf[0], "Message type should be 1 (FIRE_EVENT)");
        assertEquals(3, buf[1], "Zone ID should be at index 1");
        assertEquals((byte) Severity.LOW.ordinal(), buf[2], "Severity ordinal should be at index 2");
        assertEquals((byte) TaskType.FIRE_DETECTED.ordinal(), buf[3], "Task type ordinal at index 3");
        assertEquals((byte) FaultType.NONE.ordinal(), buf[4], "Fault type ordinal at index 4");

        receiver.close();
    }
}