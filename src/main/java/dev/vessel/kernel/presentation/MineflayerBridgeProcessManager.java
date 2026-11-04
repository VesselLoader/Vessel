package dev.vessel.kernel.presentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Manages the external Node.js Mineflayer headless client bot process for automated server verification.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class MineflayerBridgeProcessManager {

    public boolean isNodeJsInstalled() {
        try {
            Process process = new ProcessBuilder("node", "-v").start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException ignored) {
            return false;
        }
    }

    public Process launchMineflayerBot(Path botScriptPath, String host, int port, String botUsername) throws IOException {
        Objects.requireNonNull(botScriptPath, "botScriptPath cannot be null");
        Objects.requireNonNull(host, "host cannot be null");
        if (!Files.isRegularFile(botScriptPath)) {
            throw new IllegalArgumentException("Bot script does not exist: " + botScriptPath);
        }

        ProcessBuilder builder = new ProcessBuilder(
                "node",
                botScriptPath.toAbsolutePath().toString(),
                host,
                String.valueOf(port),
                botUsername != null ? botUsername : "VesselTesterBot"
        );
        builder.redirectErrorStream(true);
        return builder.start();
    }

    public String readProcessOutputUntilFinished(Process process, long timeoutSeconds) throws IOException, InterruptedException {
        Objects.requireNonNull(process, "process cannot be null");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        return sb.toString();
    }
}
