package dev.vessel.kernel.gui.security;

import java.util.Objects;

/**
 * Result outcome of a carried cursor ghost item neutralization operation.
 *
 * @param wasNeutralized whether a virtual ghost item was destroyed
 * @param resultingItemId the resulting item identifier remaining on cursor/inventory
 * @param resultingCount the resulting item count
 * @param actionTaken descriptive name of the security action taken
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record NeutralizationResult(
        boolean wasNeutralized,
        String resultingItemId,
        int resultingCount,
        String actionTaken
) {
    public NeutralizationResult {
        Objects.requireNonNull(resultingItemId, "resultingItemId cannot be null");
        Objects.requireNonNull(actionTaken, "actionTaken cannot be null");
    }

    public static NeutralizationResult purged(String destroyedItem) {
        return new NeutralizationResult(true, ContainerClickContext.ITEM_EMPTY, 0, "PURGED_VIRTUAL_ITEM: " + destroyedItem);
    }

    public static NeutralizationResult preserved(String realItem, int count) {
        return new NeutralizationResult(false, realItem, count, "PRESERVED_REAL_ITEM");
    }
}
