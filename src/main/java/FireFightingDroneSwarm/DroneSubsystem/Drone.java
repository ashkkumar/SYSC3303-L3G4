package FireFightingDroneSwarm.DroneSubsystem;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.Scheduler.Scheduler;

public class Drone implements Runnable {

    private final int droneId;
    private volatile DroneStatus status;
    public DroneTask currentTask;
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
     * Assigns a task to the drone.
     * This method is called by the Scheduler
     *
     * @param task the task to be executed by the drone
     */
    public synchronized void assignTask(DroneTask task) {
        this.currentTask = task;
        notifyAll();
    }

    /**
     * The drone waits for tasks, executes them,
     * and notifies the Scheduler upon completion.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            waitForTask();
            if (Thread.currentThread().isInterrupted()) break;
            executeTask();
            notifyScheduler();
        }
    }

    /**
     * Causes the drone thread to block until a task is assigned.
     */
    private synchronized void waitForTask() {
        while (currentTask == null) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Executes the assigned drone task by simulating
     * flight, extinguishing, and return
     * subject to change
     */
    private void executeTask() {
        FireEvent event = currentTask.getFireEvent();

        System.out.println("[Drone " + droneId + "] Dispatched to zone "
                + event.getZoneID());

        status = DroneStatus.IN_FLIGHT;
        travel();

        status = DroneStatus.EXTINGUISHING;
        extinguish(event.getSeverity());

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
     * Notifies the Scheduler that the current task has been completed.
     */
    private void notifyScheduler() {
        DroneTask finished;
        synchronized (this) {
            finished = currentTask;
        }
        boolean ok = scheduler.confirmation(finished);
        if (ok) {
            synchronized (this) {
                if (currentTask == finished) {
                    currentTask = null;
                }
            }
        }
    }

    /**
     * Just had to put this hear cause i didnt do the caclculations yet mb
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

    public synchronized boolean isIdle() {
        return currentTask == null && status == DroneStatus.IDLE;
    }

}
