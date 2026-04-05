package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import FireFightingDroneSwarm.Events.LogManager;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Drone} class.
 * Validates core functionality including state transitions, UDP communication,
 * and error handling for various injected fault types.
 */
class DroneTest {

    /** Mock logger used to satisfy Drone constructor requirements without file I/O overhead. */
    private static LogManager mockLogger = new LogManager();

    /**
     * A specialized version of {@link Drone} for testing purposes.
     * Overrides the {@code sleep} method to return immediately, allowing tests
     * involving travel or extinguishing simulations to run without real-time delays.
     */
    static class TestDrone extends Drone {
        /**
         * Constructs a TestDrone with mock dependencies.
         * @param id The unique identifier for the test drone.
         */
        public TestDrone(int id) {
            super(id, null, null, mockLogger);
        }

        /**
         * Overrides the standard sleep to prevent execution delays during testing.
         * @param ms The requested sleep duration (ignored).
         */
        @Override
        protected void sleep(int ms) {
            // No-op for instant test execution
        }
    }

    /**
     * Verifies that the {@link TestDrone#sleep(int)} override effectively
     * bypasses the {@link Thread#sleep(long)} call.
     */
    @Test
    void testSleep() {
        TestDrone drone = new TestDrone(1);

        long start = System.currentTimeMillis();
        drone.sleep(2000);
        long end = System.currentTimeMillis();

        assertTrue(end - start < 50, "Sleep should be overridden and return immediately");
    }

    /**
     * Verifies that a drone is initialized with the correct default values
     * for position, state, and resources.
     */
    @Test
    void testInitialConditions() {
        Drone drone = new Drone(1);
        assertEquals(0.0, drone.getPosX(), 1e-9);
        assertEquals(0.0, drone.getPosY(), 1e-9);
        assertEquals(DroneStatus.IDLE, drone.getStatus());
        assertEquals(100, drone.getWaterTank());
    }

    /**
     * Ensures that the drone state machine prevents illegal transitions.
     * For example, a drone should not move from IDLE to ARRIVED without traveling first.
     */
    @Test
    void testTransition() {
        Drone drone = new Drone(1);
        drone.transition(DroneStatus.ARRIVED);
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    /**
     * Validates recovery from a {@link FaultType#STUCK_MID_FLIGHT}.
     * Tests that the drone triggers the fault, stops travel, and executes
     * the logic to return to base and reset to IDLE.
     */
    @Test
    void testStuckMidFlightFault() throws Exception {
        TestDrone drone = new TestDrone(1);
        drone.currentTask = new FireEvent(3, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.HIGH, FaultType.STUCK_MID_FLIGHT, 1);
        drone.zoneCenter = new double[]{800.0, 0.0};

        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        assertTrue((boolean) getPrivateField(drone, "faultTriggered"), "Fault should have been triggered.");
        assertEquals(DroneStatus.IDLE, drone.getStatus(), "Drone should return to IDLE after base recovery.");
        assertEquals(0.0, drone.getPosX(), 0.001, "Drone should be back at base position.");
    }

    /**
     * Validates the handling of a {@link FaultType#NOZZLE_JAM}.
     * Tests that the drone enters an {@code OUT_OF_SERVICE} state and halts
     * extinguishing operations immediately.
     */
    @Test
    void testNozzleJamFault() throws Exception {
        TestDrone drone = new TestDrone(2);
        drone.currentTask = new FireEvent(4, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.MODERATE, FaultType.NOZZLE_JAM, 1);

        setPrivateField(drone, "injectedFault", FaultType.NOZZLE_JAM);
        setPrivateField(drone, "faultTriggered", false);

        drone.executeTask();

        assertEquals(DroneStatus.OUT_OF_SERVICE, drone.getStatus());
    }

    /**
     * Validates the {@link FaultType#PACKET_LOSS} behavior.
     * Ensures that when this fault is active, {@code sendStatus} does not
     * transmit data over the network.
     */
    @Test
    void testPacketLossDropsStatusPacket() throws Exception {
        DatagramSocket receiver = new DatagramSocket(null);
        receiver.setReuseAddress(true);
        receiver.bind(new InetSocketAddress(50000));
        receiver.setSoTimeout(500);

        TestDrone drone = new TestDrone(3);
        drone.currentTask = new FireEvent(1, TaskType.FIRE_DETECTED, LocalTime.now(), Severity.LOW, FaultType.PACKET_LOSS, 3);

        setPrivateField(drone, "injectedFault", FaultType.PACKET_LOSS);

        assertThrows(java.net.SocketTimeoutException.class, () -> {
            drone.sendStatus("3,IDLE,0.0,0.0,100");
            byte[] buffer = new byte[100];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            receiver.receive(packet);
        });

        receiver.close();
    }

    /**
     * Reflection helper to set private fields for testing state-dependent logic.
     * @param target The object whose field is being set.
     * @param fieldName The name of the private field.
     * @param value The value to assign to the field.
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        var field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * Reflection helper to access private fields for assertion checking.
     * @param target The object whose field is being accessed.
     * @param fieldName The name of the private field.
     * @return The value of the field.
     */
    private Object getPrivateField(Object target, String fieldName) throws Exception {
        var field = Drone.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}