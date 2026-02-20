package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.IncidentReporter;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

/**
 * This class implements the Scheduler responsible for dispatching
 * events to drones, after receiving them from the fire incident
 * subsystem.
 */
public class Scheduler implements Runnable {

    private final Queue<FireEvent> buffer = new ArrayDeque<>();
    private final int capacity;
    private Drone drone;
    private IncidentReporter incidentReporter;
    private boolean allTasksSent;
    private boolean allTasksProcessed;
    private Map<Integer, Zone> zoneIDs;

    /**
     * Constructor for the scheduler, default drones and incident
     * reporter to null to prevent cyclic dependency
     * @param capacity int for capacity of the task queue.
     */
    public Scheduler(int capacity) {
        this.capacity = capacity;
        this.drone = null;
        this.incidentReporter = null;
    }

    /**
     * Setter to set drone for this scheduler
     * @param drone Drone object corresponding to this scheduler already initialized.
     */
    public void setDrone(Drone drone) {
        this.drone = drone;
    }

    /**
     * Setter to set IncidentReporterfor this scheduler after construction.
     * @param incidentReporter IncidentReporter already initialized with this
     *                         scheduler object.
     */
    public void setIncidentReporter(IncidentReporter incidentReporter) {
        this.incidentReporter = incidentReporter;
    }

    /**
     * Synchronized confirmation from any drone to then alert incident subsystem
     * @param fireEvent FireEvent that has been completed by a drone
     * @return true if successful
     */
    public synchronized boolean confirmation(FireEvent fireEvent) {
        System.out.println("[SCHEDULER] Received confirmation of drone task: " + fireEvent);
        incidentReporter.getEventConfirmation(fireEvent);
        notifyAll();
        return true;
    }

    /**
     * Synchronized put method to be invoked by incident subsystem to effectively add
     * events to the queue
     * @param fireEvent FireEvent to be added to the queue of events
     * @throws InterruptedException in cases of unexpected termination
     */
    public synchronized void put(FireEvent fireEvent) throws InterruptedException {
        while (buffer.size() >= capacity) {
            wait(); // full
        }
        buffer.add(fireEvent);
        System.out.println("[SCHEDULER] Buffered fire event: " + fireEvent);
        notifyAll(); // wake scheduler consumer thread
    }

    /**
     * Synchronized get method to be invoked by a drone to receive an event
     * @return FireEvent for next possible event
     * @throws InterruptedException in cases of unexpected termination
     */
    public synchronized FireEvent get() throws InterruptedException {
        while (buffer.isEmpty() && !this.getAllTasksProcessed()) {
            wait();
        }

        if(this.getAllTasksProcessed()){
            return null;
        }

        FireEvent fireEvent = assignDroneEvent();
        if(this.getAllTasksSent() && buffer.isEmpty()) {
            this.allTasksProcessed = true;
        }

        notifyAll();
        return fireEvent;
    }

    /**
     * Setter invoked by the incident reporter to signify it has no more
     * events remaining to send
     * @param allTasksSent boolean true when all events have been processed
     */
    public synchronized void setAllTasksSent(boolean allTasksSent) {
        this.allTasksSent = allTasksSent;
    }

    /**
     * Getter to check current state of the system - i.e all
     * possible events have been processed
     * @return false if more events to be processed, true otherwise
     */
    public synchronized boolean getAllTasksSent() {
        return this.allTasksSent;
    }

    /**
     * Getter to check whether all tasks have been processed by
     * drones
     * @return true if all tasks processed, false otherwise
     */
    public synchronized boolean getAllTasksProcessed() {
        return this.allTasksProcessed;
    }

    /**
     * Setter for a map of ZoneIDs
     * @param zoneIDs
     */
    public void setZoneIDs(Map<Integer, Zone> zoneIDs) {
        this.zoneIDs = zoneIDs;
    }

    /**
     * Algorithm for assigning a drone the appropriate event
     * Based on Weighted pathfinding, each event is given a score based on distance and severity
     * @return Event with the highest score i
     *
     */
    public synchronized FireEvent assignDroneEvent() {
        double max = 0;
        FireEvent highestScore = buffer.peek();
        for (FireEvent event: buffer) {
            double zoneScore = calculateZoneDistance(event.getZoneID(), 0); // for now but later on will be the distance from the target zone to drones current zone when refill is implimented
            double severityScore = 0;
            Severity severity = event.getSeverity();
            if (severity.equals(Severity.LOW)) {severityScore = 1;}
            if (severity.equals(Severity.MODERATE)) {severityScore = 5;}
            if (severity.equals(Severity.HIGH)) {severityScore = 20;}

            double totalScore = severityScore / zoneScore;
            if (totalScore > max) {
                highestScore = event;
                max = totalScore;
            }
        }
        buffer.remove(highestScore);
        return highestScore;

    }

    /**
     * Returns the distancce betweens 2 zones
     * @param zone1 center of the first zone
     * @param zone2 center of the second zone
     * @return double distance between the zones
     */
    public synchronized double calculateZoneDistance(int zone1, int zone2) {
        double dx = 0;
        double dy = 0;
        // drone is at base
        if (zone2 == 0) {
            double[] zone1Distance = getZoneCenter(zone1);
            dx = 0 - zone1Distance[0];
            dy = 0 - zone1Distance[1];

        } else {
            double[] zone1Distance = getZoneCenter(zone1);
            double[] zone2Distance = getZoneCenter(zone2);
            dx = zone2Distance[0] - zone1Distance[0];
            dy = zone2Distance[1] - zone1Distance[1];
        }


        return Math.sqrt(dx * dx + dy * dy);


    }

    /**
     * Finds the start and end coordinates from the zone
     * @param zoneId the zone's ID
     * @return double[] with the coordinates
     */
    public synchronized double[] getZoneCenter(int zoneId) {
        if (zoneIDs == null) throw new IllegalStateException("zonesById not set");
        Zone z = zoneIDs.get(zoneId);
        if (z == null) throw new IllegalArgumentException("Unknown zoneId: " + zoneId);

        int[] s = z.getStartCoordinates();
        int[] e = z.getEndCoordinates();
        return new double[]{ (s[0] + e[0]) / 2.0, (s[1] + e[1]) / 2.0 };
    }

    /**
     * Scheduler's implementation of run, do nothing in this
     * iteration - just a communication channel until all tasks complete
     */
    @Override
    public void run() {
        try {
            synchronized (this) { // prevents a race on this method instance from within the object
                while (!this.getAllTasksSent() && !this.getAllTasksProcessed()) {
                    this.wait();
                }
                System.out.println("[SCHEDULER] All tasks marked complete by incident subsystem");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
