package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import org.junit.jupiter.api.Test;

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

    @Test
    void testSleep() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        long start = System.currentTimeMillis();
        drone.sleep(2000);   // should NOT actually sleep
        long end = System.currentTimeMillis();

        assertTrue(end - start < 50, "Sleep should be overridden and return immediately");
    }

    @Test
    void testGetPosX() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosX(), 1e-9);
    }

    @Test
    void testGetPosY() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        // Drone starts at base (0,0)
        assertEquals(0.0, drone.getPosY(), 1e-9);
    }

    @Test
    void testGetStatus() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testGetWaterTank() {
        Scheduler scheduler = new Scheduler(5);
        TestDrone drone = new TestDrone(1, scheduler);

        assertEquals(100, drone.getWaterTank()); // MAX_TANK = 100
    }

    @Test
    void testExecuteTask() throws Exception {

        Scheduler scheduler = new Scheduler(10);

        // Create a simple zone so getZoneCenter works
        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, new Zone(1, new int[]{0,0}, new int[]{10,10}));
        scheduler.setZoneIDs(zones);

        TestDrone drone = new TestDrone(1, scheduler);

        // Inject a task
        drone.currentTask = new FireEvent(
                1,
                TaskType.FIRE_DETECTED,
                LocalTime.of(13,0),
                Severity.LOW
        );

        drone.executeTask();

        // Final state should be IDLE
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }

    @Test
    void testTransition() {

        Scheduler scheduler = new Scheduler(5);
        Drone drone = new Drone(1, scheduler, null);

        // Attempt illegal move
        drone.transition(DroneStatus.ARRIVED);

        // Should still be IDLE after
        assertEquals(DroneStatus.IDLE, drone.getStatus());
    }
}