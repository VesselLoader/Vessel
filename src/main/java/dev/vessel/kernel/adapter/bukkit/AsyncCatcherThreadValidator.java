package dev.vessel.kernel.adapter.bukkit;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates thread execution context for Bukkit and Spigot API invocations (AsyncCatcher).
 *
 * <p>Prevents illegal multi-threaded mutations to world state, entities, and inventories, while
 * accommodating Folia regional ticking threads and scoped execution bypasses.</p>
 *
 * <p>Quells: ERR-70.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class AsyncCatcherThreadValidator {

    /**
     * Default error prefix format.
     */
    public static final String ERROR_PREFIX = "Asynchronous ";

    /**
     * Default error suffix format.
     */
    public static final String ERROR_SUFFIX = "! Thread: ";

    private volatile Thread primaryThread;
    private volatile boolean enabled = true;
    private final Set<Thread> validRegionThreads = ConcurrentHashMap.newKeySet();
    private final ThreadLocal<Integer> bypassDepth = new ThreadLocal<>();

    /**
     * Constructs a validator using the current calling thread as default primary thread.
     */
    public AsyncCatcherThreadValidator() {
        this(Thread.currentThread());
    }

    /**
     * Constructs a validator bound to a specific primary thread.
     *
     * @param primaryThread the main server loop thread
     */
    public AsyncCatcherThreadValidator(Thread primaryThread) {
        this.primaryThread = Objects.requireNonNull(primaryThread, "primaryThread cannot be null");
    }

    /**
     * Catches and validates thread access for a specific operation.
     *
     * @param reason the operation name (e.g., "entity world add", "block physics update")
     * @throws IllegalStateException if called asynchronously outside permitted server threads
     */
    public void catchOp(String reason) {
        if (!enabled) {
            return;
        }

        Integer depth = bypassDepth.get();
        if (depth != null && depth.intValue() > 0) {
            return;
        }

        Thread current = Thread.currentThread();
        if (current == primaryThread || validRegionThreads.contains(current)) {
            return;
        }

        String opReason = (reason != null && !reason.isBlank()) ? reason : "operation";
        throw new IllegalStateException(ERROR_PREFIX + opReason + ERROR_SUFFIX + current.getName());
    }

    /**
     * Acquires a scoped auto-closeable bypass token for the current thread.
     *
     * @return bypass token to use in try-with-resources
     */
    public AsyncCatcherBypassToken acquireBypass() {
        Integer depth = bypassDepth.get();
        int newDepth = (depth == null) ? 1 : depth.intValue() + 1;
        bypassDepth.set(Integer.valueOf(newDepth));
        return new AsyncCatcherBypassToken(this);
    }

    /**
     * Releases one level of bypass for the current thread.
     */
    void releaseBypass() {
        Integer depth = bypassDepth.get();
        if (depth != null) {
            int newDepth = depth.intValue() - 1;
            if (newDepth <= 0) {
                bypassDepth.remove();
            } else {
                bypassDepth.set(Integer.valueOf(newDepth));
            }
        }
    }

    /**
     * Registers a valid Folia region execution thread.
     *
     * @param thread region thread to register
     */
    public void registerRegionThread(Thread thread) {
        Objects.requireNonNull(thread, "thread cannot be null");
        validRegionThreads.add(thread);
    }

    /**
     * Unregisters a Folia region execution thread.
     *
     * @param thread region thread to unregister
     * @return true if removed, false otherwise
     */
    public boolean unregisterRegionThread(Thread thread) {
        Objects.requireNonNull(thread, "thread cannot be null");
        return validRegionThreads.remove(thread);
    }

    /**
     * Updates the primary server main thread.
     *
     * @param primaryThread new primary thread
     */
    public void setPrimaryThread(Thread primaryThread) {
        this.primaryThread = Objects.requireNonNull(primaryThread, "primaryThread cannot be null");
    }

    /**
     * Retrieves the current primary server thread.
     *
     * @return primary thread
     */
    public Thread getPrimaryThread() {
        return primaryThread;
    }

    /**
     * Enables or disables asynchronous catch checks globally.
     *
     * @param enabled validation state
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if validator is active.
     *
     * @return true if active
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the count of registered region threads.
     *
     * @return region thread count
     */
    public int getRegionThreadCount() {
        return validRegionThreads.size();
    }
}
