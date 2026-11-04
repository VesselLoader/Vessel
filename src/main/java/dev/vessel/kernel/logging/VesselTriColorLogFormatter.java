package dev.vessel.kernel.logging;

import java.util.Objects;

/**
 * Formats sanitized console log lines into strict tri-color ANSI output:
 * <ul>
 *   <li>🔴 <b>RED ({@code \u001B[31m}):</b> Errors, exceptions, crashes, fatal messages.</li>
 *   <li>🟡 <b>YELLOW ({@code \u001B[33m}):</b> Warnings, notices, deprecations, missing shims.</li>
 *   <li>⚪ <b>WHITE ({@code \u001B[37m}):</b> Normal operational information, boot stages, debug lines.</li>
 * </ul>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselTriColorLogFormatter {

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_RESET = "\u001B[0m";

    public VesselTriColorLogFormatter() {
    }

    /**
     * Formats a clean line into strict 3-color ANSI formatting based on severity semantics.
     *
     * @param sanitizedLine sanitized text with formatting codes removed
     * @param isExplicitErrorStream {@code true} if coming from standard error stream
     * @return ANSI color-formatted string
     */
    public String formatTriColor(String sanitizedLine, boolean isExplicitErrorStream) {
        Objects.requireNonNull(sanitizedLine, "sanitizedLine must not be null");
        if (sanitizedLine.isEmpty()) {
            return "";
        }

        if (isExplicitErrorStream || isErrorLine(sanitizedLine)) {
            return ANSI_RED + sanitizedLine + ANSI_RESET;
        }

        if (isWarningLine(sanitizedLine)) {
            return ANSI_YELLOW + sanitizedLine + ANSI_RESET;
        }

        return ANSI_WHITE + sanitizedLine + ANSI_RESET;
    }

    private boolean isErrorLine(String line) {
        String lower = line.toLowerCase();
        return line.contains("[ERROR]")
                || line.contains("[FATAL]")
                || line.contains("[SEVERE]")
                || line.contains("[STDERR]")
                || lower.contains("exception")
                || lower.contains("error:")
                || lower.contains("caused by:")
                || lower.contains("failed to")
                || lower.contains("crash");
    }

    private boolean isWarningLine(String line) {
        String lower = line.toLowerCase();
        return line.contains("[WARN]")
                || line.contains("[WARNING]")
                || lower.contains("warning")
                || lower.contains("notice:")
                || lower.contains("deprecated")
                || lower.contains("missing shim");
    }
}
