package FireFightingDroneSwarm.DroneSubsystem;

import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;

public class DroneTask {

        private final FireEvent fireEvent;
        private final TaskType taskType;

    /**
     * Creates a Drone Task
     * @param fireEvent
     * @param taskType
     */
    public DroneTask(FireEvent fireEvent, TaskType taskType) {
            this.fireEvent = fireEvent;
            this.taskType = taskType;
        }

    /**
     * Returns the fire event
     * @return the fireEvent
     */
    public FireEvent getFireEvent() {
            return fireEvent;
        }

    /**
     * Returns the task type
      * @return the taskType
     */
    public TaskType getTaskType() {
        return taskType;
    }


}
