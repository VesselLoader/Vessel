package dev.vessel.kernel.datapack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-speed RAM precomputed loot table evaluator using flat binary decision trees.
 * <p>
 * Eliminates ERR-93 (Loot table predicate recursion stalls and GC pauses during mass entity drops).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LootTablePrecomputedEvaluator {

    public static final long MASK_KILLED_BY_PLAYER = 1L << 0;
    public static final long MASK_ON_FIRE          = 1L << 1;
    public static final long MASK_SILK_TOUCH       = 1L << 2;
    public static final long MASK_EXPLOSION        = 1L << 3;
    public static final long MASK_LOOTING_1        = 1L << 4;
    public static final long MASK_LOOTING_2        = 1L << 5;
    public static final long MASK_LOOTING_3        = 1L << 6;
    public static final long MASK_WATER_SUBMERGED  = 1L << 7;

    public static final int TERMINAL_JUMP = -1;

    private final Map<String, PrecomputedBinaryLootNode[]> tableRegistry = new ConcurrentHashMap<>();

    /**
     * Constructs a default LootTablePrecomputedEvaluator.
     */
    public LootTablePrecomputedEvaluator() {
    }

    /**
     * Registers a pre-compiled flat binary array for a loot table.
     *
     * @param tableId namespaced loot table identifier
     * @param flatNodes precomputed binary nodes array
     */
    public void registerTable(String tableId, PrecomputedBinaryLootNode[] flatNodes) {
        Objects.requireNonNull(tableId, "tableId cannot be null");
        Objects.requireNonNull(flatNodes, "flatNodes cannot be null");
        tableRegistry.put(tableId, flatNodes.clone());
    }

    /**
     * Compiles a set of OptimizedLootTableRule items into a flat binary tree array in RAM.
     *
     * @param tableId namespaced table identifier
     * @param rules collection of loot rules
     */
    public void compileAndRegisterRules(String tableId, List<OptimizedLootTableRule> rules) {
        Objects.requireNonNull(tableId, "tableId cannot be null");
        Objects.requireNonNull(rules, "rules cannot be null");

        int size = rules.size();
        PrecomputedBinaryLootNode[] flatArray = new PrecomputedBinaryLootNode[size];

        for (int i = 0; i < size; i++) {
            OptimizedLootTableRule rule = rules.get(i);
            int nextOffset = (i < size - 1) ? 1 : TERMINAL_JUMP;

            flatArray[i] = new PrecomputedBinaryLootNode(
                    rule.conditionMask(),
                    nextOffset,
                    nextOffset,
                    true,
                    rule.dropItemId(),
                    rule.minCount(),
                    rule.maxCount(),
                    rule.dropChance()
            );
        }

        registerTable(tableId, flatArray);
    }

    /**
     * Evaluates a loot table using flat binary tree traversal without heap allocations.
     *
     * @param tableId namespaced table identifier
     * @param contextMask 64-bit contextual bitmask
     * @param randomRoll random float value [0.0, 1.0]
     * @return list of dropped item identifiers
     */
    public List<String> evaluate(String tableId, long contextMask, float randomRoll) {
        PrecomputedBinaryLootNode[] nodes = tableRegistry.get(tableId);
        if (nodes == null || nodes.length == 0) {
            return Collections.emptyList();
        }

        List<String> results = new ArrayList<>();
        evaluateInto(nodes, contextMask, randomRoll, results);
        return results;
    }

    /**
     * Zero-allocation hot path evaluation populating an external collector list.
     *
     * @param nodes precomputed flat binary tree
     * @param contextMask context conditions bitmask
     * @param randomRoll roll probability value
     * @param collector target list for dropped items
     */
    public void evaluateInto(
            PrecomputedBinaryLootNode[] nodes,
            long contextMask,
            float randomRoll,
            List<String> collector
    ) {
        Objects.requireNonNull(nodes, "nodes cannot be null");
        Objects.requireNonNull(collector, "collector cannot be null");

        int index = 0;
        int length = nodes.length;

        while (index >= 0 && index < length) {
            PrecomputedBinaryLootNode node = nodes[index];
            boolean conditionMet = node.test(contextMask);

            if (conditionMet && node.isLeaf()) {
                if (randomRoll <= node.dropChance() && !node.dropItemId().isEmpty()) {
                    collector.add(node.dropItemId());
                }
            }

            int jump = conditionMet ? node.jumpOffsetIfTrue() : node.jumpOffsetIfFalse();
            if (jump == TERMINAL_JUMP) {
                break;
            }
            index += jump;
        }
    }

    /**
     * Checks if a loot table is registered in RAM.
     *
     * @param tableId table identifier
     * @return true if precomputed table exists
     */
    public boolean containsTable(String tableId) {
        return tableId != null && tableRegistry.containsKey(tableId);
    }

    /**
     * Retrieves the precomputed binary nodes array for a table.
     *
     * @param tableId table identifier
     * @return nodes array or null
     */
    public PrecomputedBinaryLootNode[] getNodes(String tableId) {
        return (tableId != null) ? tableRegistry.get(tableId) : null;
    }

    /**
     * Clears all registered precomputed tables.
     */
    public void clear() {
        tableRegistry.clear();
    }
}
