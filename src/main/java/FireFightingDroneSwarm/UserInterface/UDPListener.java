package FireFightingDroneSwarm.UserInterface;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Listens for UDP status updates from the drone swarm and updates the controller.
 */
public class UDPListener extends Thread {
    private DatagramSocket socket;
    private final ZoneMapController controller;
    private volatile boolean running = true;

    /**
     * @param controller The controller to relay updates to.
     * @param port The port to listen on (default 5001).
     */
    public UDPListener(ZoneMapController controller, int port) {
        this.controller = controller;
        try {
            this.socket = new DatagramSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        System.out.println("[GUI] UDP Listener active on port " + socket.getLocalPort());

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();

                // Expected format: "DRONE_UPDATE:ID:ZONE:STATUS"
                String[] parts = msg.split(":");
                if (parts[0].equals("DRONE_UPDATE")) {
                    int id = Integer.parseInt(parts[1]);
                    int zone = Integer.parseInt(parts[2]);
                    String status = parts[3];
                    controller.updateDroneLocation(id, zone, status);
                }
            } catch (IOException | NumberFormatException e) {
                if (running) System.err.println("UDP Error: " + e.getMessage());
            }
        }
    }

    public void stopListener() {
        running = false;
        socket.close();
    }
}
