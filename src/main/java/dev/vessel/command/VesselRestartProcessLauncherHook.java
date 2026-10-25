package dev.vessel.command;

import java.io.IOException;
import java.util.Objects;

/**
 * JVM shutdown hook that launches the replacement server process when the previous instance
 * shuts down during {@code /restart}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselRestartProcessLauncherHook implements Runnable {

    private final ProcessBuilder processBuilder;

    public VesselRestartProcessLauncherHook(ProcessBuilder processBuilder) {
        this.processBuilder = Objects.requireNonNull(processBuilder, "processBuilder must not be null");
    }

    @Override
    public void run() {
        try {
            processBuilder.start();
        } catch (IOException ignored) {
        }
    }
}
