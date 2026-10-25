package dev.vessel.bootstrap.target;

import dev.vessel.bootstrap.mojang.VesselCustomFlagsProfile;
import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Structured console logger for the VESSEL Sovereign Server Microkernel supporting three distinct
 * operational verbosity modes:
 * <ol>
 *   <li><b>First-Run / Installer Mode ({@code [VESSEL/INSTALLER]}):</b> Emits detailed step-by-step
 *       installation progress (similar to Fabric, Forge, and NeoForge installers) when scaffolding
 *       directories, downloading the official Mojang bundler, unpacking libraries, and provisioning
 *       built-in GeyserMC &amp; spark.</li>
 *   <li><b>Subsequent Run — Normal Mode ({@code [VESSEL/INFO]}):</b> Emits concise, essential
 *       startup status lines on normal server launches so administrators know exactly what VESSEL
 *       is doing without console clutter.</li>
 *   <li><b>Deep Debug Mode ({@code [VESSEL/DEBUG]}):</b> Emits verbose internal diagnostics
 *       (bytecode transformations, ClassLoader routing, active optimization engines, and per-step
 *       millisecond timings) only when {@code --vessel-deep-debug} (or a specific trace flag) is
 *       enabled.</li>
 * </ol>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class VesselStartupConsoleLogger {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PrintStream outStream;
    private final boolean debugEnabled;
    private final List<String> recordedLines = new ArrayList<>();

    /**
     * Constructs a console logger bound to {@link System#out}.
     *
     * @param debugEnabled {@code true} if {@code --vessel-deep-debug} or trace flags are active
     */
    public VesselStartupConsoleLogger(boolean debugEnabled) {
        this(System.out, debugEnabled);
    }

    /**
     * Constructs a console logger bound to a custom {@link PrintStream}.
     *
     * @param outStream    target output stream
     * @param debugEnabled {@code true} if deep debug logging is enabled
     */
    public VesselStartupConsoleLogger(PrintStream outStream, boolean debugEnabled) {
        this.outStream = Objects.requireNonNull(outStream, "outStream must not be null");
        this.debugEnabled = debugEnabled;
    }

    /**
     * Factory helper creating a logger configured from {@link VesselCustomFlagsProfile}.
     *
     * @param flagsProfile parsed CLI flags profile
     * @return configured {@link VesselStartupConsoleLogger}
     */
    public static VesselStartupConsoleLogger fromFlagsProfile(VesselCustomFlagsProfile flagsProfile) {
        boolean debug = flagsProfile != null && (
                flagsProfile.isDeepDebugEnabled()
                || flagsProfile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_BYTECODE)
                || flagsProfile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_CLASSLOADER)
                || flagsProfile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_EVENTS)
                || flagsProfile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_TRACE_REGISTRY)
                || flagsProfile.isFlagEnabled(VesselCustomFlagsProfile.FLAG_PROFILE_BOOT)
        );
        return new VesselStartupConsoleLogger(debug);
    }

    /**
     * Logs the startup header banner on every launch.
     *
     * @param versionCode       active VESSEL build version code
     * @param runningJavaVer    detected Java feature version (e.g., 21 or 25)
     * @param firstRunInstall   {@code true} if this is the initial installation run
     * @param ultraOptimizeMode {@code true} if {@code --vessel-ultra-optimize} is active
     */
    public void logStartupBanner(
            String versionCode,
            int runningJavaVer,
            boolean firstRunInstall,
            boolean ultraOptimizeMode) {
        int classFileMajor = 44 + runningJavaVer;
        logInfo("========================================================================");
        logInfo(" VESSEL Sovereign Server Microkernel - Universal Minecraft Loader");
        logInfo(" Build Version : " + versionCode);
        logInfo(" Host Java VM  : Java " + runningJavaVer + " (Class File Version " + classFileMajor + ".0)"
                + " | VESSEL Min Required: Java 21 LTS (65.0)");
        logInfo(" Boot Mode     : "
                + (firstRunInstall ? "First-Run Installer + Single-Pass Boot" : "Standard Single-Pass Boot")
                + (ultraOptimizeMode ? " [ULTRA-OPTIMIZE]" : "")
                + (debugEnabled ? " [DEEP-DEBUG]" : ""));
        logInfo("========================================================================");
    }

    /**
     * Logs a first-run / bundler installation progress line ({@code [VESSEL/INSTALLER]}).
     *
     * @param message installer step description
     */
    public void logInstaller(String message) {
        emit("VESSEL/INSTALLER", message);
    }

    /**
     * Logs a standard concise operational line ({@code [VESSEL/INFO]}).
     *
     * @param message operational status message
     */
    public void logInfo(String message) {
        emit("VESSEL/INFO", message);
    }

    /**
     * Logs a warning line ({@code [VESSEL/WARN]}).
     *
     * @param message warning message
     */
    public void logWarn(String message) {
        emit("VESSEL/WARN", message);
    }

    /**
     * Logs an error line ({@code [VESSEL/ERROR]}).
     *
     * @param message error message
     */
    public void logError(String message) {
        emit("VESSEL/ERROR", message);
    }

    /**
     * Logs a deep diagnostic message ({@code [VESSEL/DEBUG]}) only when debug mode is enabled.
     *
     * @param message detailed diagnostic message
     */
    public void logDebug(String message) {
        if (debugEnabled) {
            emit("VESSEL/DEBUG", message);
        }
    }

    /**
     * Returns whether deep debug logging is enabled.
     *
     * @return {@code true} if debug logging is active
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Returns an unmodifiable snapshot of all lines emitted by this logger instance.
     *
     * @return list of formatted log lines
     */
    public List<String> getRecordedLines() {
        return Collections.unmodifiableList(recordedLines);
    }

    private void emit(String channel, String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String line = "[" + timestamp + "] [" + channel + "]: " + message;
        recordedLines.add(line);
        outStream.println(line);
        outStream.flush();
    }
}
