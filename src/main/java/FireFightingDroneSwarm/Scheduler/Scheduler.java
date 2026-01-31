package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.DroneSubsystem.DroneTask;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;

import java.util.ArrayDeque;
import java.util.Queue;

public class Scheduler implements Runnable {

    private final Queue<FireEvent> buffer = new ArrayDeque<>();
    private final int capacity;
    private final Drone drone;

    public Scheduler(int capacity, Drone drone) {
        this.capacity = capacity;
        this.drone = drone;
    }

    // Drone calls this after finishing
    public synchronized boolean confirmation(DroneTask droneTask) {
        System.out.println("[SCHEDULER] Received confirmation of drone task: " + droneTask);
        notifyAll(); // wake scheduler if waiting for drone to become idle
        return true;
    }

    // Producer calls this
    public synchronized void put(FireEvent fireEvent) throws InterruptedException {
        while (buffer.size() >= capacity) {
            wait(); // full
        }
        buffer.add(fireEvent);
        System.out.println("[SCHEDULER] Buffered fire event: " + fireEvent);
        notifyAll(); // wake scheduler consumer thread
    }

    // Scheduler consumes events
    private synchronized FireEvent get() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait(); // empty
        }
        FireEvent fireEvent = buffer.remove();
        notifyAll(); // wake producers (space freed)
        return fireEvent;
    }

    // Wait until the single drone is idle
    private void waitUntilDroneIdle() throws InterruptedException {
        synchronized (this) {
            while (!drone.isIdle()) {
                wait();
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                FireEvent event = get();          // blocks until an event exists
                waitUntilDroneIdle();             // blocks until drone finishes prior task

                DroneTask task = new DroneTask(event, event.getTaskType());
                System.out.println("[SCHEDULER] Dispatching task to drone: " + task);

                drone.assignTask(task);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
