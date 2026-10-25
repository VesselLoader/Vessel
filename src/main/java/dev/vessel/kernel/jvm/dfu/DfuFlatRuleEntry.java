package dev.vessel.kernel.jvm.dfu;

import java.util.Objects;

/**
 * Immutable binary-encoded rule entry in the flattened DFU lookup table.
 *
 * @param fromVersion source data version
 * @param toVersion destination data version
 * @param payloadType target payload category
 * @param ruleBitmask 64-bit primitive bitmask encoding active transformation rules
 * @param handlerOffset direct jump index in the binary execution pipeline
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DfuFlatRuleEntry(
        int fromVersion,
        int toVersion,
        DfuPayloadType payloadType,
        long ruleBitmask,
        int handlerOffset
) {
    public DfuFlatRuleEntry {
        if (fromVersion < 0) {
            throw new IllegalArgumentException("fromVersion must be non-negative: " + fromVersion);
        }
        if (toVersion < fromVersion) {
            throw new IllegalArgumentException("toVersion (" + toVersion + ") cannot be less than fromVersion (" + fromVersion + ")");
        }
        Objects.requireNonNull(payloadType, "payloadType cannot be null");
    }
}
