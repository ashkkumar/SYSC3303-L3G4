package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.DroneSubsystem.DroneTask;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * The Scheduler coordinates fire events and drone task execution.
 * <p>
 * It acts as a bounded buffer for FireEvent objects and follows
 * a producer–consumer model:
 * <ul>
 *   <li>Producers submit FireEvent objects using {@link #put(FireEvent)}</li>
 *   <li>The Scheduler consumes events and dispatches tasks to a Drone</li>
 *   <li>The Drone notifies the Scheduler upon task completion</li>
 * </ul>
 *
 * The Scheduler runs as its own thread and uses wait/notifyAll
 * to ensure proper synchronization without busy-waiting.
 */
public class Scheduler implements Runnable {

    /** Bounded buffer holding pending fire events */
    private final Queue<FireEvent> buffer = new ArrayDeque<>();

    /** Maximum number of fire events allowed in the buffer */
    private final int capacity;

    /** The drone managed by this scheduler */
    private final Drone drone;

    /**
     * Creates a Scheduler with a bounded buffer and a drone.
     *
     * @param capacity maximum number of FireEvent objects allowed in the buffer
     * @param drone the drone that will execute scheduled tasks
     */
    public Scheduler(int capacity, Drone drone) {
        this.capacity = capacity;
        this.drone = drone;
    }

    /**
     * Receives confirmation from the Drone that a task has completed.
     * <p>
     * This method wakes the Scheduler if it is waiting for the drone
     * to become idle.
     *
     * @param droneTask the completed DroneTask
     * @return true if the confirmation was successfully received
     */
    public synchronized boolean confirmation(DroneTask droneTask) {
        System.out.println("[SCHEDULER] Received confirmation of drone task: " + droneTask);
        notifyAll();
        return true;
    }

    /**
     * Adds a FireEvent to the bounded buffer.
     * <p>
     * If the buffer is full, the calling producer thread blocks
     * until space becomes available.
     *
     * @param fireEvent the FireEvent to be added
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void put(FireEvent fireEvent) throws InterruptedException {
        while (buffer.size() >= capacity) {
            wait();
        }
        buffer.add(fireEvent);
        System.out.println("[SCHEDULER] Buffered fire event: " + fireEvent);
        notifyAll();
    }

    /**
     * Retrieves and removes the next FireEvent from the buffer.
     * <p>
     * If the buffer is empty, the Scheduler thread blocks
     * until an event becomes available.
     *
     * @return the next FireEvent to be scheduled
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private synchronized FireEvent get() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait();
        }
        FireEvent fireEvent = buffer.remove();
        notifyAll();
        return fireEvent;
    }

    /**
     * Blocks until the drone becomes idle.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void waitUntilDroneIdle() throws InterruptedException {
        synchronized (this) {
            while (!drone.isIdle()) {
                wait();
            }
        }
    }

    /**
     * Main execution loop of the Scheduler.
     * <p>
     * Continuously consumes FireEvent objects from the buffer,
     * waits for the drone to become idle, converts the event into
     * a DroneTask, and assigns it to the drone.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                FireEvent event = get();
                waitUntilDroneIdle();
                DroneTask task = new DroneTask(event, event.getTaskType());
                System.out.println("[SCHEDULER] Dispatching task to drone: " + task);
                drone.assignTask(task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
