package dev.vessel.command;

/**
 * Halts the current JVM process with exit code {@code 0} after allowing pending shutdown
 * tasks and socket closures to complete.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselGracefulHaltTask implements Runnable {

    @Override
    public void run() {
        try {
            Thread.sleep(1500L);
            System.exit(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(0);
        }
    }
}
