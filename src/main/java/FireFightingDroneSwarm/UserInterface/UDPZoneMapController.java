package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * A UDP wrapper for ZoneMapController.
 * Listens for messages from drones or the incident subsystem
 * and updates the view accordingly.
 */
public class UDPZoneMapController implements Runnable {

    private final ZoneMapController controller;
    private final int listenPort;
    private volatile boolean running = true;
    private DatagramSocket socket;
    private ArrayList<Zone> zones = new ArrayList<>();

    public UDPZoneMapController(ZoneMapController controller, int listenPort) {
        this.controller = controller;
        this.listenPort = listenPort;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(listenPort);
            socket.setReuseAddress(true);
            byte[] buffer = new byte[1024];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength()).trim();
                handlePacket(msg);
            }

        } catch (Exception e) {
            if (running) e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * This method parses messages from the received Datagram and passes it
     * onto the controller
     * @param message the String received from the UDP datagram
     */
    private void handlePacket(String message) {
        String[] parts = message.split(",");
        System.out.println(parts[0]);
        switch (parts[0]) {
            case "FIRE_EVENT" -> {
                int zoneId = Integer.parseInt(parts[1]);
                SwingUtilities.invokeLater(() -> controller.fireDetected(zoneId));
            }
            case "DRONE_DISPATCHED" -> {
                int zoneId = Integer.parseInt(parts[1]);
                System.out.println(zoneId);
                System.out.println("Sending controller drone dispatched");
                SwingUtilities.invokeLater(() -> controller.droneDispatched(zoneId));
            }
            case "DRONE_RETURNING" -> {
                int zoneId = Integer.parseInt(parts[1]);
                SwingUtilities.invokeLater(() -> controller.droneReturning(zoneId));
            }
            case "ZONE_INIT" -> {

                int id = Integer.parseInt(parts[1]);
                int startX = Integer.parseInt(parts[2]);
                int startY = Integer.parseInt(parts[3]);
                int endX = Integer.parseInt(parts[4]);
                int endY = Integer.parseInt(parts[5]);

                Zone zone = new Zone(id,
                        new int[]{startX,startY},
                        new int[]{endX,endY});

                zones.add(zone);

                SwingUtilities.invokeLater(() -> controller.initializeZones(zones));
            }
            default -> System.out.println("Unknown packet type: " + parts[0]);
        }
    }

    /**
     * Close sockets at end of execution .
     */
    public void shutdown() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        ZoneMapView view = new ZoneMapView();
        ZoneMapController zoneMapController = new ZoneMapController(view);
        UDPZoneMapController udpZoneMapController = new UDPZoneMapController(zoneMapController, 60000);
        Thread thread = new Thread(udpZoneMapController);
        thread.start();
    }
}