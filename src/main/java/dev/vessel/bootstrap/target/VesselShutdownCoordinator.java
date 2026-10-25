package dev.vessel.bootstrap.target;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coordinates graceful server shutdown, executing registered cleanup hooks in reverse
 * registration order (LIFO) and flushing a shutdown marker to the server root.
 *
 * @version 1.0.0
 */
public final class VesselShutdownCoordinator {

    private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
    private final AtomicInteger executedHookCount = new AtomicInteger(0);
    private final AtomicBoolean shutdownCompleted = new AtomicBoolean(false);

    /**
     * Creates a new shutdown coordinator.
     */
    public VesselShutdownCoordinator() {
    }

    /**
     * Registers a cleanup task to be executed during graceful shutdown.
     *
     * @param cleanupTask the cleanup runnable
     */
    public void registerShutdownHook(Runnable cleanupTask) {
        Objects.requireNonNull(cleanupTask, "cleanupTask must not be null");
        shutdownHooks.add(cleanupTask);
    }

    /**
     * Executes all registered cleanup hooks in reverse order (LIFO) and writes a clean
     * shutdown marker inside the server root directory.
     *
     * @param serverRoot the server root directory
     */
    public void executeGracefulShutdown(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        if (!shutdownCompleted.compareAndSet(false, true)) {
            return;
        }
        for (int i = shutdownHooks.size() - 1; i >= 0; i--) {
            Runnable hook = shutdownHooks.get(i);
            try {
                hook.run();
                executedHookCount.incrementAndGet();
            } catch (RuntimeException ignored) {
                // Continue executing remaining shutdown hooks even if one throws
            }
        }
        try {
            Path tempDir = serverRoot.resolve("temp");
            Files.createDirectories(tempDir);
            Files.writeString(
                    tempDir.resolve("last-graceful-shutdown.txt"),
                    "status=CLEAN\nhooks=" + executedHookCount.get() + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Non-fatal if disk marker cannot be written
        }
    }

    /**
     * Returns the number of cleanup hooks executed during graceful shutdown.
     *
     * @return count of executed hooks
     */
    public int getExecutedHookCount() {
        return executedHookCount.get();
    }

    /**
     * Returns whether graceful shutdown has completed.
     *
     * @return {@code true} if shutdown was executed
     */
    public boolean isShutdownCompleted() {
        return shutdownCompleted.get();
    }
}
