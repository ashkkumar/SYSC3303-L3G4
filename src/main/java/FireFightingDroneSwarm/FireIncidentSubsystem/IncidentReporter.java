package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.Scheduler.Scheduler;
import FireFightingDroneSwarm.UserInterface.ZoneMapController;
import FireFightingDroneSwarm.UserInterface.ZoneMapView;

import java.time.Duration;
import java.util.ArrayList;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * This class represents the Fire Incident reporting thread.
 * It instantiates its reader objects, and uses the ArrayLists
 * returned to it in order to pass on events to the scheduler.
 */
public class IncidentReporter implements Runnable {
    private InputReader inputReader;
    private Scheduler scheduler;
    private ArrayList<FireEvent> events;
    private ArrayList<Zone> zones;
    private ArrayList<Long> timeBetweenEvents;
    private int nextEvent;
    private final double TIME_SCALE = 0.05;
    private ZoneMapController zoneMapController;

    private DatagramSocket socket;
    private InetAddress schedulerIP;
    private int schedulerPort = 50000;

    /**
     * Constructor for this object, takes in an instantiated InputReader
     * and a shared Scheduler
     * @param inputReader InputReader with file fields instantiated
     * @param scheduler Scheduler object for event sharing
     */
    public IncidentReporter(InputReader inputReader, Scheduler scheduler, ZoneMapController zoneMapController) {
        this.inputReader = inputReader;
        this.scheduler = scheduler;
        this.zoneMapController = zoneMapController;
        nextEvent = 0;
        this.initializeSystem();
    }

    /**
     * new Constructor with UDP socket to instantiate a IncidentReporter object
     * @param inputReader
     * @param zoneMapController
     */
    public IncidentReporter(InputReader inputReader, ZoneMapController zoneMapController) {
        this.inputReader = inputReader;
        this.zoneMapController = zoneMapController;

        try {
            socket = new DatagramSocket();
            schedulerIP = InetAddress.getByName("localhost");
        } catch (Exception e) {
            e.printStackTrace();
        }
        nextEvent = 0;
        this.initializeSystem();
    }

    /**
     * This function initializes the zones, and
     * events specific to this incident subsystem.
     */
    private void initializeSystem(){
        try{
            events = inputReader.parseEventFile();
            zones = inputReader.parseZoneFile();
            timeBetweenEvents = new ArrayList<>();

            for(Zone z : zones){
                System.out.println(z.toString());
            }

            for(FireEvent e : events){
                System.out.println(e.toString());
            }

            for(int i  = 0; i < events.size() - 1; i++){
                Duration durationBetween = Duration.between(events.get(i).getTimestamp(),
                        events.get(i+1).getTimestamp());
                long seconds = durationBetween.getSeconds() * 1000;
                timeBetweenEvents.add(seconds);
            }

            timeBetweenEvents.add(0L);

            if(zoneMapController != null){
                zoneMapController.initializeZones(zones);
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Implementation of run() for this object to invoke as a thread
     * in order to place events from its ArrayList currently
     * just in order
     */
    @Override
    public void run() {
        while(nextEvent < events.size()){
            FireEvent event = events.get(nextEvent);
            try {
                sendEvent(event);
                if (zoneMapController != null) {
                    zoneMapController.fireDetected(event.getZoneID());
                }
                Thread.sleep((long) (timeBetweenEvents.get(nextEvent) * TIME_SCALE));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            nextEvent++;
        }
        allEventsSent();
    }

    /**
     * Function to be called by scheduler when an event is passed back by
     * a drone
     * @param event the event confirmed/completed by a drone
     */
    public void getEventConfirmation(FireEvent event){
        System.out.println("[Incident Subsystem] recieved event confirmation " + event.toString());
    }


    /**
     * Helper method to find object reference to Zone by id for controller methods
     * @param id the id of the Zone object you wish to reference
     * @return the Zone object that matches, null otherwise
     */
    private Zone getZoneById(int id) {
        for (Zone z : zones) {
            if (z.getID() == id) {
                return z;
            }
        }
        return null;
    }

    /**
     * Sends a fire event to the Scheduler subsystem using a UDP packet.
     * The packet format is a simple byte array where:
     * byte[0] = message type (1 = FIRE_EVENT) we can change this tho just
     * leaving it as a number rn
     * byte[1] = zone ID
     * byte[2] = severity level
     * byte[3] = task type
     * byte[4..5] = zone start X coordinate
     * byte[6..7] = zone start Y coordinate
     * byte[8..9] = zone end X coordinate
     * byte[10..11] = zone end Y coordinate
     * This method replaces the direct Scheduler method calls used in
     * previous iterations. Instead of calling scheduler.put(event),
     * the event is serialized into a byte array and transmitted over
     * the network to the Scheduler process.
     *
     * @param event the FireEvent object that should be reported to the Scheduler
     */
    private void sendEvent(FireEvent event){
            try {
                Zone zone = getZoneById(event.getZoneID());

                if(zone == null){
                    System.out.println("[Incident Subsystem] Zone " + event.getZoneID() + " not found");
                    return;
                }

                // get the x,y coordinates for start and end positions to send over udp
                int startX = zone.getStartCoordinates()[0];
                int startY = zone.getStartCoordinates()[1];
                int endX   = zone.getEndCoordinates()[0];
                int endY   = zone.getEndCoordinates()[1];

                byte[] data = new byte[12];

                // data[0] = message type as an int (we can change this)
                data[0] = 1;

                // data[1] = zone id
                data[1] = (byte) event.getZoneID();

                // data[2] = severity and data[3] = task type
                data[2] = (byte) event.getSeverity().ordinal();
                data[3] = (byte) event.getTaskType().ordinal();

                // data[4..5] = startX coordinate
                separateBytes(data, 4, startX);

                // data[6..7] = startY coordinate
                separateBytes(data, 6, startY);

                // data[8..9] = endX coordinate
                separateBytes(data, 8, endX);

                // data[10..11] = endY coordinate
                separateBytes(data, 10, endY);

                DatagramPacket packet = new DatagramPacket(data, data.length, schedulerIP, schedulerPort);
                socket.send(packet);

                System.out.println("[Incident Subsystem] Sent event " + event.getZoneID() + " " + event.getSeverity());
            } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a UDP packet to the Scheduler indicating that all fire
     * events from the input file have now been transmitted.
     *
     * Packet format:
     * byte[0] = message type (2 = ALL_EVENTS_SENT)
     */
    private void allEventsSent(){
        try{
            byte[] data = new byte[1];
            data[0] = 2;

            DatagramPacket packet = new DatagramPacket(data, data.length, schedulerIP, schedulerPort);
            socket.send(packet);
            System.out.println("[Incident Subsystem] Sent all events " + events.size());
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * the coordinates are too large to be in a single byte if
     * coordinate is > 255, need to send coordinates split up
     * @param data
     * @param offset
     * @param value
     */
    private void separateBytes(byte[] data, int offset, int value){
        data[offset] = (byte) (value >> 8);
        data[offset + 1] = (byte) value;

        /**
         * Maryam this is for u to make your life easier lol
         * private int combineBytes(byte[] data, int offset){
         *     return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
         * }
         * this function helps decode the bytes seperated
         */
    }

    public static void main(String[] args) {

        InputReader inputReader =
                new InputReader("sample_event_multiple.csv",
                        "sample_zone_multiple.csv");
        ZoneMapController zoneMapController = new ZoneMapController(new ZoneMapView());
        Thread incidentReporter = new Thread(new IncidentReporter(inputReader, zoneMapController));
        incidentReporter.start();
    }

}
