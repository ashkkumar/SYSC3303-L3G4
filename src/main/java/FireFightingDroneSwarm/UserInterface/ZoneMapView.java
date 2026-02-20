package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

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
    }

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

    private JPanel createLegendPanel() {
        JPanel legend = new JPanel(new GridBagLayout());
        legend.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        legend.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5); // EXACTLY as you had

        addLegendItem(legend, gbc, "Z(n)", new Color(220, 235, 220), "Zone Label", Color.BLACK);
        addLegendItem(legend, gbc, "", Color.RED, "Active fire", Color.WHITE);
        addLegendItem(legend, gbc, "", new Color(80, 150, 70), "Extinguished fire", Color.WHITE);
        addLegendItem(legend, gbc, "D(n)", new Color(255, 165, 0), "Drone outbound", Color.BLACK);
        addLegendItem(legend, gbc, "D(n)", new Color(34, 139, 34), "Drone Extinguishing fire", Color.WHITE);
        addLegendItem(legend, gbc, "D(3)", new Color(180, 130, 180), "Drone Returning", Color.BLACK);

        return legend;
    }

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

    public void findScalingFactor(ArrayList<Zone> zones){

        int totalZoneLength = 0;
        int totalZoneHeight = 0;

        zoneLengths.clear();
        zoneHeights.clear();

        for (Zone z : zones) {
            int startY = z.getStartCoordinates()[1];
            int zoneLength = z.getEndCoordinates()[0] - z.getStartCoordinates()[0];

            zoneLengths.put(
                    startY,
                    zoneLengths.getOrDefault(startY, 0) + zoneLength
            );
        }

        for (int length : zoneLengths.values()) {
            totalZoneLength = Math.max(totalZoneLength, length);
        }

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

        lengthScale = (double) MAP_LENGTH / totalZoneLength;
        heightScale = (double) MAP_HEIGHT / totalZoneHeight;
    }

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

    public void fireDetected(Zone zone) {
        if (zoneMap != null) {
            zoneMap.fireDetected(zone);
        }
    }

    public void fireExtinguished(Zone zone) {
        if (zoneMap != null) {
            zoneMap.fireExtinguished(zone);
        }
    }

    public void fireTimeElapsed(Zone zone) {
        if (zoneMap != null) {
            zoneMap.fireTimeElapsed(zone);
        }
    }

    static class ZoneMapPanel extends JPanel {

        private final java.util.List<Rectangle> rectangles = new ArrayList<>();
        private final java.util.List<Zone> zones = new ArrayList<>();
        private final java.util.List<Integer> activeFires = new ArrayList<>();
        private final java.util.List<Integer> extinguishedFires = new ArrayList<>();

        public void addZone(Rectangle rect, Zone zone) {
            rectangles.add(rect);
            zones.add(zone);
            repaint();
        }

        public void fireDetected(Zone zone){
            activeFires.add(zone.getID());
            repaint();
        }

        public void fireExtinguished(Zone zone){
            activeFires.remove((Integer) zone.getID());
            extinguishedFires.add(zone.getID());
            repaint();
        }

        public void fireTimeElapsed(Zone zone){
            extinguishedFires.remove((Integer) zone.getID());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setColor(new Color(230, 230, 230));
            for (int x = 0; x <= MAP_LENGTH; x += CELL_SIZE) g2.drawLine(x, 0, x, MAP_HEIGHT);
            for (int y = 0; y <= MAP_HEIGHT; y += CELL_SIZE) g2.drawLine(0, y, MAP_LENGTH, y);

            g2.setStroke(new BasicStroke(3));
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));

            for (int i = 0; i < rectangles.size(); i++) {
                Rectangle rect = rectangles.get(i);
                Zone zone = zones.get(i); // relying on them being in order here - is this safe? Yes, because they get added in a function call internally.

                g2.setColor(Color.BLUE);
                g2.drawRect(rect.x, rect.y, rect.width, rect.height);

                g2.setColor(Color.BLACK);
                g2.drawString("Zone " + zone.getID(), rect.x + 5, rect.y + 20);

                if(activeFires.contains(zone.getID())){
                    g2.setColor(Color.RED);
                    g2.fillRect((int) rect.getCenterX(), (int) rect.getCenterY(), 2 * CELL_SIZE, 2 * CELL_SIZE);
                }

                if(extinguishedFires.contains(zone.getID())){
                    g2.setColor(new Color(80, 150, 70));
                    g2.fillRect((int) rect.getCenterX(), (int) rect.getCenterY(), 2 * CELL_SIZE, 2 * CELL_SIZE);
                }
            }

        }

    }

    public static void main(String[] args){

        ZoneMapView zoneDisplay = new ZoneMapView();
        zoneDisplay.buildUserInterface();

        int[] zoneOneStart = {0,0};
        int[] zoneOneEnd = {600, 1300};
        int[] zoneTwoStart = {600,0};
        int[] zoneTwoEnd = {1300, 1300};
        int[] zoneThreeStart = {0,1300};
        int[] zoneThreeEnd = {1300, 1600};
        int[] zoneFourStart = {1300,1300};
        int[] zoneFourEnd = {1600, 1600};

        Zone zoneOne = new Zone(1, zoneOneStart, zoneOneEnd);
        Zone zoneTwo = new Zone(2, zoneTwoStart, zoneTwoEnd);
        Zone zoneThree = new Zone(3, zoneThreeStart, zoneThreeEnd);
        Zone zoneFour = new Zone(4, zoneFourStart, zoneFourEnd);

        ArrayList<Zone> zones = new ArrayList<>();
        zones.add(zoneOne);
        zones.add(zoneTwo);
        zones.add(zoneThree);
        zones.add(zoneFour);

        zoneDisplay.findScalingFactor(zones);

        zoneDisplay.calculateZone(zoneOne);
        zoneDisplay.calculateZone(zoneTwo);
        zoneDisplay.calculateZone(zoneThree);
        zoneDisplay.calculateZone(zoneFour);

        //zoneDisplay.fireDetected(zoneOne);

        //zoneDisplay.fireExtinguished(zoneOne);

        //zoneDisplay.fireTimeElapsed(zoneOne);
    }
}
