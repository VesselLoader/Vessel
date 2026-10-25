package dev.vessel.kernel.ir;

/**
 * Immutable report representing the result of bridge method deduplication on a class.
 *
 * @param inspectedMethodCount total methods analyzed
 * @param removedBridgeCount redundant synthetic bridge methods pruned
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BridgeDeduplicationReport(
        int inspectedMethodCount,
        int removedBridgeCount
) {
    public static final BridgeDeduplicationReport EMPTY = new BridgeDeduplicationReport(0, 0);

    public boolean hasPrunedBridges() {
        return removedBridgeCount > 0;
    }
}
