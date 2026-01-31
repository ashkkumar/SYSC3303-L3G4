package FireFightingDroneSwarm.DroneSubsystem;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.Scheduler.Scheduler;

public class Drone implements Runnable {

    private final int droneId;
    private volatile DroneStatus status;
    public FireEvent currentTask;
    private final Scheduler scheduler;

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

        status = DroneStatus.IN_FLIGHT;
        travel();

        status = DroneStatus.EXTINGUISHING;
        extinguish(currentTask.getSeverity());

        status = DroneStatus.RETURNING;
        returnToBase();

        status = DroneStatus.IDLE;
        System.out.println("[Drone " + droneId + "] returned to base");
    }

    /**
     * Travel time to the fire location.
     */
    private void travel() {
        // gotta use a calc (short for calculator btw) for some real values
        sleep(1);
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
        //calc
        sleep(1500);
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
