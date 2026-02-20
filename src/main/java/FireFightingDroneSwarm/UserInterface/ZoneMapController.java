package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.*;
import java.util.ArrayList;

public class ZoneMapController {

    private final ZoneMapView view;

    public ZoneMapController(ZoneMapView view) {
        this.view = view;
    }

    public void initializeZones(ArrayList<Zone> zones) {
        SwingUtilities.invokeLater(() -> view.setupZones(zones));
    }

    public void fireDetected(int zoneId) {
        SwingUtilities.invokeLater(() -> view.fireDetected(zoneId));
    }

    public void droneDispatched(int zoneId) {
        SwingUtilities.invokeLater(() -> view.droneDispatched(zoneId));
    }

    public void droneExtinguishingFired(int zoneId) {
        SwingUtilities.invokeLater(() -> view.droneReturning(zoneId));
    }

    public void droneReturning(int zoneId) {
        SwingUtilities.invokeLater(() -> view.droneReturning(zoneId));
    }
}