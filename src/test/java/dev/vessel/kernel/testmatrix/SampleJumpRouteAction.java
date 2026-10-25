package dev.vessel.kernel.testmatrix;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Concrete route action recording branchless jump invocations for unit testing.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SampleJumpRouteAction implements BranchlessRouteAction {

    private final AtomicLong executedCount = new AtomicLong(0);

    public SampleJumpRouteAction() {
    }

    @Override
    public void execute(int opcode, long payload) {
        executedCount.incrementAndGet();
    }

    public long getExecutedCount() {
        return executedCount.get();
    }
}
