package dev.vessel.kernel.optimization.supermod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enforces per-tick instruction quotas on CC: Tweaked / ComputerCraft Lua virtual machine strands
 * so runaway user scripts cannot starve server region threads.
 *
 * @version 1.0.0
 */
public final class LuaSandboxStrandThrottler {

    private final Map<Integer, AtomicInteger> computerInstructionCounters =
            new ConcurrentHashMap<>();

    /**
     * Consumes VM instruction quota for a computer and checks whether it remains within the
     * per-tick instruction budget.
     *
     * @param computerId             the ComputerCraft / CC:Tweaked computer ID
     * @param executedInstructions   the number of bytecode instructions executed in this step
     * @param maxInstructionsPerTick the maximum instructions allowed per tick
     * @return {@code true} if within quota, or {@code false} if the computer must yield for the
     *         remainder of the tick
     */
    public boolean consumeInstructionQuota(
            int computerId,
            int executedInstructions,
            int maxInstructionsPerTick) {
        int updated = computerInstructionCounters
                .computeIfAbsent(computerId, ignored -> new AtomicInteger(0))
                .addAndGet(Math.max(0, executedInstructions));
        return updated <= maxInstructionsPerTick;
    }

    /**
     * Resets all per-computer instruction counters at the start of a new server tick.
     */
    public void resetTickQuotas() {
        computerInstructionCounters.clear();
    }
}
