package FireFightingDroneSwarm;


import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.IncidentReporter;
import FireFightingDroneSwarm.FireIncidentSubsystem.InputReader;
import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import FireFightingDroneSwarm.UserInterface.ZoneMapController;
import FireFightingDroneSwarm.UserInterface.ZoneMapView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Main system test class. Instantiate Scheduler, IncidentReporter,
 * Drone, and InputReader, hook up dependencies, and start threads.
 */
public class DroneSystemTest {
    public static void main(String[] args) throws InterruptedException {
        multipleIncidents();
    }

    public static void multipleIncidents() throws InterruptedException {
        System.out.println("\n=== Starting Iteration 1 System Test: Multiple Incidents ===");

        ZoneMapView GUI = new ZoneMapView();
        ZoneMapController controller = new ZoneMapController(GUI);
        Scheduler scheduler = new Scheduler(15);

        InputReader inputReader =
                new InputReader("sample_event_multiple.csv",
                        "sample_zone_multiple.csv");

        ArrayList<Zone> zones = inputReader.parseZoneFile();
        scheduler.setZoneIDs(buildZoneMap(zones));

        IncidentReporter incidentReporter = new IncidentReporter(inputReader, scheduler, controller);

        Drone drone = new Drone(1, scheduler, controller);

        scheduler.setDrone(drone);
        scheduler.setIncidentReporter(incidentReporter);

        Thread schedulerThread = new Thread(scheduler);
        Thread incidentThread = new Thread(incidentReporter);
        Thread droneThread = new Thread(drone);

        schedulerThread.start();
        incidentThread.start();
        droneThread.start();

        schedulerThread.join();
        incidentThread.join();
        droneThread.join();

    }

    private static Map<Integer, Zone> buildZoneMap(ArrayList<Zone> zones) {
        Map<Integer, Zone> map = new HashMap<>();
        for (Zone z : zones) {
            map.put(z.getID(), z);
        }
        return map;
    }


}