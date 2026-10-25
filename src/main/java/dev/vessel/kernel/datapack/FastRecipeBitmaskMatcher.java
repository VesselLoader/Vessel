package dev.vessel.kernel.datapack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance 64-bit primitive bitmask crafting recipe matcher.
 * <p>
 * Eliminates ERR-25 and INV-0401..0800 (linear recipe graph iteration lag,
 * crafting table tick delays and inventory synchronization stalls).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastRecipeBitmaskMatcher {

    public static final long EMPTY_MASK = 0L;

    private final CraftingGridBitmaskEncoder encoder;
    private final List<OptimizedRecipeCandidate> registeredRecipes;

    /**
     * Constructs a FastRecipeBitmaskMatcher with an internal bitmask encoder.
     */
    public FastRecipeBitmaskMatcher() {
        this.encoder = new CraftingGridBitmaskEncoder();
        this.registeredRecipes = new CopyOnWriteArrayList<>();
    }

    /**
     * Constructs a FastRecipeBitmaskMatcher using a shared encoder.
     *
     * @param encoder shared bitmask encoder
     */
    public FastRecipeBitmaskMatcher(CraftingGridBitmaskEncoder encoder) {
        this.encoder = Objects.requireNonNull(encoder, "encoder cannot be null");
        this.registeredRecipes = new CopyOnWriteArrayList<>();
    }

    /**
     * Registers a pre-compiled recipe candidate into the matcher.
     *
     * @param recipe recipe candidate
     */
    public void registerRecipe(OptimizedRecipeCandidate recipe) {
        Objects.requireNonNull(recipe, "recipe cannot be null");
        registeredRecipes.add(recipe);
    }

    /**
     * Registers a recipe by ingredient list and outputs.
     *
     * @param recipeId recipe ID
     * @param outputItem output item ID
     * @param count output count
     * @param ingredientItemIds array of ingredients required
     * @param isShapeless whether recipe is shapeless
     * @return registered candidate
     */
    public OptimizedRecipeCandidate registerRecipe(
            String recipeId,
            String outputItem,
            int count,
            String[] ingredientItemIds,
            boolean isShapeless
    ) {
        long ingredientMask = encoder.encodeGrid(ingredientItemIds);
        OptimizedRecipeCandidate candidate = new OptimizedRecipeCandidate(
                recipeId,
                outputItem,
                count,
                ingredientMask,
                isShapeless
        );
        registerRecipe(candidate);
        return candidate;
    }

    /**
     * Finds the first recipe matching the crafting grid bitmask.
     *
     * @param gridMask combined 64-bit bitmask of current crafting grid
     * @return matching recipe candidate, or null if no recipe matches
     */
    public OptimizedRecipeCandidate findFirstMatch(long gridMask) {
        if (gridMask == EMPTY_MASK) {
            return null;
        }

        for (OptimizedRecipeCandidate recipe : registeredRecipes) {
            if (recipe.matchesGrid(gridMask)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * Finds all recipes satisfied by the current crafting grid bitmask.
     *
     * @param gridMask combined 64-bit bitmask of current crafting grid
     * @return unmodifiable list of matching recipe candidates
     */
    public List<OptimizedRecipeCandidate> findAllMatches(long gridMask) {
        if (gridMask == EMPTY_MASK) {
            return Collections.emptyList();
        }

        List<OptimizedRecipeCandidate> matches = new ArrayList<>();
        for (OptimizedRecipeCandidate recipe : registeredRecipes) {
            if (recipe.matchesGrid(gridMask)) {
                matches.add(recipe);
            }
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * Helper to encode crafting grid items into a 64-bit mask.
     *
     * @param gridItems item identifiers
     * @return 64-bit mask
     */
    public long encodeGrid(String[] gridItems) {
        return encoder.encodeGrid(gridItems);
    }

    /**
     * Returns total count of registered recipes.
     *
     * @return recipe count
     */
    public int getRecipeCount() {
        return registeredRecipes.size();
    }

    /**
     * Obtains the internal encoder.
     *
     * @return encoder
     */
    public CraftingGridBitmaskEncoder getEncoder() {
        return encoder;
    }

    /**
     * Clears all registered recipes.
     */
    public void clear() {
        registeredRecipes.clear();
    }
}
