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
 * LogManager provides a global, thread-safe access point for the system's EventLogger.
 * It coordinates the stopping of the shared logger, ensures the log file is generated,
 * and performs statistical analysis on the resulting drone_logs.txt file.
 * * @author Abhiram Sureshkumar
 * @version 1.2
 */
public class LogManager {

    private static final EventLogger SHARED_LOGGER = new EventLogger(1000);
    private static final String LOG_FILE = "drone_logs.txt";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    static {
        /**
         * Ensures that each new simulation run starts with a fresh log file.
         * This prevents old logs from interfering with the current run's metrics.
         */
        File file = new File(LOG_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    public LogManager() {}

    /**
     * Records a system event to the shared log file.
     * * @param entity    The name of the component reporting the event.
     * @param eventCode A short string identifying the type of event.
     * @param data      Optional additional strings containing metadata.
     */
    public static void Log(String entity, String eventCode, String... data) {
        SHARED_LOGGER.Log(entity, eventCode, data);
    }

    /**
     * Shuts down the EventLogger to flush all pending logs to the text file,
     * then parses the file to generate performance metrics.
     */
    public static void stopAndAnalyze() {
        System.out.println("Finalizing logs and flushing to " + LOG_FILE + "...");

        SHARED_LOGGER.shutdown();

        try {
            // Wait for file system synchronization
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        performAnalysis();
    }

    /**
     * Reads the generated log file and calculates swarm performance statistics.
     */
    private static void performAnalysis() {
        Map<String, LocalTime> flightStarts = new HashMap<>();
        Map<String, Double> totalFlightTimes = new HashMap<>();
        Map<String, LocalTime> idleStarts = new HashMap<>();
        List<Double> allIdleDurations = new ArrayList<>();
        Map<String, LocalTime> incidentDiscovery = new HashMap<>();
        List<Double> responseLatencies = new ArrayList<>();

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

                if (line.contains("FIRE_SENT") || line.contains("ASSIGNMENT_RECEIVED")) {
                    String fId = extractValue(line, "FireID");
                    if (fId != null) incidentDiscovery.putIfAbsent(fId, timestamp);
                }

                if (line.contains("EXTINGUISHING_END")) {
                    String fId = extractValue(line, "FireID");
                    if (fId != null && incidentDiscovery.containsKey(fId)) {
                        double latency = Duration.between(incidentDiscovery.remove(fId), timestamp).toMillis() / 1000.0;
                        responseLatencies.add(latency);
                    }
                }

                if (droneId != null) {
                    if (line.contains("TASK_START") || line.contains("EN_ROUTE")) {
                        flightStarts.put(droneId, timestamp);
                        if (idleStarts.containsKey(droneId)) {
                            allIdleDurations.add(Duration.between(idleStarts.remove(droneId), timestamp).toMillis() / 1000.0);
                        }
                    } else if (line.contains("MOVEMENT_ARRIVED") || line.contains("BASE_REACHED")) {
                        if (flightStarts.containsKey(droneId)) {
                            double flight = Duration.between(flightStarts.remove(droneId), timestamp).toMillis() / 1000.0;
                            totalFlightTimes.put(droneId, totalFlightTimes.getOrDefault(droneId, 0.0) + flight);
                        }
                        idleStarts.put(droneId, timestamp);
                    }
                }
            }
            printReport(allIdleDurations, totalFlightTimes, responseLatencies, firstEntry, lastEntry);
        } catch (IOException e) {
            System.err.println("Metric Analysis Error: " + e.getMessage());
        }
    }

    /**
     * Extracts the timestamp from a log line.
     * * @param line The log line to parse.
     * @return The LocalTime extracted, or null if not found.
     */
    private static LocalTime parseTimestamp(String line) {
        Pattern p = Pattern.compile("(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})");
        Matcher m = p.matcher(line);
        return m.find() ? LocalTime.parse(m.group(1), TIME_FORMAT) : null;
    }

    /**
     * Extracts a numeric value associated with a key.
     * * @param line The log line to parse.
     * @param key  The key to search for.
     * @return The numeric value as a string, or null if not found.
     */
    private static String extractValue(String line, String key) {
        Pattern p = Pattern.compile(key + "[:\\s]+(\\d+)");
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Identifies the drone associated with a log line.
     * * @param line The log line to parse.
     * @return The drone identifier string, or null if not found.
     */
    private static String extractDroneId(String line) {
        Pattern p = Pattern.compile("DRONE_(\\d+)");
        Matcher m = p.matcher(line);
        return m.find() ? "DRONE_" + m.group(1) : null;
    }

    /**
     * Formats and prints the performance metrics report.
     * * @param idles     List of all recorded drone idle durations.
     * @param flights   Map of total flight times per drone.
     * @param responses List of response latencies.
     * @param start     The timestamp of the first event.
     * @param end       The timestamp of the last event.
     */
    private static void printReport(List<Double> idles, Map<String, Double> flights, List<Double> responses, LocalTime start, LocalTime end) {
        System.out.println("\n========== SIMULATION PERFORMANCE REPORT ==========");
        System.out.printf("Average Drone Idle Time: %.2f sec\n", idles.stream().mapToDouble(d -> d).average().orElse(0.0));
        flights.forEach((id, time) -> System.out.printf(" - %s Total Flight Time: %.2f sec\n", id, time));
        if (start != null && end != null) {
            System.out.printf("Total Mission Duration: %.2f sec\n", Duration.between(start, end).toMillis() / 1000.0);
        }
        System.out.println("===================================================\n");
    }
}