package dev.vessel.kernel.transform.mixin;

/**
 * Strategy applied by {@link MixinConflictArbitrationEngine} to resolve competing @Overwrite mixins.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ArbitrationStrategy {

    /**
     * Decomposes competing overwrites into an ordered sequential execution chain.
     */
    CHAIN_SEQUENTIAL_INJECT,

    /**
     * Selects the highest priority candidate and safely suppresses conflicting duplicates.
     */
    PRIORITY_DISPATCH,

    /**
     * Fails fast by rejecting all conflicting overwrites to prevent undefined world corruption.
     */
    FAIL_FAST
}
