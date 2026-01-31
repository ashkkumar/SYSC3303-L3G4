package FireFightingDroneSwarm;


import FireFightingDroneSwarm.DroneSubsystem.Drone;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.IncidentReporter;
import FireFightingDroneSwarm.FireIncidentSubsystem.InputReader;
import FireFightingDroneSwarm.Scheduler.Scheduler;

import java.io.InputStream;

/**
 * Main system test class. Instantiate Scheduler, IncidentReporter,
 * Drone, and InputReader, hook up dependencies, and start threads.
 */
public class DroneSystemTest {
    public static void main(String[] args) throws InterruptedException {
        singleIncident();
        multipleIncidents();
    }

    public static void singleIncident() throws InterruptedException {
        System.out.println("=== Starting Iteration 1 System Test ===");
        Scheduler scheduler = new Scheduler(10);
        InputReader inputReader =
                new InputReader("sample_event_file.csv", "sample_zone_file.csv");
        IncidentReporter incidentReporter = new IncidentReporter(inputReader, scheduler);
        Drone drone = new Drone(1, scheduler);
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

    public static void multipleIncidents() throws InterruptedException {
        System.out.println("\n=== Starting Iteration 1 System Test: Multiple Incidents ===");

        Scheduler scheduler = new Scheduler(15);

        InputReader inputReader =
                new InputReader("sample_event_multiple.csv",
                        "sample_zone_multiple.csv");

        IncidentReporter incidentReporter = new IncidentReporter(inputReader, scheduler);
        Drone drone = new Drone(1, scheduler);

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

}