package FireFightingDroneSwarm.Events;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Updated LogManager to correctly parse FireIDs and calculate response latency.
 */
public class LogManager {

    private static final EventLogger SHARED_LOGGER = new EventLogger(1000);
    private static final String LOG_FILE = "drone_logs.txt";

    public LogManager() {}

    public static void Log(String entity, String eventCode, String... data) {
        SHARED_LOGGER.Log(entity, eventCode, data);
    }

    public static void stop() {
        SHARED_LOGGER.shutdown();
    }

    public static void performAnalysis() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

        Map<String, LocalTime> lastIdleStart = new HashMap<>();
        Map<String, List<Double>> idleDurations = new HashMap<>();
        Map<String, LocalTime> flightStart = new HashMap<>();
        Map<String, Double> totalFlightTime = new HashMap<>();

        // Incident Tracking
        Map<String, LocalTime> incidentDetectedTime = new HashMap<>();
        List<Double> responseTimes = new ArrayList<>();

        LocalTime firstEventTime = null;
        LocalTime lastEventTime = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("[") || !line.contains("]")) continue;

                // Extract timestamp: works for both "Event log: [time]" and "[time]" formats
                String timePart = line.substring(line.indexOf("[") + 1, line.indexOf("]"));
                if (timePart.contains(",")) timePart = timePart.split(",")[0]; // Handle comma-separated logs
                LocalTime timestamp = LocalTime.parse(timePart.split(" ")[1], formatter);

                if (firstEventTime == null) firstEventTime = timestamp;
                lastEventTime = timestamp;

                // Improved FireID Extraction Logic
                if (line.contains("ASSIGNMENT_RECEIVED") || line.contains("FIRE_SENT")) {
                    String id = extractValue(line, "FireID:");
                    if (id != null) incidentDetectedTime.put(id, timestamp);
                }

                if (line.contains("EXTINGUISHING_END")) {
                    // Check for FireID in data or end of line
                    String id = extractValue(line, "FireID:");
                    if (id == null) {
                        // Fallback: get the last integer in the line which is often the FireID
                        String[] words = line.split(" ");
                        id = words[words.length - 1].replaceAll("[^0-9]", "");
                    }

                    if (id != null && incidentDetectedTime.containsKey(id)) {
                        double response = Duration.between(incidentDetectedTime.remove(id), timestamp).toMillis() / 1000.0;
                        responseTimes.add(response);
                    }
                }

                // Flight and Idle tracking
                String entity = line.contains("DRONE") ? line.substring(line.indexOf("DRONE"), line.indexOf("DRONE") + 7).replace("]", "") : "SYSTEM";

                if (line.contains("EN_ROUTE") || line.contains("TASK_START")) {
                    flightStart.put(entity, timestamp);
                    if (lastIdleStart.containsKey(entity)) {
                        idleDurations.computeIfAbsent(entity, k -> new ArrayList<>()).add(
                                Duration.between(lastIdleStart.remove(entity), timestamp).toMillis() / 1000.0);
                    }
                } else if (line.contains("MOVEMENT_ARRIVED") || line.contains("BASE_REACHED")) {
                    lastIdleStart.put(entity, timestamp);
                    if (flightStart.containsKey(entity)) {
                        totalFlightTime.put(entity, totalFlightTime.getOrDefault(entity, 0.0) +
                                Duration.between(flightStart.remove(entity), timestamp).toMillis() / 1000.0);
                    }
                }
            }
            printReport(idleDurations, totalFlightTime, responseTimes, firstEventTime, lastEventTime);
        } catch (IOException e) {
            System.err.println("Error reading logs: " + e.getMessage());
        }
    }

    private static String extractValue(String line, String key) {
        if (!line.contains(key)) return null;
        try {
            int start = line.indexOf(key) + key.length();
            String sub = line.substring(start).trim();
            return sub.split("[, \\]]")[0].trim();
        } catch (Exception e) { return null; }
    }

    private static void printReport(Map<String, List<Double>> idle, Map<String, Double> flight,
                                    List<Double> response, LocalTime start, LocalTime end) {
        System.out.println("\n========== SWARM PERFORMANCE REPORT ==========");
        double totalIdle = idle.values().stream().flatMap(List::stream).mapToDouble(Double::doubleValue).sum();
        long idleCount = idle.values().stream().mapToLong(List::size).sum();
        System.out.printf("Average Drone Idle Time: %.2f seconds\n", (idleCount > 0 ? totalIdle / idleCount : 0));

        System.out.println("Total Flight Time per Drone:");
        flight.forEach((id, time) -> System.out.printf(" - %s: %.2f seconds\n", id, time));

        if (start != null && end != null) {
            System.out.printf("Total Time to Distinguish All Fires: %.2f seconds\n",
                    Duration.between(start, end).toMillis() / 1000.0);
        }
        System.out.println("==============================================\n");
    }
}