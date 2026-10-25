package dev.vessel.kernel.datapack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FastRecipeBitmaskMatcher} verifying 64-bit bitmask crafting matching.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class FastRecipeBitmaskMatcherTest {

    @Test
    @DisplayName("Encoder: Assigns deterministic 64-bit primitive masks to ingredient items")
    void testItemMaskAssignment() {
        CraftingGridBitmaskEncoder encoder = new CraftingGridBitmaskEncoder();

        long ironMask = encoder.getOrAssignItemMask("minecraft:iron_ingot");
        long stickMask = encoder.getOrAssignItemMask("minecraft:stick");

        assertThat(ironMask).isNotZero();
        assertThat(stickMask).isNotZero();
        assertThat(ironMask & stickMask).isEqualTo(0L); // disjoint bits
    }

    @Test
    @DisplayName("Matcher: Successfully matches crafting recipe when all ingredient bits are satisfied")
    void testRecipeMatching() {
        FastRecipeBitmaskMatcher matcher = new FastRecipeBitmaskMatcher();

        String[] ironSwordIngredients = new String[] { "minecraft:iron_ingot", "minecraft:stick" };
        matcher.registerRecipe("recipe:iron_sword", "minecraft:iron_sword", 1, ironSwordIngredients, false);

        String[] gridWithIngredients = new String[] {
                "minecraft:iron_ingot",
                "minecraft:iron_ingot",
                "minecraft:stick"
        };
        long gridMask = matcher.encodeGrid(gridWithIngredients);

        OptimizedRecipeCandidate match = matcher.findFirstMatch(gridMask);
        assertThat(match).isNotNull();
        assertThat(match.outputItemId()).isEqualTo("minecraft:iron_sword");
    }

    @Test
    @DisplayName("Matcher: Fails to match when required ingredient is missing from crafting grid")
    void testRecipeMissingIngredient() {
        FastRecipeBitmaskMatcher matcher = new FastRecipeBitmaskMatcher();

        String[] pickaxeIngredients = new String[] { "minecraft:diamond", "minecraft:stick" };
        matcher.registerRecipe("recipe:diamond_pickaxe", "minecraft:diamond_pickaxe", 1, pickaxeIngredients, false);

        // Grid only contains diamond, missing stick
        String[] incompleteGrid = new String[] { "minecraft:diamond" };
        long gridMask = matcher.encodeGrid(incompleteGrid);

        OptimizedRecipeCandidate match = matcher.findFirstMatch(gridMask);
        assertThat(match).isNull();
    }

    @Test
    @DisplayName("Matcher: Finds multiple matching recipes for combined grid items")
    void testFindAllMatches() {
        FastRecipeBitmaskMatcher matcher = new FastRecipeBitmaskMatcher();

        matcher.registerRecipe("recipe:torch", "minecraft:torch", 4, new String[] { "minecraft:coal", "minecraft:stick" }, true);
        matcher.registerRecipe("recipe:stick", "minecraft:stick", 4, new String[] { "minecraft:oak_planks" }, true);

        long combinedGridMask = matcher.encodeGrid(new String[] { "minecraft:coal", "minecraft:stick", "minecraft:oak_planks" });

        List<OptimizedRecipeCandidate> matches = matcher.findAllMatches(combinedGridMask);
        assertThat(matches).hasSize(2);
    }
}
