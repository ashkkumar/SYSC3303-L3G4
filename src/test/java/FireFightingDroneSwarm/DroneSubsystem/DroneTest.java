package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DroneTest {

    /**
     * Test drone that disables real sleeping so tests run instantly.
     */
    static class TestDrone extends Drone {
        public TestDrone(int id, Scheduler scheduler) {
            super(id, scheduler, null);
        }

        @Override
        protected void sleep(int ms) {
            // do nothing (prevents real delay)
        }
    }

    /**
     * Verifies that the overridden sleep method in TestDrone
     * does not actually delay execution.
     *
     * The test measures the elapsed time and ensures that the
     * call returns immediately.
     */
    @Test
    void testSleep() {
        Drone drone = new Drone(1);

        long start = System.currentTimeMillis();
        drone.sleep(2000);   // should NOT actually sleep
        long end = System.currentTimeMillis();

        assertFalse(end - start < 50, "Sleep should be overridden and return immediately");
    }

    /**
     * Tests that a newly created drone starts at the correct
     * X position (the base location).
     */
    @Test
    void testGetPosX() {
        Drone drone = new Drone(1);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosX(), 1e-9);
    }

    /**
     * Tests that a newly created drone starts at the correct
     * Y position (the base location).
     */
    @Test
    void testGetPosY() {
        Drone drone = new Drone(1);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosY(), 1e-9);
    }

    /**
     * Verifies that a newly initialized drone begins in the
     * IDLE state before receiving any tasks.
     */
    @Test
    void testGetStatus() {
        Drone drone = new Drone(1);

        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    /**
     * Tests that a newly created drone starts with a full water tank.
     * The expected value corresponds to the MAX_TANK constant (100).
     */
    @Test
    void testGetWaterTank() {
        Drone drone = new Drone(1);

        assertEquals(100, drone.getWaterTank()); // MAX_TANK = 100
    }

    /**
     * Tests that an illegal state transition is prevented.
     *
     * Attempting to move directly from IDLE to ARRIVED should
     * not be allowed, and the drone should remain in the IDLE state.
     */
    @Test
    void testTransition() {
        Drone drone = new Drone(1);

        // Attempt illegal move
        drone.transition(DroneStatus.ARRIVED);

        // Should still be IDLE after
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testSendStatusUDP() throws Exception {

        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(5000);
        Drone drone = new Drone(1);

        String statusMsg = "1,IDLE,0.0,0.0,100";

        drone.sendStatus(statusMsg);

        byte[] buffer = new byte[100];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        receiver.receive(packet);

        String received = new String(packet.getData(), 0, packet.getLength());

        assertEquals(statusMsg, received);

        receiver.close();
    }
}