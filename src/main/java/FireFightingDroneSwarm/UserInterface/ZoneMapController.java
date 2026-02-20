package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.SwingUtilities;

public class ZoneMapController {

    private final ZoneMapView view;

    public ZoneMapController(ZoneMapView view) {
        this.view = view;
    }

    public void fireDetected(Zone zone) {
        SwingUtilities.invokeLater(() ->
                view.fireDetected(zone)
        );
    }

    public void fireExtinguished(Zone zone) {
        SwingUtilities.invokeLater(() ->
                view.fireExtinguished(zone)
        );
    }

    public void fireTimeElapsed(Zone zone) {
        SwingUtilities.invokeLater(() ->
                view.fireTimeElapsed(zone)
        );
    }
}