package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.*;
import java.util.ArrayList;

/**
 * This class acts as the controller portion of a typical
 * MVC architecture. The controller is shared between drones, and the incident reporter
 * in order to update the view when a fire is detected, or when a drone is dispatched.
 * Note all view method calls are done with SwingUtilities.invokeLater() in order to be
 * thread safe, as Swing is not default thread-safe.
 */
public class ZoneMapController {

    private final ZoneMapView view;

    /**
     * Constructor for the controller, instantiate with the global
     * view for the system.
     * @param view ZoneMapView object to use for this controller.
     */
    public ZoneMapController(ZoneMapView view) {
        this.view = view;
    }

    /**
     * This method invokes the views setup method
     * in order to paint the zones and borders on the view.
     * @param zones an ArrayList of Zone objects to paint on the panel.
     */
    public void initializeZones(ArrayList<Zone> zones) {
        SwingUtilities.invokeLater(() -> view.setupZones(zones));
    }

    /**
     * This method updates the view to paint a fire in the particular
     * zone.
     * @param zoneId the ID of the zone in which a fire is detected.
     */
    public void fireDetected(int zoneId) {
        SwingUtilities.invokeLater(() -> view.fireDetected(zoneId));
    }

    /**
     * This method updates the view to paint a drone departing
     * from its base station to the specified zone.
     * @param zoneId the ID of the zone to which the drone is dispatched.
     */
    public void droneDispatched(int zoneId) {
        SwingUtilities.invokeLater(() -> view.droneDispatched(zoneId));
    }

    /**
     * This method updates the view to paint a drone returning
     * from its fire back to its base station.
     * @param zoneId the ID of the zone from which the drone is returning.
     */
    public void droneReturning(int zoneId) {
        SwingUtilities.invokeLater(() -> view.droneReturning(zoneId));
    }
}