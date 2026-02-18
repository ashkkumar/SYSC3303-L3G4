package FireFightingDroneSwarm.UserInterface;

import FireFightingDroneSwarm.FireIncidentSubsystem.Zone;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class ZoneDisplay {

    private int totalZoneLength;
    private int totalZoneHeight;
    private Map<Integer, Integer> zoneLengths;
    private Map<Integer, Integer> zoneHeights;
    private int lengthScale;
    private int heightScale;
    private static final int MAP_LENGTH = 900;
    private static final int MAP_HEIGHT = 600;

    public ZoneDisplay(){
        totalZoneLength = 0;
        totalZoneHeight = 0;

        lengthScale = 0;
        heightScale = 0;

        zoneLengths = new HashMap<>();
        zoneHeights = new HashMap<>();
    }

    public void buildUserInterface(){

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Fire Fighting Drone Swarm - Group 4");
        frame.setLayout(null);
        frame.setSize(1200, 800);
        frame.getContentPane().setBackground(Color.WHITE);

        JPanel zoneMap = new JPanel();
        zoneMap.setBounds(100, 100, MAP_LENGTH, MAP_HEIGHT);
        zoneMap.setBackground(Color.WHITE);

        buildZone(zoneMap, 60, 90);
        frame.add(zoneMap);

        JPanel legend = new JPanel();
        legend.setBounds(750, 100, 250, 700);
        legend.setBackground(new Color(38, 162, 224));
        //frame.add(legend);

        frame.setVisible(true);
    }

    public static void main(String[] args){
        ZoneDisplay zoneDisplay = new ZoneDisplay();
        zoneDisplay.buildUserInterface();
    }

    public void buildZone(JPanel zone, int rows, int columns) {
        zone.setLayout(new GridLayout(rows, columns));

        Border cellBorder = BorderFactory.createLineBorder(new Color(230, 230, 230));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {

                JPanel cell = new JPanel(new BorderLayout());
                cell.setBackground(Color.WHITE);
                cell.setBorder(cellBorder);
                zone.add(cell);
            }
        }
    }

    public void findScalingFactor(ArrayList<Zone> zones){

        for(Zone z: zones){
            int length = zoneLengths.get(z.getEndCoordinates()[0] - z.getStartCoordinates()[0]);
            if(zoneLengths.containsKey(z.getStartCoordinates()[1])) {
                zoneLengths.put(zoneLengths.get(z.getStartCoordinates()[1]), length);
            } else {
                zoneLengths.put(zoneLengths.get(z.getStartCoordinates()[0]), length);
            }
            totalZoneLength = Math.max(totalZoneLength, length);
        }


        for(Zone z: zones){
            int height = zoneLengths.get(z.getEndCoordinates()[1] - z.getStartCoordinates()[1]);
            if(zoneHeights.containsKey(z.getStartCoordinates()[0])) {
                zoneHeights.put(zoneHeights.get(z.getStartCoordinates()[0]), height);
            } else {
                zoneHeights.put(zoneHeights.get(z.getStartCoordinates()[0]), height);
            }
            totalZoneHeight = Math.max(totalZoneHeight, height);
        }

        lengthScale = MAP_LENGTH / totalZoneLength;
        heightScale = MAP_HEIGHT / totalZoneHeight;
    }

    public void calculateZone(Zone zone){
        int zoneLength = zone.getEndCoordinates()[0] - zone.getStartCoordinates()[0];
        int zoneHeight = zone.getEndCoordinates()[1] - zone.getStartCoordinates()[1];

        int numLengthPixels = zoneLength * lengthScale;
        int numHeightPixels = zoneHeight * heightScale;
    }

    public void outlineZone(int length, int height, int startX, int startY){

    }
}



