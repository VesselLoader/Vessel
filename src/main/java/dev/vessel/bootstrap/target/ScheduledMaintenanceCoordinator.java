package dev.vessel.bootstrap.target;

import java.util.Set;

/**
 * Coordinates scheduled maintenance windows and formats countdown broadcast alerts
 * prior to automated server restarts.
 *
 * @version 1.0.0
 */
public final class ScheduledMaintenanceCoordinator {

    private static final Set<Integer> WARNING_COUNTDOWN_THRESHOLDS = Set.of(
            900, 600, 300, 180, 120, 60, 30, 15, 10, 5, 4, 3, 2, 1
    );

    /**
     * Creates a new scheduled maintenance coordinator.
     */
    public ScheduledMaintenanceCoordinator() {
    }

    /**
     * Determines whether a restart countdown warning broadcast should be triggered
     * for the given clock time and remaining seconds.
     *
     * @param currentHour         the current hour of day (0-23)
     * @param currentMinute       the current minute of hour (0-59)
     * @param secondsUntilRestart the number of seconds remaining until scheduled restart
     * @return {@code true} if the clock time is valid and {@code secondsUntilRestart} matches a broadcast threshold
     */
    public boolean shouldTriggerWarningCountdown(
            int currentHour, int currentMinute, int secondsUntilRestart) {
        if (currentHour < 0 || currentHour > 23 || currentMinute < 0 || currentMinute > 59) {
            return false;
        }
        if (secondsUntilRestart <= 0) {
            return false;
        }
        return WARNING_COUNTDOWN_THRESHOLDS.contains(secondsUntilRestart);
    }

    /**
     * Formats a standardized server-wide broadcast message for the remaining restart countdown.
     *
     * @param secondsRemaining seconds remaining before the scheduled maintenance restart
     * @return formatted broadcast string
     */
    public String formatRestartCountdownBroadcast(int secondsRemaining) {
        int clamped = Math.max(0, secondsRemaining);
        if (clamped >= 60 && clamped % 60 == 0) {
            int minutes = clamped / 60;
            return "[VESSEL/MAINTENANCE] Scheduled server restart in " + minutes
                    + (minutes == 1 ? " minute (" : " minutes (") + clamped + "s).";
        }
        return "[VESSEL/MAINTENANCE] Scheduled server restart in " + clamped
                + (clamped == 1 ? " second." : " seconds.");
    }
}
