package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;


public class ZoneMapView {

    private final Map<Integer, Integer> zoneLengths;
    private final Map<Integer, Integer> zoneHeights;
    private double lengthScale;
    private double heightScale;

    private static final int MAP_LENGTH = 900;
    private static final int MAP_HEIGHT = 600;
    private static final int CELL_SIZE = 10;

    private ZoneMapPanel zoneMap;

    public ZoneMapView(){
        zoneLengths = new HashMap<>();
        zoneHeights = new HashMap<>();
        buildUserInterface();
    }

    /**
     * This method handles placing the zone map and legend on the general window
     * and ensuring adequate spacing.
     */
    public void buildUserInterface(){

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Fire Fighting Drone Swarm - Group 4");
        frame.setSize(1300, 800);
        frame.setLayout(new BorderLayout());   
        
        zoneMap = new ZoneMapPanel();
        zoneMap.setPreferredSize(new Dimension(MAP_LENGTH, MAP_HEIGHT));
        zoneMap.setBackground(Color.WHITE);

        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(Color.WHITE);
        backgroundPanel.add(zoneMap);

        frame.add(backgroundPanel, BorderLayout.CENTER);
        JPanel legend = createLegendPanel();
        legend.setPreferredSize(new Dimension(300, 600));
        frame.add(legend, BorderLayout.EAST);

        frame.setVisible(true);
    }

    /**
     * This method creates the legend panel to discern the colours for different events
     * @return JPanel including all of the legend items and their colours
     */
    private JPanel createLegendPanel() {
        JPanel legend = new JPanel(new GridBagLayout());
        legend.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legend.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        addLegendItem(legend, gbc, "Z(n)", new Color(220, 235, 220), "Zone Label", Color.BLACK);
        addLegendItem(legend, gbc, "", Color.RED, "Active fire", Color.WHITE);
        addLegendItem(legend, gbc, "", new Color(80, 150, 70), "Extinguished fire", Color.WHITE);
        addLegendItem(legend, gbc, "D(n)", new Color(255, 165, 0), "Drone outbound", Color.BLACK);
        addLegendItem(legend, gbc, "D(n)", new Color(34, 139, 34), "Drone Extinguishing fire", Color.WHITE);
        addLegendItem(legend, gbc, "D(3)", new Color(180, 130, 180), "Drone Returning", Color.BLACK);

        return legend;
    }

    /**
     * Helper method to add legend items in a flexible manner to the legend panel created above.
     * @param panel the legend panel you wish to modify
     * @param gbc spacing constraints
     * @param iconText String to use for this legend item
     * @param boxColor Java Color to use for this legend item
     * @param description Description of the legend item
     * @param textColor Java Color for the text
     */
    private void addLegendItem(JPanel panel, GridBagConstraints gbc, String iconText, Color boxColor, String description, Color textColor) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(Color.WHITE);

        JPanel icon = new JPanel(new GridBagLayout());
        icon.setPreferredSize(new Dimension(40, 25));
        icon.setBackground(boxColor);
        icon.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        if (!iconText.isEmpty()) {
            JLabel text = new JLabel(iconText);
            text.setFont(new Font("SansSerif", Font.BOLD, 11));
            text.setForeground(textColor);
            icon.add(text);
        }

        JLabel label = new JLabel(description);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));

        row.add(icon);
        row.add(label);

        panel.add(row, gbc);
    }

    /**
     * Helper method called by the model and controller to set up the zone map on the GUI
     * @param zones ArrayList of Zone objects with their dimensions to scale the map
     */
    public void setupZones(ArrayList<Zone> zones){
        findScalingFactor(zones);
        for(Zone z : zones){
            calculateZone(z);
        }
    }

    /**
     * Helper function. Since the frame size is fixed, and the zone lengths will vary each zone needs
     * to be scaled to match the number of pixels specified for the Zone Map.
     * @param zones ArrayList of Zone objects to be depicted on the map.
     */
    public void findScalingFactor(ArrayList<Zone> zones){

        int totalZoneLength = 0;
        int totalZoneHeight = 0;

        zoneLengths.clear();
        zoneHeights.clear();

        // Place each of the zones lengths into a map, if the Y-coordinate where the zone begins already
        // exists then add this length to calculate the total length of the window
        for (Zone z : zones) {
            int startY = z.getStartCoordinates()[1];
            int zoneLength = z.getEndCoordinates()[0] - z.getStartCoordinates()[0];

            zoneLengths.put(
                    startY,
                    zoneLengths.getOrDefault(startY, 0) + zoneLength
            );
        }

        // Find max. zone length
        for (int length : zoneLengths.values()) {
            totalZoneLength = Math.max(totalZoneLength, length);
        }

        //Repeat of above steps for width - find the max width covered by the zones
        for (Zone z : zones) {
            int startX = z.getStartCoordinates()[0];
            int zoneHeight = z.getEndCoordinates()[1] - z.getStartCoordinates()[1];

            zoneHeights.put(
                    startX,
                    zoneHeights.getOrDefault(startX, 0) + zoneHeight
            );
        }

        for (int height : zoneHeights.values()) {
            totalZoneHeight = Math.max(totalZoneHeight, height);
        }

        // Calculate the scaling factor to decide how many pixels to multiply the specified lengths and width
        lengthScale = (double) MAP_LENGTH / totalZoneLength;
        heightScale = (double) MAP_HEIGHT / totalZoneHeight;
    }

    /**
     * Helper function to draw the zone as a rectangle on the panel. Uses above scaling factors
     * to determine length and width of the rectangle.
     * @param zone Zone object to be drawn on the GUI.
     */
    public void calculateZone(Zone zone){

        int zoneLength = zone.getEndCoordinates()[0] - zone.getStartCoordinates()[0];
        int zoneHeight = zone.getEndCoordinates()[1] - zone.getStartCoordinates()[1];

        int pixelWidth = (int) (zoneLength * lengthScale);
        int pixelHeight = (int) (zoneHeight * heightScale);

        int startX = (int) (zone.getStartCoordinates()[0] * lengthScale);
        int startY = (int) (zone.getStartCoordinates()[1] * heightScale);

        Rectangle rect = new Rectangle(startX, startY, pixelWidth, pixelHeight);
        zoneMap.addZone(rect, zone);
    }

    public void fireDetected(int zoneId) {
        if (zoneMap != null) {
            zoneMap.fireDetected(zoneId);
        }
    }

    public void droneDispatched(int zoneId) {
        if (zoneMap != null) {
            zoneMap.droneDispatched(zoneId);
        }
    }

    public void droneReturning(int zoneId) {
        if (zoneMap != null) {
            zoneMap.droneReturning(zoneId);
        }
    }

    static class ZoneMapPanel extends JPanel {

        private final List<Rectangle> rectangles = new ArrayList<>();
        private final List<Zone> zones = new ArrayList<>();

        private final List<Integer> activeFires = new ArrayList<>();
        private final List<Integer> extinguishedFires = new ArrayList<>();

        private final List<DroneAnimation> drones = new ArrayList<>();

        private Timer animationTimer;

        /***
         * Starts a timer to repaint the GUI every 30 ms, does some processing of the active drones
         * and the extinguished fires to repaint the screen.
         */
        public ZoneMapPanel() {

            animationTimer = new Timer(30, e -> {

                for (DroneAnimation drone : drones) {

                    switch (drone.state) {

                        case GOING:
                            drone.progress += 0.005;
                            if (drone.progress >= 1.0) {
                                drone.progress = 1.0;
                                drone.state = DroneAnimation.State.EXTINGUISHING;
                            }
                            break;

                        case RETURNING:
                            drone.progress -= 0.005;
                            break;

                        case EXTINGUISHING:
                            break;
                    }
                }

                Iterator<DroneAnimation> iterator = drones.iterator();
                while (iterator.hasNext()) {
                    DroneAnimation drone = iterator.next();

                    if (drone.state == DroneAnimation.State.RETURNING
                            && drone.progress <= 0.0) {
                        iterator.remove();
                        extinguishedFires.remove((Integer) drone.zoneId);
                    }
                }
                repaint();
            });

            animationTimer.start();
        }

        /**
         * Helper class to create an animation from a Drone, with
         * state and progress to track how far it is from its return
         * or destination.
         */
        private static class DroneAnimation {

            enum State {
                GOING,
                EXTINGUISHING,
                RETURNING
            }

            int zoneId;
            double progress = 0.0;
            State state = State.GOING;
        }

        public void addZone(Rectangle rect, Zone zone) {
            rectangles.add(rect);
            zones.add(zone);
        }

        public void fireDetected(int zoneId) {
            activeFires.add(zoneId);
        }

        public void fireExtinguished(int zoneId) {
            activeFires.remove((Integer) zoneId);
            extinguishedFires.add(zoneId);
        }

        public void droneDispatched(int zoneId) {
            DroneAnimation drone = new DroneAnimation();
            drone.zoneId = zoneId;
            drone.progress = 0.0;
            drone.state = DroneAnimation.State.GOING;

            drones.add(drone);
        }

        public void droneReturning(int zoneId) {
            for (DroneAnimation drone : drones) {
                if (drone.zoneId == zoneId && drone.state == DroneAnimation.State.EXTINGUISHING) {
                    fireExtinguished(zoneId);
                    drone.state = DroneAnimation.State.RETURNING;
                }
            }
        }

        /**
         * Override method of the default paint method in order to
         * add grid lines and zone markers - makes it easier to animate the panel flexibly, no
         * real workaround for this.
         * @param g Graphics object to use for painting this JPanel.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setColor(new Color(230, 230, 230));

            for (int x = 0; x <= MAP_LENGTH; x += CELL_SIZE) {
                g2.drawLine(x, 0, x, MAP_HEIGHT);
            }

            for (int y = 0; y <= MAP_HEIGHT; y += CELL_SIZE){
                g2.drawLine(0, y, MAP_LENGTH, y);
            }


            g2.setStroke(new BasicStroke(3));
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));

            for (int i = 0; i < rectangles.size(); i++) {

                Rectangle rect = rectangles.get(i);
                Zone zone = zones.get(i);

                // Paint each zone and its borders
                g2.setColor(Color.BLUE);
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);

                g2.setColor(Color.BLACK);
                g2.drawString("Zone " + zone.getID(), rect.x + 5, rect.y + 20);

                // Paint the fire in this zone
                if (activeFires.contains(zone.getID())) {
                    g2.setColor(Color.RED);
                    g2.fillRect(
                            (int) rect.getCenterX(),
                            (int) rect.getCenterY(),
                            2 * CELL_SIZE,
                            2 * CELL_SIZE
                    );
                }

                // Change fire colour to green if moved to extinguished fires
                if (extinguishedFires.contains(zone.getID())) {
                    g2.setColor(new Color(80, 150, 70));
                    g2.fillRect(
                            (int) rect.getCenterX(),
                            (int) rect.getCenterY(),
                            2 * CELL_SIZE,
                            2 * CELL_SIZE
                    );
                }
            }

            // Draw all of the active drones and their current positions
            for (DroneAnimation drone : drones) {
                drawDrone(g2, drone);
            }
        }

        private void drawDrone(Graphics2D g2, DroneAnimation drone) {

            for (int i = 0; i < zones.size(); i++) {
                if (zones.get(i).getID() == drone.zoneId) {

                    Rectangle rect = rectangles.get(i);

                    int cornerX = 0;
                    int cornerY = 0;

                    int centerX = (int) rect.getCenterX() + 5;
                    int centerY = (int) rect.getCenterY() + 5;

                    double p = Math.max(0, Math.min(drone.progress, 1));

                    int currentX = (int) (cornerX + p * (centerX - cornerX));
                    int currentY = (int) (cornerY + p * (centerY - cornerY));

                    switch (drone.state) {
                        case GOING:
                            g2.setColor(new Color(255, 165, 0));
                            break;
                        case EXTINGUISHING:
                            g2.setColor(new Color(34, 139, 34));
                            break;
                        case RETURNING:
                            g2.setColor(new Color(180, 130, 180));
                            break;
                    }

                    g2.fillRect(currentX - 5, currentY - 5, 20, 20);

                    String label = "D(" + drone.zoneId + ")"; //Needs to change to drone ID - need some link
                    Font font = new Font("SansSerif", Font.BOLD, 8);
                    g2.setFont(font);

                    FontMetrics metrics = g2.getFontMetrics(font);
                    int textWidth = metrics.stringWidth(label);
                    int textHeight = metrics.getAscent();

                    int textX = (currentX - textWidth / 2) + 4;
                    int textY = (currentY + textHeight / 2) + 4;

                    g2.setColor(Color.BLACK);
                    g2.drawString(label, textX, textY);
                    break;
                }
            }
        }
    }

}
