package dev.vessel.kernel.datapack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LootTablePrecomputedEvaluator} verifying flat binary tree traversal.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class LootTablePrecomputedEvaluatorTest {

    @Test
    @DisplayName("Evaluate: Drops item when required context conditions are satisfied")
    void testLootTableEvaluationSatisfied() {
        LootTablePrecomputedEvaluator evaluator = new LootTablePrecomputedEvaluator();

        String tableId = "minecraft:entities/zombie";
        List<OptimizedLootTableRule> rules = List.of(
                new OptimizedLootTableRule("zombie_rotten_flesh", tableId, 0L, "minecraft:rotten_flesh", 1, 2, 1.0f),
                new OptimizedLootTableRule("zombie_iron_ingot", tableId, LootTablePrecomputedEvaluator.MASK_KILLED_BY_PLAYER, "minecraft:iron_ingot", 1, 1, 1.0f)
        );

        evaluator.compileAndRegisterRules(tableId, rules);

        long contextWithPlayerKill = LootTablePrecomputedEvaluator.MASK_KILLED_BY_PLAYER;
        List<String> drops = evaluator.evaluate(tableId, contextWithPlayerKill, 0.5f);

        assertThat(drops).contains("minecraft:rotten_flesh", "minecraft:iron_ingot");
    }

    @Test
    @DisplayName("Evaluate: Excludes drop when context condition is missing (e.g. non-player kill)")
    void testLootTableEvaluationUnsatisfied() {
        LootTablePrecomputedEvaluator evaluator = new LootTablePrecomputedEvaluator();

        String tableId = "minecraft:entities/zombie";
        List<OptimizedLootTableRule> rules = List.of(
                new OptimizedLootTableRule("zombie_iron_rare", tableId, LootTablePrecomputedEvaluator.MASK_KILLED_BY_PLAYER, "minecraft:iron_ingot", 1, 1, 1.0f)
        );

        evaluator.compileAndRegisterRules(tableId, rules);

        // Killed by environment/fall, not player
        long contextWithoutPlayer = 0L;
        List<String> drops = evaluator.evaluate(tableId, contextWithoutPlayer, 0.1f);

        assertThat(drops).doesNotContain("minecraft:iron_ingot");
    }

    @Test
    @DisplayName("ZeroAllocation: Evaluates into caller provided list without extra collections")
    void testZeroAllocationEvaluationInto() {
        LootTablePrecomputedEvaluator evaluator = new LootTablePrecomputedEvaluator();

        String tableId = "minecraft:blocks/stone";
        PrecomputedBinaryLootNode[] nodes = new PrecomputedBinaryLootNode[] {
                new PrecomputedBinaryLootNode(
                        LootTablePrecomputedEvaluator.MASK_SILK_TOUCH,
                        LootTablePrecomputedEvaluator.TERMINAL_JUMP,
                        1,
                        true,
                        "minecraft:stone",
                        1,
                        1,
                        1.0f
                ),
                new PrecomputedBinaryLootNode(
                        0L,
                        LootTablePrecomputedEvaluator.TERMINAL_JUMP,
                        LootTablePrecomputedEvaluator.TERMINAL_JUMP,
                        true,
                        "minecraft:cobblestone",
                        1,
                        1,
                        1.0f
                )
        };

        evaluator.registerTable(tableId, nodes);

        List<String> collector = new ArrayList<>();
        evaluator.evaluateInto(nodes, LootTablePrecomputedEvaluator.MASK_SILK_TOUCH, 0.0f, collector);

        assertThat(collector).containsExactly("minecraft:stone");
    }
}
