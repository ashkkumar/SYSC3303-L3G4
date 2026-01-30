package FireFightingDroneSwarm.FireIncidentSubsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;

public class InputReader {
    private String filePath;

    public InputReader(String filePath) {
        this.filePath = filePath;
    }

    private void parseFile(){
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while((line = br.readLine()) != null){
                boolean isEvent = line.matches("[0-9]+");

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

                    switch(tokens[3]){
                        case "LOW":
                            severity = Severity.LOW;
                            break;
                        case "MODERATE":
                            severity = Severity.MODERATE;
                            break;
                        case "HIGH":
                            severity = Severity.HIGH;
                            break;
                    }

                    FireEvent event = new FireEvent(zoneID, taskType, timestamp, severity);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
