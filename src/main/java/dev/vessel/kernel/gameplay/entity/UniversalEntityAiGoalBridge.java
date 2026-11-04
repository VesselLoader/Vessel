package dev.vessel.kernel.gameplay.entity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Universal thread-safe bridge connecting modded AI goals (GoalSelector, Citizens Sentinel)
 * with modern Brain and Behavior systems.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class UniversalEntityAiGoalBridge {

    private final ConcurrentMap<UUID, List<String>> activeEntityGoals = new ConcurrentHashMap<>();

    public void registerEntityGoal(UUID entityUuid, int priority, String goalIdentifier) {
        Objects.requireNonNull(entityUuid, "entityUuid cannot be null");
        Objects.requireNonNull(goalIdentifier, "goalIdentifier cannot be null");
        activeEntityGoals.computeIfAbsent(entityUuid, k -> new CopyOnWriteArrayList<>()).add(priority + ":" + goalIdentifier);
    }

    public List<String> getEntityGoals(UUID entityUuid) {
        if (entityUuid == null) {
            return List.of();
        }
        List<String> list = activeEntityGoals.get(entityUuid);
        return list != null ? List.copyOf(list) : List.of();
    }

    public boolean hasActiveGoals(UUID entityUuid) {
        return entityUuid != null && activeEntityGoals.containsKey(entityUuid);
    }

    public void clearEntityGoals(UUID entityUuid) {
        if (entityUuid != null) {
            activeEntityGoals.remove(entityUuid);
        }
    }
}
