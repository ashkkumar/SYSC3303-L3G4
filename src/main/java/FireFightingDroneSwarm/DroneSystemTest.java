package FireFightingDroneSwarm;


import FireFightingDroneSwarm.FireIncidentSubsystem.InputReader;
import FireFightingDroneSwarm.Scheduler.Scheduler;

public class DroneSystemTest {
    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        InputReader inputReader = new InputReader("/Users/maryam/Documents/SYSC3303-L3G4/src/resources/sample_event_file.csv",
                scheduler);
    }
}