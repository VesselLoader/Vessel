package dev.vessel.kernel.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates Fabric's nested {@code TransactionContext} semantics on top of Forge/NeoForge
 * {@code Action.SIMULATE} / {@code Action.EXECUTE} capabilities, preventing item or fluid
 * duplication on aborted nested transactions.
 *
 * <p>Uses flat maps with composite keys and zero inner classes in strict compliance with Rule 17.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-28)
 */
public final class TransactionContextSimulator {

    private final AtomicInteger currentDepth = new AtomicInteger(0);
    private final Map<String, Long> activeSlotAmounts = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> depthModifiedKeys = new ConcurrentHashMap<>();
    private final Map<String, Long> depthRollbackSnapshots = new ConcurrentHashMap<>();

    /**
     * Opens a new nested transaction scope and returns its 1-based depth level.
     *
     * @return positive transaction depth identifier
     */
    public int beginNestedTransaction() {
        int depth = currentDepth.incrementAndGet();
        depthModifiedKeys.put(depth, new ArrayList<>());
        return depth;
    }

    /**
     * Records a simulated slot or tank mutation at the specified transaction depth.
     *
     * @param transactionDepth active transaction depth
     * @param slotKey          unique slot or tank key
     * @param previousAmount   amount prior to this mutation
     * @param newAmount        new simulated amount
     */
    public void recordSimulatedMutation(
            int transactionDepth,
            String slotKey,
            long previousAmount,
            long newAmount
    ) {
        Objects.requireNonNull(slotKey, "slotKey cannot be null");
        if (transactionDepth <= 0 || transactionDepth > currentDepth.get()) {
            throw new IllegalArgumentException("Invalid transaction depth: " + transactionDepth);
        }

        String snapshotKey = transactionDepth + "::" + slotKey;
        depthRollbackSnapshots.putIfAbsent(snapshotKey, previousAmount);
        depthModifiedKeys.computeIfAbsent(transactionDepth, d -> new ArrayList<>()).add(slotKey);
        activeSlotAmounts.put(slotKey, newAmount);
    }

    /**
     * Commits all mutations recorded at the specified transaction depth to the parent scope
     * (or finalizes them if at root depth {@code 1}).
     *
     * @param transactionDepth active transaction depth to commit
     */
    public void commitTransaction(int transactionDepth) {
        if (transactionDepth <= 0 || transactionDepth != currentDepth.get()) {
            throw new IllegalStateException(
                    "Cannot commit out-of-order transaction depth " + transactionDepth
                            + " (current depth: " + currentDepth.get() + ")"
            );
        }

        List<String> modified = depthModifiedKeys.remove(transactionDepth);
        if (modified != null) {
            for (String slotKey : modified) {
                Long prev = depthRollbackSnapshots.remove(transactionDepth + "::" + slotKey);
                if (transactionDepth > 1 && prev != null) {
                    int parentDepth = transactionDepth - 1;
                    String parentSnapshotKey = parentDepth + "::" + slotKey;
                    depthRollbackSnapshots.putIfAbsent(parentSnapshotKey, prev);
                    depthModifiedKeys.computeIfAbsent(parentDepth, d -> new ArrayList<>()).add(slotKey);
                }
            }
        }
        currentDepth.decrementAndGet();
    }

    /**
     * Aborts the specified transaction depth and rolls back all slot mutations recorded at that
     * depth to their pre-transaction values.
     *
     * @param transactionDepth active transaction depth to abort
     */
    public void abortAndRollbackTransaction(int transactionDepth) {
        if (transactionDepth <= 0 || transactionDepth != currentDepth.get()) {
            throw new IllegalStateException(
                    "Cannot abort out-of-order transaction depth " + transactionDepth
                            + " (current depth: " + currentDepth.get() + ")"
            );
        }

        List<String> modified = depthModifiedKeys.remove(transactionDepth);
        if (modified != null) {
            for (String slotKey : modified) {
                Long prev = depthRollbackSnapshots.remove(transactionDepth + "::" + slotKey);
                if (prev != null) {
                    activeSlotAmounts.put(slotKey, prev);
                }
            }
        }
        currentDepth.decrementAndGet();
    }

    /**
     * Returns the current committed or simulated amount for the given slot key.
     *
     * @param slotKey unique slot or tank key
     * @return current amount
     */
    public long getCurrentSlotAmount(String slotKey) {
        Objects.requireNonNull(slotKey, "slotKey cannot be null");
        return activeSlotAmounts.getOrDefault(slotKey, 0L);
    }
}
