package FireFightingDroneSwarm.Events;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The EventLogger class records system events and writes them to a log file.
 * It also provides methods to analyze the log data such as response time,
 * throughput, and utilization.
 */
public class EventLogger {
    private class LogEvent {
        final long time;
        final String entity;
        final String code;
        final String[] data;

        LogEvent(long time, String entity, String code, String... data) {
            this.time = time;
            this.entity = entity;
            this.code = code;
            this.data = data;
        }

        String format() {
            String timestamp = LocalDateTime.now().format(FORMATTER);

            String log = "Event log: [" +
                    timestamp + ", " +
                    entity + ", " +
                    code + "]";

            if (data != null) {
                for (String d : data) {
                    log += ", " + d;
                }
            }

            log += "]";
            return log;
        }
    }

    private final ConcurrentLinkedQueue<LogEvent> queue = new ConcurrentLinkedQueue<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final ScheduledExecutorService scheduler;

    private static final Pattern LOG_PATTERN = Pattern.compile("\\[(.*?),(.*?),(.*?)\\]");

    private boolean flag = false;

    private final String fileName = "swarm_system.log";

    public EventLogger(long periodMs) {
        ThreadFactory factory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "log-flusher-daemon");
                t.setDaemon((true));
                return t;
            }
        };

        scheduler = Executors.newSingleThreadScheduledExecutor(factory);

        scheduler.scheduleAtFixedRate(
                this::flush,
                periodMs,
                periodMs,
                TimeUnit.MILLISECONDS
        );
    }

    public void Log(String entity, String eventCode, String... data) {
        queue.add(new LogEvent(
                System.currentTimeMillis(),
                entity,
                eventCode,
                data
        ));

    }

    public void flush() {
        LogEvent e;


        while ((e = queue.poll()) != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, flag))) {
                flag = true; //now the write will append to file everytime
                writer.write(e.format() + "\n");

            } catch (IOException exception) {
                System.err.println("An error occurred: " + exception.getMessage());
            }
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();

        flush();
    }
}
