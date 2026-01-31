package FireFightingDroneSwarm.FireIncidentSubsystem;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * This class is responsible for all input file parsing i.e the
 * event input file, and the zone input file
 */
public class InputReader {

    /** Classpath resource name for event file */
    private final String eventResource;

    /** Classpath resource name for zone file */
    private final String zoneResource;

    /**
     * Constructor for an InputReader object to process necessary files for the system
     *
     * @param eventResource classpath resource name for event file
     *                      (e.g. "events.txt" or "input/events.txt")
     * @param zoneResource  classpath resource name for the zone file
     *                      (e.g. "zones.txt" or "input/zones.txt")
     */
    public InputReader(String eventResource, String zoneResource) {
        this.zoneResource = zoneResource;
        this.eventResource = eventResource;
    }

    /**
     * This function handles all parsing of the event file, taking in the events and
     * creating FireEvent objects
     *
     * @return an ArrayList of FireEvents read in from the file in the order presented
     */
    public ArrayList<FireEvent> parseEventFile() {
        ArrayList<FireEvent> events = new ArrayList<>();

        try (BufferedReader br = createReader(eventResource)) {
            String line;

            while ((line = br.readLine()) != null) {
                boolean isEvent = line.matches(".*[0-9].*");

                if (isEvent) {
                    String[] tokens = line.split(",");

                    LocalTime timestamp = LocalTime.parse(tokens[0]);
                    int zoneID = Integer.parseInt(tokens[1]);
                    TaskType taskType = null;
                    Severity severity = null;

                    switch (tokens[2]) {
                        case "DRONE_REQUEST":
                            taskType = TaskType.DRONE_REQUESTED;
                            break;
                        case "FIRE_DETECTED":
                            taskType = TaskType.FIRE_DETECTED;
                            break;
                    }

                    switch (tokens[3]) {
                        case "Low":
                            severity = Severity.LOW;
                            break;
                        case "Moderate":
                            severity = Severity.MODERATE;
                            break;
                        case "High":
                            severity = Severity.HIGH;
                            break;
                    }

                    FireEvent event =
                            new FireEvent(zoneID, taskType, timestamp, severity);
                    events.add(event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * This function handles all parsing of the zone file, taking in the zone information and
     * creating Zone objects
     *
     * @return an ArrayList of Zones read in from the file in the order presented
     */
    public ArrayList<Zone> parseZoneFile() {
        ArrayList<Zone> zones = new ArrayList<>();

        try (BufferedReader br = createReader(zoneResource)) {
            String line;

            while ((line = br.readLine()) != null) {
                boolean isZone = line.matches(".*[0-9].*");

                if (isZone) {
                    String[] tokens = line.split(",");

                    int zoneID = Integer.parseInt(tokens[0]);
                    int[] startCoordinates = parseCoordinates(tokens[1]);
                    int[] endCoordinates = parseCoordinates(tokens[2]);

                    Zone zone =
                            new Zone(zoneID, startCoordinates, endCoordinates);
                    zones.add(zone);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return zones;
    }

    /**
     * Helper function to create a BufferedReader for a classpath resource
     *
     * @param resourceName classpath resource name
     * @return BufferedReader for the resource
     * @throws IllegalArgumentException if the resource cannot be found
     */
    private BufferedReader createReader(String resourceName) {
        InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream(resourceName);

        if (is == null) {
            throw new IllegalArgumentException(
                    "Resource not found: " + resourceName);
        }

        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /**
     * Helper function to parse coordinate strings of the form "(x;y)"
     *
     * @param coordinates coordinate string representation
     * @return integer array containing x and y coordinates
     */
    public int[] parseCoordinates(String coordinates) {
        String[] tokens = coordinates.split(";");
        String stringRepX = tokens[0].replace("(", "");
        String stringRepY = tokens[1].replace(")", "");

        int x = Integer.parseInt(stringRepX);
        int y = Integer.parseInt(stringRepY);

        return new int[]{x, y};
    }
}
