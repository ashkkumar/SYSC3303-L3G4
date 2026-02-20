package FireFightingDroneSwarm.FireIncidentSubsystem;

import FireFightingDroneSwarm.Scheduler.Scheduler;
import FireFightingDroneSwarm.UserInterface.ZoneMapController;

import java.time.Duration;
import java.util.ArrayList;

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
                scheduler.put(event);
                if (zoneMapController != null) {
                    zoneMapController.fireDetected(event.getZoneID());
                }
                Thread.sleep((long) (timeBetweenEvents.get(nextEvent) * TIME_SCALE));

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            nextEvent++;
        }
        scheduler.setAllTasksSent(true);
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

}
