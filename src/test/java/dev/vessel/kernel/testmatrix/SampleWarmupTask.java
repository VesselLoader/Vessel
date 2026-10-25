package dev.vessel.kernel.testmatrix;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete test task executing warm-up iterations for JIT priority sorter evaluation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SampleWarmupTask implements Runnable {

    private final AtomicInteger invocationCounter = new AtomicInteger(0);

    public SampleWarmupTask() {
    }

    @Override
    public void run() {
        invocationCounter.incrementAndGet();
    }

    public int getInvocations() {
        return invocationCounter.get();
    }
}
