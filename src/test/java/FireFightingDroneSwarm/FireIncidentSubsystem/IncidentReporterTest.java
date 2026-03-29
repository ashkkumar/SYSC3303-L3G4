package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.Events.EventLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for IncidentReporter, verifying UDP packet structures
 * and internal data parsing.
 */
class IncidentReporterTest {
    private EventLogger logger;

    @BeforeEach
    void setUp() {
        logger = new EventLogger(1000);
    }

    /**
     * Tests that allEventsSent() sends a UDP packet with the correct type (2).
     */
    @Test
    void testAllEventsSentPacket() throws Exception {
        // Bind to the expected port the reporter sends to
        DatagramSocket receiver = new DatagramSocket(50000);
        receiver.setSoTimeout(2000);

        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv"),
                null, null, logger
        );

        Method allEventsSent = IncidentReporter.class.getDeclaredMethod("allEventsSent");
        allEventsSent.setAccessible(true);
        allEventsSent.invoke(reporter);

        byte[] buf = new byte[16];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        receiver.receive(packet);

        assertEquals(2, buf[0]); // Message type ALL_EVENTS_SENT is 2
        receiver.close();
    }

    /**
     * Tests that the separateBytes() helper correctly splits an integer.
     */
    @Test
    void testSeparateBytes() throws Exception {
        IncidentReporter reporter = new IncidentReporter(
                new InputReader("sample_event_file.csv", "sample_zone_file.csv"),
                null, null, logger
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
}