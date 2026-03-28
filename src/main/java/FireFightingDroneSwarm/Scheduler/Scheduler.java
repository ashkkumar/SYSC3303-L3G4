package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.DroneSubsystem.DroneStatus;
import FireFightingDroneSwarm.DroneSubsystem.FaultType;
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
    private final Map<Integer, FireEvent> activeAssignments = new HashMap<>();
    private final Map<Integer, Long> assignmentStartTimes = new HashMap<>();
    //private FireEvent currentEvent;
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
     * Helper method for tests to add a drone state to the list of possible drones
     * @param key key index into droneStates map
     * @param droneState droneState value to map to key
     */
    public synchronized void addDroneState(int key, DroneState droneState) {
        droneStates.put(key, droneState);
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

        FireEvent fireEvent = buffer.poll();
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
     * This method consists of most of the scheduler logic,
     * it looks at the current fire events in the queue,
     * scores the best drone, and best event for that drone and then
     * dispatches a UDP message to that drone in particular.
     */
    public void assignDroneEvent() {

        if (buffer.isEmpty() || droneStates.isEmpty()) {
            System.out.println("[SCHEDULER] No drones available or no buffer events");
            return;
        }

        FireEvent bestEvent = null;
        DroneState bestDrone = null;
        double bestScore = 0;

        // Iterate through every fire event
        for (FireEvent event : buffer) {

            double[] zoneCenter = getZoneCenter(event.getZoneID());

            // Look for the first idle drone - to be changed later to include drones
            // on the way to other zones
            for (DroneState drone : droneStates.values()) {
                if (drone.getStatus() != DroneStatus.IDLE) continue;

                // Calculate the drone's distance to the zone center
                double dx = zoneCenter[0] - drone.getPosX();
                double dy = zoneCenter[1] - drone.getPosY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                // Find event severity
                double severityScore = switch (event.getSeverity()) {
                    case LOW -> 1;
                    case MODERATE -> 5;
                    case HIGH -> 20;
                };

                // Compute the score for that particular drone
                double totalScore = severityScore / (distance + 1); // avoid divide by zero
                // Update when the score is better than the best so far
                if (totalScore > bestScore) {
                    bestScore = totalScore;
                    bestEvent = event;
                    bestDrone = drone;
                }
            }
        }

        // Remove the event, and send a UDP packet to that drone
        if (bestEvent != null) {

            buffer.remove(bestEvent);

            System.out.println("[Scheduler] Assigning Drone "
                    + bestDrone.getDroneId()
                    + " to Zone "
                    + bestEvent.getZoneID());

            activeAssignments.put(bestDrone.getDroneId(), bestEvent);
            assignmentStartTimes.put(bestDrone.getDroneId(), System.currentTimeMillis());

           //this.currentEvent = bestEvent;
           this.sendFireEventToDrone(bestDrone, bestEvent);
        }
    }

    /**
     * Returns the distance between 2 zones
     * @param zone1 center of the first zone
     * @param zone2 center of the second zone
     * @return double distance between the zones
     */
    public double calculateZoneDistance(int zone1, int zone2) {
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
     * This method handles the actual UDP datagram sending of
     * fire events to a drone after the drone has been selected
     * @param drone the DroneState corresponding to the drone to be dispatched
     * @param event the FireEvent that the drone will service.
     */
    private void sendFireEventToDrone(DroneState drone, FireEvent event) {

        try {

            Zone zone = zoneIDs.get(event.getZoneID());

            int startX = zone.getStartCoordinates()[0];
            int startY = zone.getStartCoordinates()[1];
            int endX = zone.getEndCoordinates()[0];
            int endY = zone.getEndCoordinates()[1];

            byte[] data = new byte[13];

            data[0] = 3; // message type (assignment)
            data[1] = (byte) event.getZoneID();
            data[2] = (byte) event.getSeverity().ordinal();
            data[3] = (byte) event.getTaskType().ordinal();

            // NEW
            data[4] = (byte) event.getFault().ordinal();

            // shifted coordinates
            separateBytes(data, 5, startX);
            separateBytes(data, 7, startY);
            separateBytes(data, 9, endX);
            separateBytes(data, 11, endY);

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

    /**
     * This method handles parsing and instantiation of a FireEvent
     * after a UDP Datagram has been received.
     *
     * Expected packet format:
     * byte[0] = message type (1 = FIRE_EVENT)
     * byte[1] = zone ID
     * byte[2] = severity level
     * byte[3] = task type
     * byte[4] = fault type
     * byte[5..6] = zone start X coordinate
     * byte[7..8] = zone start Y coordinate
     * byte[9..10] = zone end X coordinate
     * byte[11..12] = zone end Y coordinate
     *
     * @param data the byte array from the UDP datagram.
     */
    private void handleFireEvent(byte[] data) {

        int zoneID = data[1];
        int severityNum = data[2];
        int taskNum = data[3];
        int faultNum = data[4];

        // shifted all indices by +1 after inserting faultType
        int startX = combineBytes(data[5], data[6]);
        int startY = combineBytes(data[7], data[8]);
        int endX   = combineBytes(data[9], data[10]);
        int endY   = combineBytes(data[11], data[12]);

        Severity severity = Severity.values()[severityNum];
        TaskType taskType = TaskType.values()[taskNum];
        FaultType faultType = FaultType.values()[faultNum];

        FireEvent event = new FireEvent(zoneID, taskType, LocalTime.now(), severity, faultType);

        try {
            put(event);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Scheduler] Received fire event for zone " + zoneID);
        this.assignDroneEvent();
    }

    /**
     * This method handles updating and initializing a DroneStatus object
     * upon receiving a UDP package from a drone.
     * @param data the byte array from the UDP datagram.
     * @param length the length of the byte array.
     * @param address the IP from the datagram.
     * @param port the port from the datagram.
     */
    private void handleDroneStatus(byte[] data, int length, InetAddress address, int port) {

        String message = new String(data, 0, length).trim();
        String[] parts = message.split(",");


        if (message.isEmpty()) {
            System.out.println("Ignoring empty packet");
            return;
        }

        if (parts[0].equals("FAULT")) {
            System.out.println("[Scheduler] Received explicit fault packet: " + message);
            return;
        }

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

        switch(status) {
            case FAULTED:
            case OUT_OF_SERVICE: {
                FireEvent assignedEvent = activeAssignments.remove(droneId);
                assignmentStartTimes.remove(droneId);

                if (assignedEvent != null) {
                    FireEvent failed = new FireEvent(
                            assignedEvent.getZoneID(),
                            assignedEvent.getTaskType(),
                            LocalTime.now(),
                            assignedEvent.getSeverity(),
                            FaultType.NONE
                    );

                    try {
                        put(failed);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case IDLE:
                activeAssignments.remove(droneId);
                assignmentStartTimes.remove(droneId);
                break;
            default:
                break;
        }

        System.out.println("[Scheduler] Drone " + droneId +
                " Status: " + status +
                " Position: (" + posX + "," + posY + ")" +
                " Water: " + water);
    }

    /**
     * Helper method to combine bytes into an integer, since
     * the integer will take up two bytes - requires recombination
     * @param high the high byte from the byte array
     * @param low the low byte from the byte array
     * @return the integer represented by these two bytes together
     */
    private int combineBytes(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    /**
     * Helper method to separate an integer value into two separate bytes to be sent
     * in UDP Datagrams.
     * @param data the byte array to add the data to
     * @param offset the offset in the byte array
     * @param value the integer value to separate into bytes
     */
    private void separateBytes(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 8);      // high byte
        data[offset + 1] = (byte) value;         // low byte
    }

    /**
     * Scheduler's implementation of run, wait to receive a packet from the
     * subsystem or a drone, and then process the packet. Continuously
     * handle fire events in the buffer in the meantime.
     */
    @Override
    public void run() {
        try {
            synchronized (this) { // prevents a race on this method instance from within the object
                while (!this.getAllTasksSent() && !this.getAllTasksProcessed()) {
                    this.receivePacket();
                    this.assignDroneEvent();
                }
                System.out.println("[SCHEDULER] All tasks marked complete by incident subsystem");
            }
        } catch (Exception e) {
            e.printStackTrace();
            socket.close();
        }
    }

    /**
     * Helper method to test scheduler algorithm assigns priorities correctly.
     * @return current highest priority FireEvent
     */
    //public FireEvent getCurrentEvent(){
        //return this.currentEvent;
    //}

    /**
     * Helper function to initialize zone map within the class, now that
     * separate main methods will be used.
     * @param zones an ArrayList of Zone objects.
     * @return the ArrayList converted to a map with IDs as keys, and zones as values.
     */
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
