package dev.vessel.kernel.jvm.dfu;

/**
 * Execution status resulting from DFU AOT path evaluation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum DfuOptimizationStatus {
    BYPASS,
    AOT_FLAT_TABLE,
    FALLBACK_DYNAMIC,
    COMPLETED
}
