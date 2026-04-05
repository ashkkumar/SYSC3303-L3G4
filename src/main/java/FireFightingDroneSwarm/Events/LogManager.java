package FireFightingDroneSwarm.Events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LogManager provides a global access point for the system's EventLogger and
 * calculates required performance metrics after simulation completion.
 * * @author Abhiram Sureshkumar
 * @version 1.5
 */
public class LogManager {

    private static final EventLogger SHARED_LOGGER = new EventLogger(1000);
    private static final String LOG_FILE = "drone_logs.txt";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    static {
        File file = new File(LOG_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Private constructor to prevent instantiation.
     */
    public LogManager() {}

    /**
     * Records a system event.
     * * @param entity    Reporting component identifier.
     * @param eventCode Unique identifier for the event type.
     * @param data      Metadata associated with the event.
     */
    public static void Log(String entity, String eventCode, String... data) {
        SHARED_LOGGER.Log(entity, eventCode, data);
    }

    /**
     * Shuts down the logger and triggers the analysis of the resulting log file.
     */
    public static void stopAndAnalyze() {
        SHARED_LOGGER.shutdown();
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        performAnalysis();
    }

    /**
     * Updates drone active time tracking based on state transitions.
     * * @param line      The log line being processed.
     * @param droneId   The ID of the drone.
     * @param timestamp The time the event occurred.
     * @param startMap  Map tracking when a drone started its current task.
     * @param activeMap Map accumulating total active time per drone.
     */
    private static void handleDroneUtilization(String line, String droneId, LocalTime timestamp,
                                               Map<String, LocalTime> startMap, Map<String, Double> activeMap) {
        if (line.contains("TASK_START") || line.contains("EN_ROUTE") || line.contains("MOVEMENT_START")) {
            startMap.putIfAbsent(droneId, timestamp);
        } else if (line.contains("BASE_REACHED") || line.contains("IDLE")) {
            if (startMap.containsKey(droneId)) {
                double active = Duration.between(startMap.remove(droneId), timestamp).toMillis() / 1000.0;
                activeMap.put(droneId, activeMap.getOrDefault(droneId, 0.0) + active);
            }
        }
    }

    /**
     * Parses the log file to calculate response times, completion times, and drone utilization.
     */
    private static void performAnalysis() {
        Map<String, LocalTime> eventCreation = new HashMap<>();
        List<Double> responseTimes = new ArrayList<>();
        List<Double> completionTimes = new ArrayList<>();

        Map<String, LocalTime> droneWorkStart = new HashMap<>();
        Map<String, Double> droneActiveTime = new HashMap<>();

        LocalTime firstEntry = null;
        LocalTime lastEntry = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LocalTime timestamp = parseTimestamp(line);
                if (timestamp == null) continue;

                if (firstEntry == null) firstEntry = timestamp;
                lastEntry = timestamp;

                String droneId = extractDroneId(line);
                String fireId = extractValue(line, "FireID");

                if ((line.contains("FIRE_SENT") || line.contains("ASSIGN_TASK")) && fireId != null) {
                    eventCreation.putIfAbsent(fireId, timestamp);
                }

                if (line.contains("MOVEMENT_ARRIVED") && fireId != null) {
                    if (eventCreation.containsKey(fireId)) {
                        double resp = Duration.between(eventCreation.get(fireId), timestamp).toMillis() / 1000.0;
                        responseTimes.add(resp);
                    }
                }

                if (line.contains("EXTINGUISHING_END") && fireId != null) {
                    if (eventCreation.containsKey(fireId)) {
                        double comp = Duration.between(eventCreation.remove(fireId), timestamp).toMillis() / 1000.0;
                        completionTimes.add(comp);
                    }
                }

                if (droneId != null) {
                    handleDroneUtilization(line, droneId, timestamp, droneWorkStart, droneActiveTime);
                }
            }

            displayMetricReport(responseTimes, completionTimes, droneActiveTime, firstEntry, lastEntry);
        } catch (IOException e) {
            System.err.println("Metric Analysis Error: " + e.getMessage());
        }
    }

    /**
     * Extracts the timestamp from a log line.
     * * @param line The log line to parse.
     * @return LocalTime object or null if not found.
     */
    private static LocalTime parseTimestamp(String line) {
        Pattern p = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
        Matcher m = p.matcher(line);
        return m.find() ? LocalTime.parse(m.group(1), TIME_FORMAT) : null;
    }

    /**
     * Extracts a specific numeric value following a key from a log line.
     * * @param line The log line to parse.
     * @param key  The key identifier (e.g., "FireID").
     * @return The extracted ID as a String, or null if not found.
     */
    private static String extractValue(String line, String key) {
        Pattern p = Pattern.compile(key + "[:\\s]+(\\d+)");
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extracts the Drone ID from a log line.
     * * @param line The log line to parse.
     * @return The formatted drone ID or null if not found.
     */
    private static String extractDroneId(String line) {
        Pattern p = Pattern.compile("DRONE_(\\d+)");
        Matcher m = p.matcher(line);
        return m.find() ? "DRONE_" + m.group(1) : null;
    }

    /**
     * Displays results for Average/Max Response Time, Average/Max Completion Time, and Drone Utilization.
     * * @param responses   List of calculated response times.
     * @param completions List of calculated completion times.
     * @param activeTimes Map of accumulated active durations per drone.
     * @param start       Timestamp of the first log entry.
     * @param end         Timestamp of the last log entry.
     */
    private static void displayMetricReport(List<Double> responses, List<Double> completions, Map<String, Double> activeTimes, LocalTime start, LocalTime end) {
        double totalDuration = (start != null && end != null) ? Duration.between(start, end).toMillis() / 1000.0 : 0;

        System.out.println("\n========== PERFORMANCE METRICS ==========");

        System.out.printf("Average Event Response Time: %.2f sec\n", responses.stream().mapToDouble(d -> d).average().orElse(0.0));
        System.out.printf("Maximum Event Response Time: %.2f sec\n", responses.stream().mapToDouble(d -> d).max().orElse(0.0));

        System.out.printf("Average Event Completion Time: %.2f sec\n", completions.stream().mapToDouble(d -> d).average().orElse(0.0));
        System.out.printf("Maximum Event Completion Time: %.2f sec\n", completions.stream().mapToDouble(d -> d).max().orElse(0.0));

        System.out.println("Drone Utilization (Active / Total Simulation Time):");
        activeTimes.forEach((id, active) -> {
            double utilization = (totalDuration > 0) ? (active / totalDuration) * 100 : 0;
            System.out.printf(" - %s: %.2f%% (%.2f sec active)\n", id, utilization, active);
        });

        System.out.printf("\nTotal Simulation Duration: %.2f sec\n", totalDuration);
        System.out.println("====================================================\n");
    }
}