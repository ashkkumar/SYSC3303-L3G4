package FireFightingDroneSwarm.FireIncidentSubsystem;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import java.time.LocalTime;

/**
 * This class is the template class for holding event data when read in
 * from the event csv file. Each event has a timestamp, zone information,
 * and severity infroamtion.
 */
public class FireEvent {
    private int zoneID;
    private TaskType taskType;
    private LocalTime timestamp;
    private Severity severity;
    private Scheduler scheduler;

    /**
     * Constructor for a FireEvent object, to represent incoming fire events.
     * @param zone int representing the zone of the fire
     * @param taskType enum value of TaskType
     * @param timestamp LocalTime representation of timestamp
     * @param severity enum value of Severity
     */
    public FireEvent(int zone, TaskType taskType, LocalTime timestamp, Severity severity) {
        this.zoneID = zone;
        this.taskType = taskType;
        this.timestamp = timestamp;
        this.severity = severity;
    }

    /**
     * Getter for this event's particular zone
     * @return int representing the zone of the fire
     */
    public int getZoneID() {
        return zoneID;
    }

    /**
     * Getter for this event's task type, possible values defined in TaskType
     * @return TaskType enum value
     */
    public TaskType getTaskType() {
        return taskType;
    }

    /**
     * Getter for this event's timestamp as a LocalTime object in hh:mm:ss format
     * @return LocalTime with hh:mm:ss values defined
     */
    public LocalTime getTimestamp() {
        return timestamp;
    }

    /**
     * Getter for this event's severity, possible values defined in Severity
     * @return Severity enum value
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Override toString() method to return a representation of this event and its fields
     * @return String representation of this event
     */
    public String toString() {
        return "FireEvent [zoneID=" + zoneID + ", taskType=" + taskType + ", timestamp=" +
                timestamp + ", severity=" + severity + "]";
    }
}