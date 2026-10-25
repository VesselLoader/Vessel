package dev.vessel.kernel.testmatrix;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;

/**
 * Weak memory barrier synchronizer utilizing Java 21 {@link VarHandle} Release/Acquire fences
 * to minimize CPU hardware cache-coherence bus locking penalties compared to heavy volatile and MFENCE instructions.
 *
 * <p>Remediates OPT-ASM-0500..0700 by optimizing hardware memory bus utilization.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class WeakMemoryBarrierSynchronizer {

    private long sampleState = 0L;
    private static final VarHandle STATE_HANDLE;

    static {
        try {
            STATE_HANDLE = MethodHandles.lookup().findVarHandle(WeakMemoryBarrierSynchronizer.class, "sampleState", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public WeakMemoryBarrierSynchronizer() {
    }

    /**
     * Executes a unidirectional acquire fence, ensuring that subsequent memory reads
     * are not reordered before this point by either CPU or JIT compiler.
     */
    public void acquireFence() {
        VarHandle.acquireFence();
    }

    /**
     * Executes a unidirectional release fence, ensuring that prior memory writes
     * are visible and not reordered past this point.
     */
    public void releaseFence() {
        VarHandle.releaseFence();
    }

    /**
     * Executes a bi-directional full hardware memory fence (MFENCE / LOCK bus lock).
     */
    public void fullFence() {
        VarHandle.fullFence();
    }

    /**
     * Executes a LoadLoad memory fence preventing reads from floating before prior loads.
     */
    public void loadLoadFence() {
        VarHandle.loadLoadFence();
    }

    /**
     * Executes a StoreStore memory fence preventing writes from floating before prior stores.
     */
    public void storeStoreFence() {
        VarHandle.storeStoreFence();
    }

    /**
     * Sets value using Release semantics via VarHandle.
     *
     * @param handle target VarHandle
     * @param target instance container
     * @param value updated value
     */
    public void setRelease(VarHandle handle, Object target, Object value) {
        Objects.requireNonNull(handle, "handle must not be null");
        Objects.requireNonNull(target, "target must not be null");
        handle.setRelease(target, value);
    }

    /**
     * Reads value using Acquire semantics via VarHandle.
     *
     * @param handle target VarHandle
     * @param target instance container
     * @return current acquired value
     */
    public Object getAcquire(VarHandle handle, Object target) {
        Objects.requireNonNull(handle, "handle must not be null");
        Objects.requireNonNull(target, "target must not be null");
        return handle.getAcquire(target);
    }

    /**
     * Sets internal state using weak Release semantics.
     *
     * @param value new state value
     */
    public void setInternalStateRelease(long value) {
        STATE_HANDLE.setRelease(this, value);
    }

    /**
     * Reads internal state using Acquire semantics.
     *
     * @return current state
     */
    public long getInternalStateAcquire() {
        return (long) STATE_HANDLE.getAcquire(this);
    }

    /**
     * Benchmarks the throughput of the specified memory barrier mode.
     *
     * @param mode memory ordering mode to evaluate
     * @param iterations number of barrier executions
     * @return benchmark statistics report
     */
    public MemoryBarrierStats benchmarkBarrier(MemoryBarrierMode mode, int iterations) {
        Objects.requireNonNull(mode, "mode must not be null");
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }

        long startNanos = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            switch (mode) {
                case ACQUIRE_RELEASE -> {
                    STATE_HANDLE.setRelease(this, (long) i);
                    long read = (long) STATE_HANDLE.getAcquire(this);
                    if (read < 0) {
                        System.currentTimeMillis();
                    }
                }
                case FULL_FENCE -> {
                    STATE_HANDLE.setVolatile(this, (long) i);
                    long read = (long) STATE_HANDLE.getVolatile(this);
                    if (read < 0) {
                        System.currentTimeMillis();
                    }
                }
                case OPAQUE -> {
                    STATE_HANDLE.setOpaque(this, (long) i);
                    long read = (long) STATE_HANDLE.getOpaque(this);
                    if (read < 0) {
                        System.currentTimeMillis();
                    }
                }
                case PLAIN -> {
                    this.sampleState = i;
                    if (this.sampleState < 0) {
                        System.currentTimeMillis();
                    }
                }
            }
        }

        long elapsedNanos = Math.max(1L, System.nanoTime() - startNanos);
        double seconds = elapsedNanos / 1_000_000_000.0;
        double opsPerSec = iterations / (seconds > 0 ? seconds : 1.0);

        return new MemoryBarrierStats(iterations, elapsedNanos, opsPerSec, mode);
    }
}
