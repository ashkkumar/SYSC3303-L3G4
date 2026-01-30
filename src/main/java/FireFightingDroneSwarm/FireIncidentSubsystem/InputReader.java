package FireFightingDroneSwarm.FireIncidentSubsystem;
import FireFightingDroneSwarm.Scheduler.Scheduler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;

public class InputReader {
    private String filePath;
    private Scheduler scheduler;

    public InputReader(String filePath, Scheduler scheduler) {
        this.filePath = filePath;
        this.scheduler = scheduler;
        this.parseFile();
    }

    private void parseFile(){
        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            String line;
            while((line = br.readLine()) != null){
                boolean isEvent = line.matches(".*[0-9].*");

                if(isEvent){
                    System.out.println(line);
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
                    scheduler.notify(event);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
