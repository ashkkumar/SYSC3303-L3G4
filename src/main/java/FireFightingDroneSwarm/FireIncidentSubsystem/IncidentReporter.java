package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.Events.EventLogger;
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
    private ZoneMapController zoneMapController = null;

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
     */
    public IncidentReporter(InputReader inputReader) {
        this.inputReader = inputReader;

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

            for(Zone z : zones){
                sendZoneToGUI(z);
            }

            // logger.Log("Incident Reporter", "INIT", "Incident Reporter Initialization");

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
                    zoneMapController.fireDetected(event.getZoneID(), 1);
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

            // increased size from 12 -> 13 due to added fault type byte
            byte[] data = new byte[13];

            // data[0] = message type
            data[0] = 1;

            // data[1] = zone id
            data[1] = (byte) event.getZoneID();

            // data[2] = severity, data[3] = task type
            data[2] = (byte) event.getSeverity().ordinal();
            data[3] = (byte) event.getTaskType().ordinal();

            // data[4] = fault type
            data[4] = (byte) event.getFault().ordinal();

            // data[5..6] = startX coordinate
            separateBytes(data, 5, startX);

            // data[7..8] = startY coordinate
            separateBytes(data, 7, startY);

            // data[9..10] = endX coordinate
            separateBytes(data, 9, endX);

            // data[11..12] = endY coordinate
            separateBytes(data, 11, endY);

            DatagramPacket packet = new DatagramPacket(data, data.length, schedulerIP, schedulerPort);

            String guiMessage = "FIRE_EVENT," + event.getZoneID();
            byte[] guiBytes = guiMessage.getBytes();
            DatagramPacket guiPacket = new DatagramPacket(guiBytes, guiBytes.length, InetAddress.getLocalHost(), 60000);

            socket.send(packet);
            socket.send(guiPacket);

            System.out.println("[Incident Subsystem] Sent event " + event.getZoneID() + " " + event.getSeverity());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a UDP packet to the Scheduler indicating that all fire
     * events from the input file have now been transmitted.
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
     * @param data the byte array to separate
     * @param offset the offset in the array to use as index
     * @param value the integer value to parse
     */
    private void separateBytes(byte[] data, int offset, int value){
        data[offset] = (byte) (value >> 8);
        data[offset + 1] = (byte) value;
    }

    /**
     * Helper method to help initialize zones on the GUI by sending as UDP datagram
     * @param zone the zone to send to the GUI
     */
    private void sendZoneToGUI(Zone zone) {
        try {
            String msg = "ZONE_INIT," +
                    zone.getID() + "," +
                    zone.getStartCoordinates()[0] + "," +
                    zone.getStartCoordinates()[1] + "," +
                    zone.getEndCoordinates()[0] + "," +
                    zone.getEndCoordinates()[1];

            byte[] data = msg.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 60000);

            socket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        EventLogger logger = new EventLogger(1000);
        InputReader inputReader =
                new InputReader("sample_event_multiple.csv",
                        "sample_zone_multiple.csv");
        Thread incidentReporter = new Thread(new IncidentReporter(inputReader));
        incidentReporter.start();
    }

}
