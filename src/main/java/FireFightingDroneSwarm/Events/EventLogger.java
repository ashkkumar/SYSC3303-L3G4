package FireFightingDroneSwarm.Events;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EventLogger {
    private class LogEvent {
        final String entity;
        final String code;
        final String[] data;

        LogEvent(String entity, String code, String... data) {
            this.entity = entity;
            this.code = code;
            this.data = data;
        }

        String format() {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            StringBuilder sb = new StringBuilder();
            // Simplified format to make regex parsing reliable
            sb.append("[").append(timestamp).append("] [").append(entity).append("] [").append(code).append("]");
            if (data != null && data.length > 0) {
                for (String d : data) {
                    sb.append(" [").append(d).append("]");
                }
            }
            return sb.toString();
        }
    }

    private final ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final ScheduledExecutorService scheduler;
    private final String fileName = "drone_logs.txt";

    // Updated Regex to match the [Timestamp] [Entity] [Code] format
    private static final Pattern LOG_LINE_PATTERN = Pattern.compile("\\[(.*?)\\] \\[(.*?)\\] \\[(.*?)\\]");

    public EventLogger(long periodMs) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "log-flusher-daemon");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::flush, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    public void Log(String entity, String eventCode, String... data) {
        queue.add(new LogEvent(entity, eventCode, data));
    }

    public synchronized void flush() {
        if (queue.isEmpty()) return;
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            LogEvent e;
            while ((e = queue.poll()) != null) {
                writer.println(e.format());
            }
        } catch (IOException exception) {
            System.err.println("Logging error: " + exception.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        flush();
    }

    public void performSystemAnalysis() {
        flush(); // Ensure everything is written before analysis
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName));
            Map<String, Long> fireStartTimes = new HashMap<>();
            Map<String, Long> droneMoveStart = new HashMap<>();

            long totalFlightTime = 0;
            long totalResponseTime = 0;
            int extinguishedCount = 0;
            long firstTimestamp = -1;
            long lastTimestamp = -1;

            for (String line : lines) {
                Matcher m = LOG_LINE_PATTERN.matcher(line);
                if (!m.find()) continue;

                long time = LocalDateTime.parse(m.group(1), FORMATTER)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                String entity = m.group(2).trim();
                String code = m.group(3).trim();

                if (firstTimestamp == -1) firstTimestamp = time;
                lastTimestamp = time;

                // --- Calculation Logic ---

                // 1. Fire Response (Uses FireID)
                if (code.equals("FIRE_SENT")) {
                    String id = parseDataField(line, "FireID");
                    if (id != null) fireStartTimes.put(id, time);
                }
                else if (code.equals("FIRE_CONFIRMED") || code.equals("FIRE_EXTINGUISHED")) {
                    String id = parseDataField(line, "FireID");
                    Long start = fireStartTimes.remove(id);
                    if (start != null) {
                        totalResponseTime += (time - start);
                        extinguishedCount++;
                    }
                }

                // 2. Flight Time (Per Drone)
                if (code.equals("MOVEMENT_START")) {
                    droneMoveStart.put(entity, time);
                }
                else if (code.equals("MOVEMENT_ARRIVED") || code.equals("RETURN_BASE_ARRIVED")) {
                    Long start = droneMoveStart.remove(entity);
                    if (start != null) {
                        totalFlightTime += (time - start);
                    }
                }
            }
            displayReport(firstTimestamp, lastTimestamp, totalResponseTime, extinguishedCount, totalFlightTime);
        } catch (IOException e) {
            System.err.println("Analysis failed: " + e.getMessage());
        }
    }

    private String parseDataField(String line, String key) {
        // Looks for [Key: Value] inside the log line
        Pattern p = Pattern.compile("\\[" + key + ":\\s*(.*?)\\]");
        Matcher m = p.matcher(line);
        return m.find() ? m.group(1).trim() : null;
    }

    private void displayReport(long start, long end, long resp, int count, long flight) {
        double totalSec = (end - start) / 1000.0;
        System.out.println("\n========== SIMULATION METRICS ==========");
        System.out.println("Total Simulation Time: " + String.format("%.2f", totalSec) + "s");
        System.out.println("Fires Extinguished: " + count);
        if (count > 0) {
            System.out.println("Avg Response Time: " + String.format("%.2f", (resp / (double)count) / 1000.0) + "s");
        }
        System.out.println("Cumulative Drone Flight Time: " + (flight / 1000.0) + "s");
        System.out.println("========================================\n");
    }
}