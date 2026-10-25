package dev.vessel.kernel.tick;

/**
 * Exception thrown when an asynchronous actor request across region strands
 * exceeds its timeout deadline.
 * <p>
 * Ensures that stalling or lagging adjacent regions do not propagate deadlocks
 * or thread exhaustion to neighboring actor threads.
 * </p>
 * <p>
 * Directly resolves ERR-31, ERR-32, and FOL-041..060.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class CrossRegionAskTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final long targetRegionKey;
    private final long timeoutMillis;

    /**
     * Constructs a new CrossRegionAskTimeoutException.
     *
     * @param targetRegionKey the packed key of the destination region that timed out
     * @param timeoutMillis the configured timeout duration in milliseconds
     */
    public CrossRegionAskTimeoutException(long targetRegionKey, long timeoutMillis) {
        super(String.format("Cross-region actor request to region 0x%016X timed out after %d ms",
                targetRegionKey, timeoutMillis));
        this.targetRegionKey = targetRegionKey;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Returns the target region key that timed out.
     *
     * @return 64-bit packed region key
     */
    public long targetRegionKey() {
        return targetRegionKey;
    }

    /**
     * Returns the timeout duration that was exceeded.
     *
     * @return timeout in milliseconds
     */
    public long timeoutMillis() {
        return timeoutMillis;
    }
}
