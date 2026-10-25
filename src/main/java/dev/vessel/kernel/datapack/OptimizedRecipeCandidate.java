package dev.vessel.kernel.datapack;

import java.util.Objects;

/**
 * Pre-compiled immutable recipe candidate indexed by a 64-bit ingredient bitmask
 * for single-cycle lookup during crafting interactions.
 *
 * @param recipeId namespaced recipe identifier
 * @param outputItemId namespaced output item identifier
 * @param outputCount quantity produced
 * @param ingredientMask 64-bit bitmask encoding required ingredient categories
 * @param isShapeless whether the recipe is shapeless
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record OptimizedRecipeCandidate(
        String recipeId,
        String outputItemId,
        int outputCount,
        long ingredientMask,
        boolean isShapeless
) {
    public OptimizedRecipeCandidate {
        Objects.requireNonNull(recipeId, "recipeId cannot be null");
        Objects.requireNonNull(outputItemId, "outputItemId cannot be null");
        if (outputCount <= 0) {
            outputCount = 1;
        }
    }

    /**
     * Evaluates if the candidate's ingredients are satisfied by the current grid bitmask.
     *
     * @param gridMask bitmask of items present in the crafting grid
     * @return true if grid satisfies all required ingredient bits
     */
    public boolean matchesGrid(long gridMask) {
        return (ingredientMask & gridMask) == ingredientMask;
    }
}
