package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.DroneSubsystem.FaultType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class IncidentReporterTest {

    /**
     * Tests that the separateBytes() helper correctly splits
     * a two-byte integer value into high and low bytes.
     * @throws Exception if reflection fails
     */
    @Test
    void testSeparateBytes() throws Exception {
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv")
        );

        byte[] data = new byte[2];

        Method separateBytes = IncidentReporter.class.getDeclaredMethod(
                "separateBytes", byte[].class, int.class, int.class
        );
        separateBytes.setAccessible(true);

        separateBytes.invoke(reporter, data, 0, 1600);

        assertEquals((byte) (1600 >> 8), data[0]);
        assertEquals((byte) 1600, data[1]);
    }

    /**
     * Tests that getZoneById() returns the correct Zone object
     * when the requested zone ID exists.
     * @throws Exception if reflection fails
     */
    @Test
    void testGetZoneById() throws Exception {
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv")
        );

        Method getZoneById = IncidentReporter.class.getDeclaredMethod("getZoneById", int.class);
        getZoneById.setAccessible(true);

        Zone zone = (Zone) getZoneById.invoke(reporter, 3);

        assertNotNull(zone);
        assertEquals(3, zone.getID());
    }

    /**
     * Tests that getZoneById() returns null when the requested
     * zone ID does not exist.
     * @throws Exception if reflection fails
     */
    @Test
    void testGetZoneByIdInvalid() throws Exception {
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv")
        );

        Method getZoneById = IncidentReporter.class.getDeclaredMethod("getZoneById", int.class);
        getZoneById.setAccessible(true);

        Zone zone = (Zone) getZoneById.invoke(reporter, 999);

        assertNull(zone);
    }

    /**
     * Tests that allEventsSent() sends a UDP packet with the
     * correct message type value.
     * Expected packet format:
     * byte[0] = 2 (ALL_EVENTS_SENT)
     * @throws Exception if reflection or socket communication fails
     */
    @Test
    void testAllEventsSentPacket() throws Exception {
        DatagramSocket receiver = new DatagramSocket(50000);
        receiver.setSoTimeout(2000);

        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv")
        );

        Method allEventsSent = IncidentReporter.class.getDeclaredMethod("allEventsSent");
        allEventsSent.setAccessible(true);

        allEventsSent.invoke(reporter);

        byte[] buf = new byte[16];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        receiver.receive(packet);

        assertEquals(1, packet.getLength());
        assertEquals(2, buf[0]);

        receiver.close();
    }

    /**
     * Tests that sendEvent() sends a correctly formatted UDP packet
     * for a fire event.
     * Expected packet format:
     * byte[0] = message type (1 = FIRE_EVENT)
     * byte[1] = zone ID
     * byte[2] = severity
     * byte[3] = task type
     * byte[4..5] = startX
     * byte[6..7] = startY
     * byte[8..9] = endX
     * byte[10..11] = endY
     * @throws Exception if reflection or socket communication fails
     */
    @Test
    void testSendEventPacket() throws Exception {
        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(5000);

        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv")
        );

        FireEvent event = new FireEvent(
                3,
                TaskType.FIRE_DETECTED,
                LocalTime.of(13, 0),
                Severity.LOW,
                FaultType.NONE,
                1
        );

        Method sendEvent = IncidentReporter.class.getDeclaredMethod("sendEvent", FireEvent.class);
        sendEvent.setAccessible(true);

        sendEvent.invoke(reporter, event);

        byte[] buf = new byte[32];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        receiver.receive(packet);

        assertEquals(15, packet.getLength());
        assertEquals(1, buf[0]); // message type
        assertEquals(3, buf[1]); // zone ID
        assertEquals((byte) Severity.LOW.ordinal(), buf[2]);
        assertEquals((byte) TaskType.FIRE_DETECTED.ordinal(), buf[3]);

        receiver.close();
    }
}