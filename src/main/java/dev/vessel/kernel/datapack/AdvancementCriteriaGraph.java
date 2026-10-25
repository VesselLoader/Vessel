package dev.vessel.kernel.datapack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Directed Acyclic Graph (DAG) for high-performance player advancement progression.
 * <p>
 * Eliminates exhaustive every-tick advancement scans by indexing criteria listeners
 * by trigger type and dynamically pruning evaluation branches whose parent prerequisites
 * remain locked or whose trigger-matching criteria are already granted.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class AdvancementCriteriaGraph {

    // Graph structure
    private final Map<String, Set<String>> parentToChildren;
    private final Map<String, String> childToParent;
    private final Map<String, Set<String>> advancementCriteria;
    private final Map<String, Map<String, String>> criterionTriggers; // advId -> (criterion -> triggerType)
    private final Map<String, Set<String>> triggerToAdvancements;    // triggerType -> Set<advId>
    private final Set<String> rootAdvancements;
    private final Set<String> allAdvancements;

    // Player progression state: playerId -> (advId -> Set<grantedCriteria>)
    private final Map<UUID, Map<String, Set<String>>> playerCriteria;
    // Player unlocked advancements: playerId -> Set<advId>
    private final Map<UUID, Set<String>> playerUnlocked;

    /**
     * Constructs a new advancement criteria graph.
     */
    public AdvancementCriteriaGraph() {
        this.parentToChildren = new ConcurrentHashMap<>();
        this.childToParent = new ConcurrentHashMap<>();
        this.advancementCriteria = new ConcurrentHashMap<>();
        this.criterionTriggers = new ConcurrentHashMap<>();
        this.triggerToAdvancements = new ConcurrentHashMap<>();
        this.rootAdvancements = ConcurrentHashMap.newKeySet();
        this.allAdvancements = ConcurrentHashMap.newKeySet();

        this.playerCriteria = new ConcurrentHashMap<>();
        this.playerUnlocked = new ConcurrentHashMap<>();
    }

    /**
     * Registers an advancement into the DAG.
     *
     * @param advancementId canonical advancement ID (e.g., "minecraft:story/root")
     * @param parentId      parent advancement ID, or null if root
     * @param criteria      set of criteria names required to unlock
     * @param triggers      map of criterion name to trigger event type (e.g., "minecraft:inventory_changed")
     */
    public synchronized void registerAdvancement(
            String advancementId,
            String parentId,
            Set<String> criteria,
            Map<String, String> triggers) {
        Objects.requireNonNull(advancementId, "advancementId cannot be null");
        Objects.requireNonNull(criteria, "criteria cannot be null");

        allAdvancements.add(advancementId);
        advancementCriteria.put(advancementId, Set.copyOf(criteria));

        // Clean up previous parent reference if re-registering
        String oldParent = childToParent.get(advancementId);
        if (oldParent != null && !oldParent.equals(parentId)) {
            Set<String> oldChildren = parentToChildren.get(oldParent);
            if (oldChildren != null) {
                oldChildren.remove(advancementId);
            }
        }

        if (parentId == null || parentId.isBlank()) {
            rootAdvancements.add(advancementId);
            childToParent.remove(advancementId);
        } else {
            rootAdvancements.remove(advancementId);
            childToParent.put(advancementId, parentId);
            parentToChildren.computeIfAbsent(parentId, k -> ConcurrentHashMap.newKeySet()).add(advancementId);
        }

        Map<String, String> triggerMap = (triggers != null) ? Map.copyOf(triggers) : Collections.emptyMap();
        criterionTriggers.put(advancementId, triggerMap);

        for (String triggerType : triggerMap.values()) {
            if (triggerType != null && !triggerType.isBlank()) {
                triggerToAdvancements.computeIfAbsent(triggerType, k -> ConcurrentHashMap.newKeySet()).add(advancementId);
            }
        }
    }

    /**
     * Validates that the registered advancements form a valid Directed Acyclic Graph (DAG).
     *
     * @throws CyclicAdvancementException if a circular dependency cycle is detected
     */
    public void validateAcyclic() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        for (String node : allAdvancements) {
            if (!visited.contains(node)) {
                checkCycle(node, visited, recursionStack, currentPath);
            }
        }
    }

    private void checkCycle(
            String node,
            Set<String> visited,
            Set<String> recursionStack,
            List<String> currentPath) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);

        Set<String> children = parentToChildren.get(node);
        if (children != null) {
            for (String child : children) {
                if (!visited.contains(child)) {
                    checkCycle(child, visited, recursionStack, currentPath);
                } else if (recursionStack.contains(child)) {
                    // Precise cycle extraction: only include nodes participating in the cycle
                    int cycleStartIndex = currentPath.indexOf(child);
                    List<String> cycle = (cycleStartIndex >= 0)
                            ? new ArrayList<>(currentPath.subList(cycleStartIndex, currentPath.size()))
                            : new ArrayList<>(currentPath);
                    cycle.add(child);
                    throw new CyclicAdvancementException(
                            "Circular dependency detected in advancement graph: " + String.join(" -> ", cycle),
                            cycle);
                }
            }
        }

        recursionStack.remove(node);
        currentPath.remove(currentPath.size() - 1);
    }

    /**
     * Computes the topological evaluation order of the advancements.
     *
     * @return ordered list of advancement IDs (roots first)
     */
    public List<String> getTopologicalOrder() {
        validateAcyclic();

        Map<String, Integer> inDegree = new HashMap<>();
        for (String adv : allAdvancements) {
            inDegree.put(adv, 0);
        }

        for (String adv : allAdvancements) {
            Set<String> children = parentToChildren.get(adv);
            if (children != null) {
                for (String child : children) {
                    inDegree.put(child, inDegree.getOrDefault(child, 0) + 1);
                }
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> order = new ArrayList<>(allAdvancements.size());
        while (!queue.isEmpty()) {
            String u = queue.poll();
            order.add(u);

            Set<String> children = parentToChildren.get(u);
            if (children != null) {
                for (String child : children) {
                    int degree = inDegree.get(child) - 1;
                    inDegree.put(child, degree);
                    if (degree == 0) {
                        queue.add(child);
                    }
                }
            }
        }
        return order;
    }

    /**
     * Returns the subset of advancements currently listening to the given trigger
     * that are eligible for evaluation for the given player.
     * <p>
     * An advancement is eligible if and only if:
     * 1. It is not already unlocked by the player.
     * 2. Its parent advancement is already unlocked by the player (or it is a root advancement).
     * 3. It still has at least one locked criterion listening to the specified trigger event.
     *
     * @param playerId    player UUID
     * @param triggerType trigger event type (e.g., "minecraft:inventory_changed")
     * @return set of active eligible advancement IDs
     */
    public Set<String> getActiveAdvancementsForTrigger(UUID playerId, String triggerType) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(triggerType, "triggerType cannot be null");

        Set<String> candidates = triggerToAdvancements.get(triggerType);
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> unlocked = playerUnlocked.getOrDefault(playerId, Collections.emptySet());
        Set<String> active = new HashSet<>();

        for (String advId : candidates) {
            if (unlocked.contains(advId)) {
                continue; // Already unlocked
            }

            String parentId = childToParent.get(advId);
            if (parentId == null || unlocked.contains(parentId)) {
                // Check if advancement still has uncompleted criteria matching this trigger
                Map<String, String> triggers = criterionTriggers.get(advId);
                if (triggers != null && !triggers.isEmpty()) {
                    Set<String> granted = getCompletedCriteria(playerId, advId);
                    boolean hasPendingMatchingCriterion = false;
                    for (Map.Entry<String, String> entry : triggers.entrySet()) {
                        if (triggerType.equals(entry.getValue()) && !granted.contains(entry.getKey())) {
                            hasPendingMatchingCriterion = true;
                            break;
                        }
                    }
                    if (!hasPendingMatchingCriterion) {
                        continue; // Pruned: all criteria for this trigger are already completed
                    }
                }

                active.add(advId);
            }
        }
        return active;
    }

    /**
     * Grants a criterion to a player for a specific advancement.
     *
     * @param playerId      player UUID
     * @param advancementId advancement ID
     * @param criterion     criterion name
     * @return true if this grant resulted in the advancement becoming fully unlocked
     */
    public boolean grantCriterion(UUID playerId, String advancementId, String criterion) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(advancementId, "advancementId cannot be null");
        Objects.requireNonNull(criterion, "criterion cannot be null");

        Set<String> unlocked = playerUnlocked.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        if (unlocked.contains(advancementId)) {
            return false; // Already unlocked
        }

        // Verify parent is unlocked if present
        String parentId = childToParent.get(advancementId);
        if (parentId != null && !unlocked.contains(parentId)) {
            return false; // Parent must be unlocked before progressing
        }

        Set<String> requiredCriteria = advancementCriteria.get(advancementId);
        if (requiredCriteria == null || !requiredCriteria.contains(criterion)) {
            return false; // Invalid or unregistered criterion
        }

        Map<String, Set<String>> advMap = playerCriteria.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Set<String> granted = advMap.computeIfAbsent(advancementId, k -> ConcurrentHashMap.newKeySet());
        granted.add(criterion);

        // Check completion condition
        if (granted.containsAll(requiredCriteria)) {
            unlocked.add(advancementId);
            return true;
        }
        return false;
    }

    /**
     * Directly marks an advancement as unlocked for a player.
     *
     * @param playerId      player UUID
     * @param advancementId advancement ID
     * @return true if successfully transitioned to unlocked state
     */
    public boolean unlockAdvancement(UUID playerId, String advancementId) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(advancementId, "advancementId cannot be null");
        if (!allAdvancements.contains(advancementId)) {
            return false;
        }
        Set<String> unlocked = playerUnlocked.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        return unlocked.add(advancementId);
    }

    /**
     * Tests if an advancement is fully unlocked for a player.
     *
     * @param playerId      player UUID
     * @param advancementId advancement ID
     * @return true if unlocked
     */
    public boolean isAdvancementUnlocked(UUID playerId, String advancementId) {
        Set<String> unlocked = playerUnlocked.get(playerId);
        return unlocked != null && unlocked.contains(advancementId);
    }

    /**
     * Returns all completed criteria for a player on an advancement.
     *
     * @param playerId      player UUID
     * @param advancementId advancement ID
     * @return unmodifiable set of granted criteria names
     */
    public Set<String> getCompletedCriteria(UUID playerId, String advancementId) {
        Map<String, Set<String>> advMap = playerCriteria.get(playerId);
        if (advMap == null) {
            return Collections.emptySet();
        }
        Set<String> granted = advMap.get(advancementId);
        return (granted != null) ? Collections.unmodifiableSet(granted) : Collections.emptySet();
    }

    /**
     * Returns advancement completion progress ratio from 0.0 to 1.0.
     *
     * @param playerId      player UUID
     * @param advancementId advancement ID
     * @return progress percentage between 0.0f and 1.0f
     */
    public float getProgress(UUID playerId, String advancementId) {
        if (isAdvancementUnlocked(playerId, advancementId)) {
            return 1.0f;
        }
        Set<String> required = advancementCriteria.get(advancementId);
        if (required == null || required.isEmpty()) {
            return 0.0f;
        }
        Set<String> granted = getCompletedCriteria(playerId, advancementId);
        return (float) granted.size() / (float) required.size();
    }

    /**
     * Clears all progress and unlocked advancements for a player.
     *
     * @param playerId player UUID
     */
    public void resetPlayer(UUID playerId) {
        playerCriteria.remove(playerId);
        playerUnlocked.remove(playerId);
    }

    /**
     * Total number of registered advancements.
     *
     * @return count
     */
    public int getAdvancementCount() {
        return allAdvancements.size();
    }
}
