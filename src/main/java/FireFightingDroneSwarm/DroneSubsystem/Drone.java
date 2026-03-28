package FireFightingDroneSwarm.DroneSubsystem;
import FireFightingDroneSwarm.FireIncidentSubsystem.FireEvent;
import FireFightingDroneSwarm.FireIncidentSubsystem.Severity;
import FireFightingDroneSwarm.Scheduler.Scheduler;
import FireFightingDroneSwarm.UserInterface.ZoneMapController;
import FireFightingDroneSwarm.FireIncidentSubsystem.TaskType;

import java.io.IOException;
import java.net.*;


public class Drone implements Runnable {

    private final int droneId;
    private volatile DroneStatus status;
    public FireEvent currentTask;
    private Scheduler scheduler = null;
    private static final double DRONE_SPEED = 80.0; // units per second (Iteration 0)
    private int waterTank;
    private static final int MAX_TANK = 100;
    private boolean hasFuel = true;
    double[] zoneCenter = new double[2];
    private FaultType injectedFault = FaultType.NONE;
    private boolean faultTriggered = false;
    private int faultSleepTime = 2000;

    //UDP
    DatagramPacket sendPacket, receivePacket;
    DatagramSocket sendReceiveSocket;

    // Drone position (start at base)
    private double posX;
    private double posY;

    //Drone zone
    private int zone;

    // Base position (choose whatever your sim assumes; often 0,0)
    private static final double BASE_X = 0;
    private static final double BASE_Y = 0;

    // target location
    private double targetX;
    private double targetY;
    private ZoneMapController zoneMapController;



    /**
     * @param droneId    ID to represent a drone object
     * @param scheduler  The Scheduler the drone communicates with
     * @param zoneMapController
     */
    public Drone(int droneId, Scheduler scheduler, ZoneMapController zoneMapController) {
        this.droneId = droneId;
        this.scheduler = scheduler;
        this.status = DroneStatus.IDLE;
        this.zoneMapController = zoneMapController;
        this.posX = BASE_X;
        this.posY = BASE_Y;
        this.zone = 0;
        this.waterTank = MAX_TANK;

        try {
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Drone(int droneId) {
        this.droneId = droneId;
        this.status = DroneStatus.IDLE;
        this.posX = BASE_X;
        this.posY = BASE_Y;

        this.waterTank = MAX_TANK;

        try {
            sendReceiveSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * The drone continuously sends its status to the
     * scheduler so that the scheduler can effectively schedule tasks for the drone.
     */
    @Override
    public void run() {

        while (status != DroneStatus.OUT_OF_SERVICE) {
            try {
                String statusMsg =
                        droneId + "," +
                                status + "," +
                                posX + "," +
                                posY + "," +
                                waterTank;

                sendStatus(statusMsg);
                receiveFireEvent();

                Thread.sleep(5000);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Executes the assigned drone task by simulating
     * flight, extinguishing, and return
     * subject to change
     */
    void executeTask() {
        System.out.println("[Drone " + droneId + "] Dispatched to zone "
                + currentTask.getZoneID());

        double[] xy = zoneCenter;
        this.targetX = xy[0];
        this.targetY = xy[1];

        transition(DroneStatus.EN_ROUTE);
        this.sendGuiUpdate("DRONE_DISPATCHED", currentTask.getZoneID());


        if (zoneMapController != null) {
            zoneMapController.droneDispatched(currentTask.getZoneID());
        }

        injectedFault = currentTask.getFault();

        boolean arrived = travelTo(targetX, targetY);
        if (!arrived) {
            transition(DroneStatus.RETURNING);
            this.sendGuiUpdate("DRONE_RETURNING", currentTask.getZoneID());
            boolean returned = travelTo(BASE_X, BASE_Y);
            if (!returned) {
                return;
            }
            transition(DroneStatus.IDLE);
            System.out.println("[Drone " + droneId + "] returned to base");

            return;
        }
        transition(DroneStatus.ARRIVED);

        transition(DroneStatus.DROPPING_AGENT);
        boolean success = extinguish(currentTask.getSeverity());

        if (!success) {
            System.out.println("Drone Couldn't drop agent, Fault occured");
            return;
        }

        int amountUsed = calculateWaterUsage(currentTask.getSeverity());
        this.waterTank -= amountUsed;
        /**
        if(this.waterTank <= 0 || !remainingFlight()) {
            System.out.println("[Drone " + droneId + "] Tank empty or low fuel. Returning.");
            transition(DroneStatus.RETURNING);
            travelTo(BASE_X, BASE_Y);
            transition(DroneStatus.REFILLING);
            refill();
         here it returns if the tank is empty but then after the block it returns again
        }
        else{

            System.out.println("[Drone " + droneId + "] Remaining water: " + waterTank + ". Staying in field.");
            transition(DroneStatus.IDLE);
        }
         this chunk is kinda messed up? if tank is not empty then we idle? but idle -> returning is illegal
        **/
        transition(DroneStatus.RETURNING);
        this.sendGuiUpdate("DRONE_RETURNING", currentTask.getZoneID());


        if (zoneMapController != null) {
            zoneMapController.droneReturning(currentTask.getZoneID());
        }
        boolean returned = travelTo(BASE_X, BASE_Y);
        if (!returned) {
            return;
        }

        transition(DroneStatus.REFILLING);
        refill();

        transition(DroneStatus.IDLE);
        System.out.println("[Drone " + droneId + "] returned to base");


        // zone = currentTask.getZoneID(); for when we implement refilling, if the tank isn't full it will stay and go to idle
    }

    /**
     * Validates if the new status is a valid transiton
     * @param newStatus The new state of the drone
     */
    synchronized void transition(DroneStatus newStatus) {
        System.out.println("[Drone " + droneId + " " + status + "] Transitioning to " + newStatus);

        if ( newStatus == DroneStatus.OUT_OF_SERVICE) {
            status = newStatus;

            String statusMsg =
                    droneId + "," +
                    status + "," +
                    posX + "," +
                    posY + "," +
                    waterTank;
            sendStatus(statusMsg);
            return;
        }
        // we wanna check that the transition is valid, no illegal moves
        // IDLE - > EN_ROUTE -> ARRIVED -> DROPPING_AGENT -> EN_ROUTE or RETURNING -> REFILLING -> IDLE
        switch (status){
            case IDLE:
                if (newStatus != DroneStatus.EN_ROUTE && newStatus != DroneStatus.FAULTED) return;
                break;
            case EN_ROUTE:
                if (newStatus != DroneStatus.ARRIVED &&
                        newStatus != DroneStatus.FAULTED ) return;
                break;
            case ARRIVED:
                if (newStatus != DroneStatus.DROPPING_AGENT) return;
                break;
            case DROPPING_AGENT:
                if (newStatus != DroneStatus.RETURNING &&
                        newStatus != DroneStatus.EN_ROUTE) return;
                break;
            case RETURNING:
                if (newStatus != DroneStatus.REFILLING && newStatus != DroneStatus.FAULTED) return;
                break;
            case REFILLING:
                if (newStatus != DroneStatus.IDLE) return;
                break;
            case FAULTED:
                if (newStatus != DroneStatus.RETURNING) return;
                break;
            case OUT_OF_SERVICE:
                return;
        }

        status = newStatus;
        String statusMsg =
                droneId + "," +
                        status + "," +
                        posX + "," +
                        posY + "," +
                        waterTank;
        sendStatus(statusMsg);
    }

    /**
     * Simulates fire extinguishing time based on fire severity.
     * Now if theres a fault detected with the nozzle it'll put the drone out of service
     *
     * @param severity the severity level of the fire
     */
    private boolean extinguish(Severity severity) {

        if (injectedFault == FaultType.NOZZLE_JAM && !faultTriggered) {
            faultTriggered = true;

            System.out.println("[Drone " + droneId + "] Fault triggered Nozzle Jam.");
            transition(DroneStatus.OUT_OF_SERVICE);
            sendFaultStatus("NOZZLE_JAM");

            return false;
        }

        int extinguishTime;

        // need a calc
        switch (severity) {
            case LOW -> extinguishTime = 1000;
            case MODERATE -> extinguishTime = 2000;
            case HIGH -> extinguishTime = 3500;
            default -> extinguishTime = 1500;
        }

        sleep(extinguishTime);
        return true;
    }

    /**
     * Simulates the return flight after task completion.
     * Changed it to update the drone position gradually
     * easier to log now
     */
    private boolean travelTo(double x, double y) {
        double distance =  calculateDistanceToZone(x, y);

        double dx = x - posX;
        double dy = y - posY;

        long totalTime =  (long) ((distance / DRONE_SPEED) * 1000);

        int steps = 10;
        double stepX = dx / steps;
        double stepY = dy / steps;
        long stepTime = totalTime / steps;

        for (int i = 0; i < steps; i++) {

            if (injectedFault == FaultType.STUCK_MID_FLIGHT && !faultTriggered
            && i >= steps / 2) {
                faultTriggered = true;
                System.out.println("[Drone " + droneId + "] Fault triggered: stuck mid flight");

                transition(DroneStatus.FAULTED);
                sendFaultStatus("STUCK_MID_FLIGHT");

                sleep(faultSleepTime);

                return false;
            }
            posX += stepX;
            posY += stepY;

            sendStatus(droneId + "," + status + "," +posX + "," + posY + "," + waterTank);
            sleep((int) stepTime);
        }

        posX = x;
        posY = y;
        return true;

    }

    /**
     * simulates refilling time (2 second)
     */
    private void refill() {
        sleep(2000);
    }

    /**
     * Determines the amount of water/agent to dispense based on the fire's severity.
     * @param severity The severity level of the fire event.
     * @return The integer amount of water units to be used.
     */
    private int calculateWaterUsage(Severity severity) {
        return switch (severity) {
            case LOW -> 20;
            case MODERATE -> 30;
            case HIGH -> 50;
        };
    }

    /**
     * Checks if the drone has enough flight capability (fuel) to continue operating.
     * @return true if the drone has sufficient fuel, false if it must return to base.
     */
    private boolean remainingFlight() {
        return this.hasFuel;
    }

    /**
     * Calculates the travel distance to coordinates x, y
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the distance to be travelled
     */
    private double calculateDistanceToZone(double x, double y) {

        double dx =  x - posX;
        double dy = y - posY;

        return Math.sqrt(dx * dx + dy * dy);  // simple distance model
    }

    /**
     * Just had to put this here because I do the calculations yet mb
     * actual times going to be based off one of the drones
     * @param ms duration to sleep in milliseconds
     */
    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the X position of the drone
     * @return double - x coordinate
     */
    public double getPosX() { return posX; }

    /**
     * Returns the Y position of the drone
     * @return double - y coordinate
     */
    public double getPosY() { return posY; }

    /**
     * Returns the status of the drone
     * @return the drone status DroneStatus.IDLE / EN_ROUTE etc
     */
    public DroneStatus getStatus() { return status; }
    public int getWaterTank() { return waterTank; } // optional

    /**
     * Sends the status (as a string) of the drone to the Scheduler server
     * @param statusMsg The status string being sent
     */
    public void sendStatus(String statusMsg) {

        if (injectedFault == FaultType.PACKET_LOSS && !faultTriggered) {
            faultTriggered = true;
            System.out.println("[Drone " + droneId + "] Fault triggered: packet loss");
            return;
        }

        System.out.println("Sending drone status: " + statusMsg);
        byte msg[] = statusMsg.getBytes();

        try {
            sendPacket = new DatagramPacket(msg, msg.length, InetAddress.getLocalHost(), 50000);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public double[] getZoneCenter(int startX, int startY, int endX, int endY) {
        int[] s = {startX, startY};
        int[] e = {endX, endY};
        return new double[]{ (s[0] + e[0]) / 2.0, (s[1] + e[1]) / 2.0 };
    }

    /**
     * Method to receive fire event on UDP socket from the scheduler and to begin
     * state execution.
     *
     * Expected packet format:
     * byte[0] = message type (3 = ASSIGNMENT)
     * byte[1] = zone ID
     * byte[2] = severity level
     * byte[3] = task type
     * byte[4] = fault type
     * byte[5..6] = zone start X coordinate
     * byte[7..8] = zone start Y coordinate
     * byte[9..10] = zone end X coordinate
     * byte[11..12] = zone end Y coordinate
     */
    public void receiveFireEvent() {

        try {

            // FIX 1: increase size from 12 -> 13
            receivePacket = new DatagramPacket(new byte[13], 13);
            sendReceiveSocket.receive(receivePacket);

            byte[] data = receivePacket.getData();

            int messageType = data[0];

            if (messageType != 3) {
                return; // not a fire event assignment
            }

            // (optional safer parsing)
            int zoneID = data[1] & 0xFF;
            int severityNum = data[2] & 0xFF;
            int taskNum = data[3] & 0xFF;
            int faultNum = data[4] & 0xFF;

            // FIX 2: shift all coordinate indices by +1
            int startX = combineBytes(data[5], data[6]);
            int startY = combineBytes(data[7], data[8]);
            int endX   = combineBytes(data[9], data[10]);
            int endY   = combineBytes(data[11], data[12]);

            zoneCenter = getZoneCenter(startX, startY, endX, endY);

            Severity severity = Severity.values()[severityNum];
            TaskType taskType = TaskType.values()[taskNum];
            FaultType faultType = FaultType.values()[faultNum];

            System.out.println("[Drone " + droneId + "] Received fire assignment for zone " + zoneID);

            currentTask = new FireEvent(zoneID, taskType, java.time.LocalTime.now(), severity, faultType);

            this.faultTriggered = false;
            executeTask();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper function for UDP packet parsing.
     * @param high upper byte to combine
     * @param low lower byte to combine
     * @return integer represented by the combined bytes
     */
    private int combineBytes(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    /**
     * Helper method to change GUI view using UDP packets
     * @param type event type either "DRONE_DISPATCHED" or "DRONE_RETURNING"
     * @param zoneId int representing zone to which drone is travelling
     */
    private void sendGuiUpdate(String type, int zoneId) {
        try {
            String msg = type + "," + zoneId;
            byte[] data = msg.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 60000);

            sendReceiveSocket.send(packet);
            System.out.println("Drone sending packet to GUI");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *Sends a fault notification to the scheduler over UDP
     * the message has the format:
     * FAULT,droneId,faultType,posX,posY
     * @param fault, the fault encountered by the drone
     */
    private void sendFaultStatus(String fault){
        String msg =  droneId + "," + status + "," + posX + "," + posY + "," + waterTank;
        System.out.println("Sending fault: " + msg);

        byte[] data = msg.getBytes();

        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), 50000);
            sendReceiveSocket.send(packet);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Thread newDrone = new Thread(new Drone(1));
        Thread droneTwo = new Thread(new Drone(2));
        newDrone.start();
        droneTwo.start();
    }
}
