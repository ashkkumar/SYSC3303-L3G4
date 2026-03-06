package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.Scheduler.Scheduler;
import FireFightingDroneSwarm.UserInterface.ZoneMapController;

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
    private int schedulerPort = 5000;

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
     *
     * @param inputReader
     * @param zoneMapController
     */
    public IncidentReporter(InputReader inputReader, ZoneMapController zoneMapController) {
        this.inputReader = inputReader;
        this.zoneMapController = zoneMapController;

        try {
            socket = new DatagramSocket();
            schedulerIP = InetAddress.getLocalHost();
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

            byte[] data = new byte[4];

            data[0] = 1;
            data[1] = (byte) event.getZoneID();
            data[2] = (byte) event.getSeverity().ordinal();
            data[3] = (byte) event.getTaskType().ordinal();
            /* should we send the zone datas over too? or should
            the scheduler do that now?
            "The Scheduler will now be used to coordinate the movement of drone such
            that each drone services roughly the same number of zones as all of the others and so that the waiting time for
            fires to be extinguished in a zone is minimized."
            */
            DatagramPacket packet = new DatagramPacket(data, data.length, schedulerIP, schedulerPort);
            socket.send(packet);

            System.out.println("[Incident Subsystem] Sent event " + event.getZoneID() + " " + event.getSeverity());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

}
