package dev.vessel.bootstrap.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Structured ANSI-colored logger for the VESSEL microkernel enforcing Decision 15
 * color semantics (White INFO, Yellow WARN, Red ERROR) and automatic suppression
 * of duplicate log messages.
 *
 * @version 1.0.0
 */
public final class VesselLogger {

    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    private final Set<String> seenMessages = ConcurrentHashMap.newKeySet();
    private final List<String> formattedHistory = new CopyOnWriteArrayList<>();
    private final AtomicInteger suppressedDuplicateCount = new AtomicInteger(0);

    /**
     * Creates a new VESSEL structured logger.
     */
    public VesselLogger() {
    }

    /**
     * Logs an informational message using ANSI white coloring.
     *
     * @param subsystemTag the subsystem tag (e.g., {@code "BOOT"}, {@code "REMAP"})
     * @param message      the log message
     */
    public void info(String subsystemTag, String message) {
        recordLog("INFO", ANSI_WHITE, subsystemTag, message);
    }

    /**
     * Logs a warning message using ANSI yellow coloring.
     *
     * @param subsystemTag the subsystem tag
     * @param message      the warning message
     */
    public void warn(String subsystemTag, String message) {
        recordLog("WARN", ANSI_YELLOW, subsystemTag, message);
    }

    /**
     * Logs an error message using ANSI red coloring.
     *
     * @param subsystemTag the subsystem tag
     * @param message      the error message
     */
    public void error(String subsystemTag, String message) {
        recordLog("ERROR", ANSI_RED, subsystemTag, message);
    }

    /**
     * Returns the total number of duplicate log entries suppressed by this logger instance.
     *
     * @return suppressed duplicate log count
     */
    public int getSuppressedDuplicateLogCount() {
        return suppressedDuplicateCount.get();
    }

    /**
     * Returns an unmodifiable snapshot of formatted non-duplicate log entries.
     *
     * @return list of ANSI-formatted log strings
     */
    public List<String> getFormattedLogHistory() {
        return Collections.unmodifiableList(new ArrayList<>(formattedHistory));
    }

    private void recordLog(String level, String ansiColor, String subsystemTag, String message) {
        Objects.requireNonNull(subsystemTag, "subsystemTag must not be null");
        Objects.requireNonNull(message, "message must not be null");
        String dedupKey = level + "|" + subsystemTag.trim() + "|" + message.trim();
        if (!seenMessages.add(dedupKey)) {
            suppressedDuplicateCount.incrementAndGet();
            return;
        }
        String formatted = ansiColor + "[VESSEL/" + level + "][" + subsystemTag.trim() + "] "
                + message + ANSI_RESET;
        formattedHistory.add(formatted);
    }
}
