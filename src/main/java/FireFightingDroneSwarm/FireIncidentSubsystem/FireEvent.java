package FireFightingDroneSwarm.FireIncidentSubsystem;
import java.time.LocalTime;

public class FireEvent {
    private int zoneID;
    private TaskType taskType;
    private LocalTime timestamp;
    private Severity severity;

    public FireEvent(int zone, TaskType taskType, LocalTime timestamp, Severity severity) {
        this.zoneID = zone;
        this.taskType = taskType;
        this.timestamp = timestamp;
        this.severity = severity;
    }

    public int getZoneID() {
        return zoneID;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public LocalTime getTimestamp() {
        return timestamp;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String toString() {
        return "FireEvent [zoneID=" + zoneID + ", taskType=" + taskType + ", timestamp=" +
                timestamp + ", severity=" + severity + "]";
    }
}
