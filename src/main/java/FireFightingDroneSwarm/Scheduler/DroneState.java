package FireFightingDroneSwarm.Scheduler;
import FireFightingDroneSwarm.DroneSubsystem.DroneStatus;
import java.net.InetAddress;

/**
 * This class is a helper class for the Scheduler to keep
 * track of current drones in the swarm. As the scheduler will not keep
 * references to objects anymore, this class can be used instead.
 */
public class DroneState {

    private int droneId;
    private DroneStatus status;
    private double posX;
    private double posY;
    private int waterTank;

    private InetAddress address;
    private int port;

    /**
     * Constructor for DroneState to instantiate a status for the drone
     * @param droneId int representing droneId
     * @param status DroneStatus enum value
     * @param posX double representing current X of the drone
     * @param posY double representing current Y of the drone
     * @param waterTank int representing water tank fullness
     * @param address InetAddress found from UDP datagram from this drone
     * @param port port found from UDP datagram from this drone
     */
    public DroneState(int droneId, DroneStatus status,
                      double posX, double posY,
                      int waterTank,
                      InetAddress address, int port) {

        this.droneId = droneId;
        this.status = status;
        this.posX = posX;
        this.posY = posY;
        this.waterTank = waterTank;
        this.address = address;
        this.port = port;
    }

    /**
     * Getter for the IP of this DroneStatus representation of the drone
     * @return InetAddress from the last UDP datagram from this drone
     */
    public InetAddress getAddress() { return address; }

    /**
     * Getter for the port number of this DroneStatus representation of the drone
     * @return int from the last UDP datagram from this drone representing port #
     */
    public int getPort() { return port; }

    /**
     * Getter for the current status of the drone
     * @return DroneStatus enum value
     */
    public DroneStatus getStatus() { return status; }

    /**
     * Getter for the x-coordinate of the drone
     * @return double representing x position
     */
    public double getPosX() { return posX; }

    /**
     * Getter for the y-coordinate of the drone
     * @return double representing y position
     */
    public double getPosY() { return posY; }

    /**
     * Getter for the drone ID
     * @return int corresponding to the drone id
     */
    public int getDroneId() { return droneId; }

    public int getWaterTank() { return waterTank; }
    /**
     * Update method to update the drone's status when it has a state change
     * @param status DroneStatus value for state change
     * @param posX double representing new X coordinate
     * @param posY double representing new Y coordinate
     * @param waterTank int representing new water tank value
     */
    public void update(DroneStatus status, double posX, double posY, int waterTank) {
        this.status = status;
        this.posX = posX;
        this.posY = posY;
        this.waterTank = waterTank;
    }

    public String toString() {
        return "DroneState [droneId=" + droneId + ", status=" + status + ", posX=" + posX + ", posY=" + posY + ", waterTank=" + waterTank + ", address=" + address + ", port=" + port + "]";
    }

}