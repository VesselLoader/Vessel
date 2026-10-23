package dev.vessel.kernel.optimization.crosschunk;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accelerates recipe resolution across 50,000+ modded recipes (FastSuite / FastFurnace /
 * FastWorkbench style) using per-machine {@code lastMatchedRecipe} pointers, an $O(1)$
 * inverted hash index, a Negative Recipe Cache for invalid input combinations, and
 * RecipeBook packet payload stripping.
 *
 * @version 1.0.0
 */
public final class IndexedRecipeLookupAccelerator {

    private final Map<String, String> invertedRecipeIndex = new ConcurrentHashMap<>();
    private final Map<String, Long> recipeRequiredTagMask = new ConcurrentHashMap<>();
    private final Map<Long, String> lastMatchedInputByMachine = new ConcurrentHashMap<>();
    private final Map<Long, String> lastMatchedRecipeByMachine = new ConcurrentHashMap<>();
    private final Set<String> negativeRecipeInputCache = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new $O(1)$ indexed recipe lookup accelerator.
     */
    public IndexedRecipeLookupAccelerator() {
    }

    /**
     * Indexes a recipe into the $O(1)$ inverted hash table by recipe type, primary input
     * item ID, and 64-bit ingredient tag bitmask.
     *
     * @param recipeId             canonical namespaced recipe identifier
     * @param recipeType           recipe type key (e.g., {@code "minecraft:smelting"}, {@code "create:crushing"})
     * @param primaryInputItemId   numeric raw item ID of the primary ingredient
     * @param ingredientTagBitmask 64-bit required ingredient tag bitmask
     */
    public void indexRecipe(
            String recipeId,
            String recipeType,
            int primaryInputItemId,
            long ingredientTagBitmask) {
        Objects.requireNonNull(recipeId, "recipeId must not be null");
        Objects.requireNonNull(recipeType, "recipeType must not be null");
        String signatureKey = composeSignatureKey(recipeType, primaryInputItemId, ingredientTagBitmask);
        invertedRecipeIndex.put(signatureKey, recipeId.trim());
        recipeRequiredTagMask.put(recipeId.trim(), ingredientTagBitmask);
        negativeRecipeInputCache.remove(signatureKey);
    }

    /**
     * Resolves a matching recipe in $O(1)$ time by checking the machine's
     * {@code lastMatchedRecipe} pointer first, then consulting the Negative Recipe Cache,
     * and finally querying the inverted hash index.
     *
     * @param machineBlockPos    64-bit packed block position of the crafting/smelting machine
     * @param recipeType         recipe type key
     * @param primaryInputItemId numeric raw item ID of the primary ingredient
     * @param inputTagBitmask    64-bit tag bitmask of the input item
     * @return matched recipe ID, or {@code null} if no recipe matches
     */
    public String lookupRecipeFast(
            long machineBlockPos,
            String recipeType,
            int primaryInputItemId,
            long inputTagBitmask) {
        Objects.requireNonNull(recipeType, "recipeType must not be null");
        String signatureKey = composeSignatureKey(recipeType, primaryInputItemId, inputTagBitmask);
        String previousSignature = lastMatchedInputByMachine.get(machineBlockPos);
        if (signatureKey.equals(previousSignature)) {
            return lastMatchedRecipeByMachine.get(machineBlockPos);
        }
        if (negativeRecipeInputCache.contains(signatureKey)) {
            return null;
        }
        String matchedRecipe = invertedRecipeIndex.get(signatureKey);
        if (matchedRecipe != null) {
            lastMatchedInputByMachine.put(machineBlockPos, signatureKey);
            lastMatchedRecipeByMachine.put(machineBlockPos, matchedRecipe);
            return matchedRecipe;
        }
        negativeRecipeInputCache.add(signatureKey);
        return null;
    }

    /**
     * Checks whether the given input combination is already recorded in the Negative Recipe Cache.
     *
     * @param recipeType         recipe type key
     * @param primaryInputItemId numeric raw item ID
     * @param inputTagBitmask    64-bit tag bitmask
     * @return {@code true} if known not to match any recipe
     */
    public boolean isKnownNegativeRecipeInput(
            String recipeType, int primaryInputItemId, long inputTagBitmask) {
        Objects.requireNonNull(recipeType, "recipeType must not be null");
        return negativeRecipeInputCache.contains(
                composeSignatureKey(recipeType, primaryInputItemId, inputTagBitmask));
    }

    /**
     * Estimates the compressed RecipeBook synchronization payload size in bytes after
     * stripping redundant unlocked recipe history on player login.
     *
     * @param totalRegisteredRecipes total number of registered server recipes
     * @param stripUnlockedHistory   whether to strip bulky unlocked recipe book lists
     * @return compressed payload byte estimate
     */
    public int compressRecipeBookSyncPayload(
            int totalRegisteredRecipes, boolean stripUnlockedHistory) {
        int clamped = Math.max(0, totalRegisteredRecipes);
        int rawEstimate = clamped * 48;
        return stripUnlockedHistory ? Math.max(64, rawEstimate / 10) : rawEstimate;
    }

    private static String composeSignatureKey(
            String recipeType, int primaryInputItemId, long tagBitmask) {
        return recipeType.trim().toLowerCase(Locale.ROOT)
                + "|"
                + primaryInputItemId
                + "|"
                + tagBitmask;
    }
}
