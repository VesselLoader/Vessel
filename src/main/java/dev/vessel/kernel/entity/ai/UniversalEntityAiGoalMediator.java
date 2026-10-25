package dev.vessel.kernel.entity.ai;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sovereign thread-safe mediator orchestrating entity AI goals between Paper MobGoals
 * and Forge/NeoForge Brain, Behaviors, and GoalSelectors under Decision 45 and EAI-0001..0400.
 *
 * <p>Harmonizes behavioral control conflicts across loaders, arbitrates action type exclusivity
 * (e.g. movement, targeting), and prevents race conditions on multi-threaded entity ticking
 * environments such as Folia regions.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class UniversalEntityAiGoalMediator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalEntityAiGoalMediator.class);

    private final Map<UUID, CopyOnWriteMobGoalList> actionGoalsByEntity = new ConcurrentHashMap<>();
    private final Map<UUID, CopyOnWriteMobGoalList> targetGoalsByEntity = new ConcurrentHashMap<>();

    /**
     * Constructs a new Universal Entity AI Goal Mediator.
     */
    public UniversalEntityAiGoalMediator() {
    }

    /**
     * Obtains or creates the action goals container for the given entity.
     *
     * @param entityId entity unique identifier
     * @return non-null CopyOnWriteMobGoalList
     */
    public CopyOnWriteMobGoalList getOrCreateActionGoals(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        return actionGoalsByEntity.computeIfAbsent(entityId, k -> new CopyOnWriteMobGoalList());
    }

    /**
     * Obtains or creates the target pursuit goals container for the given entity.
     *
     * @param entityId entity unique identifier
     * @return non-null CopyOnWriteMobGoalList
     */
    public CopyOnWriteMobGoalList getOrCreateTargetGoals(UUID entityId) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        return targetGoalsByEntity.computeIfAbsent(entityId, k -> new CopyOnWriteMobGoalList());
    }

    /**
     * Registers a Paper MobGoal onto the specified entity.
     *
     * @param entityId     target entity UUID
     * @param name         unique descriptive goal key
     * @param priority     priority index (lower is higher priority)
     * @param actionType   behavior category
     * @param taskRunnable tick execution task
     * @return registered MobGoalEntry
     */
    public MobGoalEntry registerPaperGoal(
            UUID entityId,
            String name,
            int priority,
            MobGoalActionType actionType,
            Runnable taskRunnable
    ) {
        return registerGoal(entityId, name, priority, actionType, MobGoalSource.PAPER_BUKKIT, taskRunnable);
    }

    /**
     * Registers a Forge/NeoForge Goal/Behavior onto the specified entity.
     *
     * @param entityId     target entity UUID
     * @param name         unique descriptive goal key
     * @param priority     priority index
     * @param actionType   behavior category
     * @param taskRunnable tick execution task
     * @return registered MobGoalEntry
     */
    public MobGoalEntry registerForgeBehavior(
            UUID entityId,
            String name,
            int priority,
            MobGoalActionType actionType,
            Runnable taskRunnable
    ) {
        return registerGoal(entityId, name, priority, actionType, MobGoalSource.FORGE_NEOFORGE, taskRunnable);
    }

    /**
     * Unified registration of an entity goal from any supported source.
     *
     * @param entityId     entity UUID
     * @param name         goal name
     * @param priority     priority level
     * @param actionType   action classification
     * @param source       originating subsystem
     * @param taskRunnable execution body
     * @return constructed entry
     */
    public MobGoalEntry registerGoal(
            UUID entityId,
            String name,
            int priority,
            MobGoalActionType actionType,
            MobGoalSource source,
            Runnable taskRunnable
    ) {
        Objects.requireNonNull(entityId, "entityId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(actionType, "actionType cannot be null");
        Objects.requireNonNull(source, "source cannot be null");

        MobGoalEntry entry = new MobGoalEntry(
                UUID.randomUUID(),
                name,
                priority,
                actionType,
                source,
                taskRunnable
        );

        CopyOnWriteMobGoalList list = (actionType == MobGoalActionType.TARGET)
                ? getOrCreateTargetGoals(entityId)
                : getOrCreateActionGoals(entityId);

        list.add(entry);
        LOGGER.debug("Registered AI Goal '{}' for entity {} from {}", name, entityId, source);
        return entry;
    }

    /**
     * Executes the AI tick loop for the specified entity, performing conflict arbitration
     * so that mutually exclusive goals of the same action type do not concurrently hijack entity controls.
     *
     * @param entityId entity to tick
     */
    public void tickEntityAi(UUID entityId) {
        if (entityId == null) {
            return;
        }

        CopyOnWriteMobGoalList actionList = actionGoalsByEntity.get(entityId);
        if (actionList != null && !actionList.isEmpty()) {
            arbitrateAndTickList(actionList);
        }

        CopyOnWriteMobGoalList targetList = targetGoalsByEntity.get(entityId);
        if (targetList != null && !targetList.isEmpty()) {
            arbitrateAndTickList(targetList);
        }
    }

    private void arbitrateAndTickList(CopyOnWriteMobGoalList list) {
        MobGoalEntry[] snapshot = list.getSnapshotArray();
        Set<MobGoalActionType> claimedActions = EnumSet.noneOf(MobGoalActionType.class);

        // Snapshot is pre-sorted by priority (lowest integer value first)
        for (MobGoalEntry entry : snapshot) {
            MobGoalActionType type = entry.getActionType();
            if (claimedActions.contains(type)) {
                // Higher-priority goal already claimed this action domain
                entry.setExecutionState(MobGoalExecutionState.PAUSED);
            } else {
                claimedActions.add(type);
                entry.setExecutionState(MobGoalExecutionState.RUNNING);
                try {
                    entry.executeTick();
                } catch (Exception ex) {
                    LOGGER.error("Uncaught exception while ticking AI goal '{}' ({})",
                            entry.getName(), entry.getSource(), ex);
                }
            }
        }
    }

    /**
     * Unregisters and cleans up all AI goals associated with a dead or despawned entity.
     *
     * @param entityId entity UUID to purge
     */
    public void unregisterEntity(UUID entityId) {
        if (entityId == null) {
            return;
        }
        actionGoalsByEntity.remove(entityId);
        targetGoalsByEntity.remove(entityId);
        LOGGER.debug("Cleaned up AI goals for entity {}", entityId);
    }

    /**
     * Returns the total number of managed entities with active AI mappings.
     *
     * @return active entity count
     */
    public int getManagedEntityCount() {
        return Math.max(actionGoalsByEntity.size(), targetGoalsByEntity.size());
    }
}
