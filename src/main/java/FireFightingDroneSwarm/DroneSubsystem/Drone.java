package FireFightingDroneSwarm.DroneSubsystem;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.Scheduler.Scheduler;

public class Drone implements Runnable {

    private final int droneId;
    private volatile DroneStatus status;
    public FireEvent currentTask;
    private final Scheduler scheduler;
    private static final double DRONE_SPEED = 20.0; // units per second (Iteration 0)

    /**
     * Creates a Drone object
     * @param droneId ID to represent a drone object
     * @param scheduler The Scheduler the drone communicates with
     */
    public Drone(int droneId, Scheduler scheduler) {
        this.droneId = droneId;
        this.scheduler = scheduler;
        this.status = DroneStatus.IDLE;
    }

    /**
     * The drone waits for tasks, executes them,
     * and notifies the Scheduler upon completion.
     */
    @Override
    public void run() {
        try {
            while ((currentTask = scheduler.get()) != null) {
                this.executeTask();
                scheduler.confirmation(currentTask);
                currentTask = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes the assigned drone task by simulating
     * flight, extinguishing, and return
     * subject to change
     */
    private void executeTask() {
        System.out.println("[Drone " + droneId + "] Dispatched to zone "
                + currentTask.getZoneID());

        // needa sort this out
        transition(DroneStatus.EN_ROUTE);
        travel();

        transition(DroneStatus.ARRIVED);

        transition(DroneStatus.DROPPING_AGENT);
        extinguish(currentTask.getSeverity());

        transition(DroneStatus.DROPPING_AGENT);
        returnToBase();

        transition(DroneStatus.IDLE);
        System.out.println("[Drone " + droneId + "] returned to base");
    }

    /**
     * Validates if the new status is a valid transiton
     * @param newStatus The new state of the drone
     */
    private synchronized void transition(DroneStatus newStatus) {
        System.out.println("[Drone " + droneId + " " + status + " + ] Transitioning to " + newStatus);

        // we wanna check that the transition is valid, no illegal moves
        // IDLE - > EN_ROUTE -> ARRIVED -> DROPPING_AGENT -> EN_ROUTE or RETURNING -> REFILLING -> IDLE
        switch (status){
            case IDLE:
                if (newStatus != DroneStatus.EN_ROUTE) return;
                break;
            case EN_ROUTE:
                if  (newStatus != DroneStatus.ARRIVED) return;
                break;
            case ARRIVED:
                if (newStatus != DroneStatus.DROPPING_AGENT) return;
                break;
            case DROPPING_AGENT:
                if (newStatus != DroneStatus.RETURNING &&
                    newStatus != DroneStatus.EN_ROUTE) return;
                break;
            case RETURNING:
                if (newStatus != DroneStatus.IDLE) return;
                break;
            case REFILLING:
                if (newStatus != DroneStatus.IDLE) return;
                break;
        }
        status = newStatus;

    }

    /**
     * Travel time to the fire location.
     */
    private void travel() {
        // gotta use a calc (short for calculator btw) for some real values
        double distance = calculateDistanceToZone(currentTask.getZoneID());
        long travelTimeMs = (long) ((distance / DRONE_SPEED) * 1000);

        sleep((int) travelTimeMs);
    }

    /**
     * Simulates fire extinguishing time based on fire severity.
     * @param severity the severity level of the fire
     */
    private void extinguish(Severity severity) {
        int extinguishTime;

        // need a calc
        switch (severity) {
            case LOW -> extinguishTime = 1;
            case MODERATE -> extinguishTime = 2;
            case HIGH -> extinguishTime = 3;
            default -> extinguishTime = 4;
        }

        sleep(extinguishTime);
    }

    /**
     * Simulates the return flight after task completion.
     */
    private void returnToBase() {

        double distance = calculateDistanceToZone(currentTask.getZoneID());
        long returnTimeMs = (long) ((distance / DRONE_SPEED) * 1000);

        sleep((int) returnTimeMs);
    }

    private double calculateDistanceToZone(int zoneID) {
        // add in a proper distance calc based on coords
        return zoneID * 10.0;  // simple distance model
    }

    /**
     * Just had to put this here because I do the calculations yet mb
     * actual times gonna be based off one of the drones
     * @param ms duration to sleep in milliseconds
     */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
