package FireFightingDroneSwarm.FireIncidentSubsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * This class is responsible for all input file parsing i.e the
 * event input file, and the zone input file
 */
public class InputReader {
    String eventFilePath;
    String zoneFilePath;

    /**
     * Constructor for an InputReader object to process necessary files for the system
     * @param eventFilePath absolute file path for event file
     * @param zoneFilePath absolute file path for the zone file
     */
    public InputReader(String eventFilePath, String zoneFilePath) {
        this.zoneFilePath = zoneFilePath;
        this.eventFilePath = eventFilePath;
    }

    /**
     * This function handles all parsing of the event file, taking in the events and
     * creating FireEvent objects
     * @return an ArrayList of FireEvents read in from the file in the order presented
     */
    public ArrayList<FireEvent> parseEventFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(eventFilePath));
            String line;
            ArrayList<FireEvent> events = new ArrayList<>();
            while((line = br.readLine()) != null){
                boolean isEvent = line.matches(".*[0-9].*");

                if(isEvent){
                    String[] tokens = line.split(",");

                    LocalTime timestamp = LocalTime.parse(tokens[0]);
                    int zoneID = Integer.parseInt(tokens[1]);
                    TaskType taskType = null;
                    Severity severity = null;

                    switch(tokens[2]){
                        case "DRONE_REQUEST":
                            taskType = TaskType.DRONE_REQUESTED;
                            break;
                        case "FIRE_DETECTED":
                            taskType = TaskType.FIRE_DETECTED;
                            break;
                    }

                    switch(tokens[3]) {
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

                    FireEvent event = new FireEvent(zoneID, taskType, timestamp, severity);
                    events.add(event);
                    //scheduler.notify(event);
                }
            }
            br.close();
            return events;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This function handles all parsing of the zone file, taking in the zone information and
     * creating Zone objects
     * @return an ArrayList of Zones read in from the file in the order presented
     */
    public ArrayList<Zone> parseZoneFile(){
        try{

            BufferedReader br = new BufferedReader(new FileReader(zoneFilePath));
            String line;
            ArrayList<Zone> zones = new ArrayList<>();

            while((line = br.readLine()) != null){
                boolean isZone = line.matches(".*[0-9].*");
                if(isZone){
                    String[] tokens = line.split(",");

                    int zoneID = Integer.parseInt(tokens[0]);
                    int[] startCoordinates = parseCoordinates(tokens[1]);
                    int[] endCoordinates = parseCoordinates(tokens[2]);

                    Zone zone = new Zone(zoneID, startCoordinates, endCoordinates);
                    zones.add(zone);
                }
            }
            br.close();
            return zones;
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public int[] parseCoordinates(String coordinates){
        String[] tokens = coordinates.split(";");
        String stringRepX = tokens[0].replaceAll("\\(", "");
        String stringRepY = tokens[1].replaceAll("\\)", "");

        int x = Integer.parseInt(stringRepX);
        int y = Integer.parseInt(stringRepY);

        return new int[]{x, y};
    }

}
