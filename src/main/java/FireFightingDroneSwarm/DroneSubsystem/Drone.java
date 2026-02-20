package FireFightingDroneSwarm.DroneSubsystem;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.Scheduler.Scheduler;

public class Drone implements Runnable {

    private final int droneId;
    private volatile DroneStatus status;
    public FireEvent currentTask;
    private final Scheduler scheduler;
    private static final double DRONE_SPEED = 80.0; // units per second (Iteration 0)

    // Drone position (start at base)
    private double posX;
    private double posY;

    //Drone zone
    private int zone;

    // Base position (choose whatever your sim assumes; often 0,0)
    private static final double BASE_X = 0;
    private static final double BASE_Y = 0;

    // target location
    private double targetX;
    private double targetY;


    /**
     * Creates a Drone object
     * @param droneId ID to represent a drone object
     * @param scheduler The Scheduler the drone communicates with
     */
    public Drone(int droneId, Scheduler scheduler) {
        this.droneId = droneId;
        this.scheduler = scheduler;
        this.status = DroneStatus.IDLE;
        this.posX = BASE_X;
        this.posY = BASE_Y;
        this.zone = 0;
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

        double[] xy = scheduler.getZoneCenter(currentTask.getZoneID());
        this.targetX = xy[0];
        this.targetY = xy[1];

        transition(DroneStatus.EN_ROUTE);
        travelTo(targetX, targetY);

        transition(DroneStatus.ARRIVED);

        transition(DroneStatus.DROPPING_AGENT);
        extinguish(currentTask.getSeverity());

        transition(DroneStatus.RETURNING);
        travelTo(BASE_X, BASE_Y);

        transition(DroneStatus.REFILLING);
        refill();

        transition(DroneStatus.IDLE);
        System.out.println("[Drone " + droneId + "] returned to base");

        // zone = currentTask.getZoneID(); for when we implement refilling, if the tank isn't full it will stay and go to idle
    }

    /**
     * Validates if the new status is a valid transiton
     * @param newStatus The new state of the drone
     */
    private synchronized void transition(DroneStatus newStatus) {
        System.out.println("[Drone " + droneId + " " + status + "] Transitioning to " + newStatus);

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
                if (newStatus != DroneStatus.REFILLING) return;
                break;
            case REFILLING:
                if (newStatus != DroneStatus.IDLE) return;
                break;
        }
        status = newStatus;

    }

    /**
     * Simulates fire extinguishing time based on fire severity.
     * @param severity the severity level of the fire
     */
    private void extinguish(Severity severity) {
        int extinguishTime;

        // need a calc
        switch (severity) {
            case LOW -> extinguishTime = 1000;
            case MODERATE -> extinguishTime = 2000;
            case HIGH -> extinguishTime = 3500;
            default -> extinguishTime = 1500;
        }

        sleep(extinguishTime);
    }

    /**
     * Simulates the return flight after task completion.
     */
    private void travelTo(double x, double y) {
        double distance =  calculateDistanceToZone(x, y);

        long travelTimeMs = (long) ((distance / DRONE_SPEED) * 1000);
        sleep((int) travelTimeMs);
    }

    /**
     * simulates refilling time (2 second)
     */
    private void refill() {
        sleep(2000);
    }

    /**
     * Calculates the travel distance to coordinates x, y
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the distance to be travelled
     */
    private double calculateDistanceToZone(double x, double y) {

        double dx =  x - posX;
        double dy = y - posY;

        posX = x;
        posY = y;

        return Math.sqrt(dx * dx + dy * dy);  // simple distance model
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
