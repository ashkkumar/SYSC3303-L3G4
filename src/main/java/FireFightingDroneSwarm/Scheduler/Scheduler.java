package FireFightingDroneSwarm.Scheduler;

import FireFightingDroneSwarm.DroneSubsystem.DroneTask;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;

public class Scheduler {
    public Scheduler(){}

    public void confirmation(DroneTask droneTask){
        System.out.println("Recieved confirmation of drone task");
    }

    public void notify(FireEvent fireEvent){
        System.out.println("Recieved notify of fire event");
        System.out.println(fireEvent);
    }


}
