package FireFightingDroneSwarm.FireIncidentSubsystem;
import FireFightingDroneSwarm.DroneSubsystem.FaultType;
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
    private FaultType fault;
    private int fireID;
    private boolean preServiced = false;

    /**
     * Constructor for a FireEvent object, to represent incoming fire events.
     * @param zone int representing the zone of the fire
     * @param taskType enum value of TaskType
     * @param timestamp LocalTime representation of timestamp
     * @param severity enum value of Severity
     */
    public FireEvent(int zone, TaskType taskType, LocalTime timestamp, Severity severity, FaultType fault, int fireID) {
        this.zoneID = zone;
        this.taskType = taskType;
        this.timestamp = timestamp;
        this.severity = severity;
        this.fault = fault;
        this.fireID = fireID;
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


    public FaultType getFault() {return  fault;}

    /**
     * Setter for fault, important for scheduler to readd fire event
     * @param fault
     */
    public void setFault(FaultType fault) {this.fault=fault;}

    /**
     * Gets the preserviced variable, for sending multiple drones to 1 fire
     * @return
     */
    public boolean isPreServiced() {
        return preServiced;
    }

    /**
     * Sets the preserviced variable, for sending multiple drones to 1 fire
     * @param preServiced
     */
    public void setPreServiced(boolean preServiced) {
        this.preServiced = preServiced;
    }

    /**
     * Override toString() method to return a representation of this event and its fields
     * @return String representation of this event
     */
    public String toString() {
        return "FireEvent [zoneID=" + zoneID + ", taskType=" + taskType + ", timestamp=" +
                timestamp + ", severity=" + severity + ", fault=" + fault + ", fireID=" + fireID + "]";
    }

    public int getFireID() {
        return fireID;
    }
}