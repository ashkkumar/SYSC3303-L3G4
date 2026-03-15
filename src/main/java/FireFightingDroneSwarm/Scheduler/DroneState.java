package FireFightingDroneSwarm.Scheduler;
import FireFightingDroneSwarm.DroneSubsystem.DroneStatus;
import java.net.InetAddress;

public class DroneState {

    private int droneId;
    private DroneStatus status;
    private double posX;
    private double posY;
    private int waterTank;

    private InetAddress address;
    private int port;

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

    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }
    public DroneStatus getStatus() { return status; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public int getDroneId() { return droneId; }
    public void update(DroneStatus status, double posX, double posY, int waterTank) {
        this.status = status;
        this.posX = posX;
        this.posY = posY;
        this.waterTank = waterTank;
    }
}