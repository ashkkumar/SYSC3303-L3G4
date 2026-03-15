package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.DroneSubsystem.DroneStatus;
import FireFightingDroneSwarm.FireIncidentSubsystem.*;

import java.net.*;
import java.time.LocalTime;
import java.util.*;

/**
 * This class implements the Scheduler responsible for dispatching
 * events to drones, after receiving them from the fire incident
 * subsystem.
 */
public class Scheduler implements Runnable {

    private final Queue<FireEvent> buffer = new ArrayDeque<>();
    private final int capacity;
    private IncidentReporter incidentReporter;
    private boolean allTasksSent;
    private boolean allTasksProcessed;
    private Map<Integer, Zone> zoneIDs;
    private Map<Integer, DroneState> droneStates = new HashMap<>();

    private DatagramSocket socket;
    private DatagramPacket receivePacket;

    /**
     * Constructor for the scheduler, default drones and incident
     * reporter to null to prevent cyclic dependency
     * @param capacity int for capacity of the task queue.
     */
    public Scheduler(int capacity) {
        this.capacity = capacity;
        this.incidentReporter = null;

        try {
            this.socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(50000));
            System.out.println("Bound to socket 50000");
        } catch (SocketException e) {
            e.printStackTrace();
            System.out.println("Socket binding error for Scheduler");
        }

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

        FireEvent fireEvent = null;
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
     *
     */
    public void assignDroneEvent() {

        if (buffer.isEmpty() || droneStates.isEmpty()) {
            return;
        }

        FireEvent bestEvent = null;
        DroneState bestDrone = null;
        double bestScore = 0;

        for (FireEvent event : buffer) {

            double[] zoneCenter = getZoneCenter(event.getZoneID());

            for (DroneState drone : droneStates.values()) {

                // only consider idle drones
                if (drone.getStatus() != DroneStatus.IDLE) continue;

                double dx = zoneCenter[0] - drone.getPosX();
                double dy = zoneCenter[1] - drone.getPosY();
                double distance = Math.sqrt(dx * dx + dy * dy);

                double severityScore = switch (event.getSeverity()) {
                    case LOW -> 1;
                    case MODERATE -> 5;
                    case HIGH -> 20;
                };

                double totalScore = severityScore / (distance + 1); // avoid divide by zero

                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestEvent = event;
                    bestDrone = drone;
                }
            }
        }

        if (bestEvent != null && bestDrone != null) {

            buffer.remove(bestEvent);

            System.out.println("[Scheduler] Assigning Drone "
                    + bestDrone.getDroneId()
                    + " to Zone "
                    + bestEvent.getZoneID());

            sendFireEventToDrone(bestDrone, bestEvent);
        }
    }


    /**
     * Returns the distance between 2 zones
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

    private void sendFireEventToDrone(DroneState drone, FireEvent event) {

        try {

            Zone zone = zoneIDs.get(event.getZoneID());

            int startX = zone.getStartCoordinates()[0];
            int startY = zone.getStartCoordinates()[1];
            int endX = zone.getEndCoordinates()[0];
            int endY = zone.getEndCoordinates()[1];

            byte[] data = new byte[12];

            data[0] = 3; // message type (assignment)
            data[1] = (byte) event.getZoneID();
            data[2] = (byte) event.getSeverity().ordinal();
            data[3] = (byte) event.getTaskType().ordinal();

            separateBytes(data, 4, startX);
            separateBytes(data, 6, startY);
            separateBytes(data, 8, endX);
            separateBytes(data, 10, endY);

            InetAddress droneAddress = drone.getAddress();
            int dronePort = drone.getPort();

            DatagramPacket packet =
                    new DatagramPacket(data, data.length, droneAddress, dronePort);

            socket.send(packet);

            System.out.println("[Scheduler] Sent task to Drone "
                    + drone.getDroneId());

        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * Notifies the scheduler that the drone has reached the fire zone.
     * This satisfies the Iteration 2 requirement for arrival notification.
     */
    public synchronized void notifyArrival(int droneId) {
        System.out.println("[SCHEDULER] Drone " + droneId + " has arrived at the zone.");
        notifyAll();
    }

    /**
     * Method to receive and parse a Datagram Packet representing a fire incident from the incident
     * reporter subsystem. Construct a new FireEvent and add to Queue.
     */
    public void receivePacket() {
        try {

            receivePacket = new DatagramPacket(new byte[1024], 1024);
            socket.receive(receivePacket);

            byte[] data = receivePacket.getData();
            InetAddress address = receivePacket.getAddress();
            int port = receivePacket.getPort();

            if (data[0] == 1) {
                handleFireEvent(data);
            }
            else {
                handleDroneStatus(data, receivePacket.getLength(), address, port);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFireEvent(byte[] data) {

        int zoneID = data[1];
        int severityNum = data[2];
        int taskNum = data[3];

        int startX = combineBytes(data[4], data[5]);
        int startY = combineBytes(data[6], data[7]);
        int endX   = combineBytes(data[8], data[9]);
        int endY   = combineBytes(data[10], data[11]);

        Severity severity = Severity.values()[severityNum];
        TaskType taskType = TaskType.values()[taskNum];

        FireEvent event = new FireEvent(zoneID, taskType, LocalTime.now(), severity);

        try {
            put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Scheduler] Received fire event for zone " + zoneID);
    }

    private void handleDroneStatus(byte[] data, int length, InetAddress address, int port) {

        String message = new String(data, 0, length).trim();

        String[] parts = message.split(",");

        int droneId = Integer.parseInt(parts[0]);
        DroneStatus status = DroneStatus.valueOf(parts[1]);
        double posX = Double.parseDouble(parts[2]);
        double posY = Double.parseDouble(parts[3]);
        int water = Integer.parseInt(parts[4]);


        DroneState drone = droneStates.get(droneId);

        if (drone == null) {
            drone = new DroneState(droneId, status, posX, posY, water, address, port);
            droneStates.put(droneId, drone);

            System.out.println("[Scheduler] Registered Drone " + droneId);

        } else {
            drone.update(status, posX, posY, water);

        }

        System.out.println("[Scheduler] Drone " + droneId +
                " Status: " + status +
                " Position: (" + posX + "," + posY + ")" +
                " Water: " + water);
    }


    private int combineBytes(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    /**
     * Helper method to determine if there are any idle drones at this point in time.
     * @return the first idle drone, null otherwise
     */
    public DroneState getIdleDrone() {

        for (DroneState drone : droneStates.values()) {
            if (drone.getStatus() == DroneStatus.IDLE) {
                return drone;
            }
        }

        return null;
    }

    /**
     * Helper method for finding the drone closest to a particular zone center based on the status of
     * all drones currently.
     * @param x double representing x coordinate of zone center
     * @param y double representing y coordinate of zone center
     * @return the drone closest to the zone center
     */
    public DroneState findClosestDrone(double x, double y) {

        DroneState best = null;
        double minDistance = Double.MAX_VALUE;

        for (DroneState drone : droneStates.values()) {

            if (drone.getStatus() != DroneStatus.IDLE)
                continue;

            double dx = x - drone.getPosX();
            double dy = y - drone.getPosY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < minDistance) {
                minDistance = distance;
                best = drone;
            }
        }

        return best;
    }

    private void separateBytes(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 8);      // high byte
        data[offset + 1] = (byte) value;         // low byte
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
                    this.receivePacket();
                }
                System.out.println("[SCHEDULER] All tasks marked complete by incident subsystem");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<Integer, Zone> buildZoneMap(ArrayList<Zone> zones) {
        Map<Integer, Zone> map = new HashMap<>();
        for (Zone z : zones) {
            map.put(z.getID(), z);
        }
        return map;
    }


    public static void main(String[] args) {
        InputReader inputReader =
                new InputReader("sample_event_multiple.csv",
                        "sample_zone_multiple.csv");
        Scheduler scheduler = new Scheduler(15);
        scheduler.setZoneIDs(Scheduler.buildZoneMap(inputReader.parseZoneFile()));
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.start();
    }
}
