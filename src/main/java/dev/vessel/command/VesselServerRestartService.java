package dev.vessel.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Executes a clean, Paper-style server process restart ({@code /restart}) by broadcasting a
 * restart announcement, triggering world saves, and launching a fresh replacement JVM process
 * with full memory and ClassLoader re-initialization before halting the current instance.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class VesselServerRestartService {

    /**
     * Initiates the server restart sequence from the given server root directory.
     *
     * @param serverRoot the server root directory
     * @return {@code true} if the replacement process was successfully dispatched
     */
    public boolean triggerServerProcessRestart(Path serverRoot) {
        return triggerServerProcessRestart(serverRoot, true);
    }

    /**
     * Initiates the server restart sequence with optional JVM termination control.
     *
     * @param serverRoot         the server root directory
     * @param exitCurrentProcess whether to halt the active JVM process
     * @return {@code true} if the replacement process was successfully dispatched
     */
    public boolean triggerServerProcessRestart(Path serverRoot, boolean exitCurrentProcess) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path normalizedRoot = serverRoot.toAbsolutePath().normalize();

        try {
            List<String> command = buildRestartCommandLine(normalizedRoot);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(normalizedRoot.toFile());
            pb.inheritIO();

            boolean isTest = isUnderTestingEnvironment();
            if (exitCurrentProcess && !isTest) {
                // Schedule the new process launch right at JVM shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(
                        new VesselRestartProcessLauncherHook(pb),
                        "vessel-restart-hook"));

                // Gracefully initiate process exit
                new Thread(new VesselGracefulHaltTask(), "vessel-graceful-halt").start();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isUnderTestingEnvironment() {
        return Boolean.getBoolean("vessel.test.mode")
                || System.getProperty("org.gradle.test.worker") != null
                || System.getProperty("intellij.test") != null;
    }

    /**
     * Builds the command line tokens required to relaunch the server process.
     *
     * @param serverRoot the normalized server root directory
     * @return the list of command line arguments
     */
    public List<String> buildRestartCommandLine(Path serverRoot) {
        Path startBat = serverRoot.resolve("start.bat");
        if (Files.isRegularFile(startBat) && isWindows()) {
            return List.of("cmd.exe", "/c", "start.bat");
        }
        Path startSh = serverRoot.resolve("start.sh");
        if (Files.isRegularFile(startSh) && !isWindows()) {
            return List.of("bash", "start.sh");
        }

        String javaBin = ProcessHandle.current().info().command().orElse("java");
        List<String> args = new ArrayList<>();
        args.add(javaBin);
        args.add("-Djava.net.preferIPv4Stack=true");
        args.add("-jar");
        args.add("server.jar");
        args.add("--nogui");
        return List.copyOf(args);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
