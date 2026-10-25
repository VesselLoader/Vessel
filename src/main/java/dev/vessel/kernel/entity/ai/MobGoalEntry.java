package dev.vessel.kernel.entity.ai;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable or thread-safe descriptor representing a single entity AI goal across
 * Paper MobGoals and Forge GoalSelector.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class MobGoalEntry implements Comparable<MobGoalEntry> {

    private final UUID goalId;
    private final String name;
    private final int priority;
    private final MobGoalActionType actionType;
    private final MobGoalSource source;
    private final Runnable taskRunnable;
    private volatile MobGoalExecutionState executionState;

    /**
     * Constructs a new MobGoalEntry.
     *
     * @param goalId       unique goal identifier
     * @param name         descriptive name
     * @param priority     priority level (lower integer means higher priority)
     * @param actionType   behavioral category
     * @param source       originating subsystem
     * @param taskRunnable tick execution action (may be null if handled externally)
     */
    public MobGoalEntry(
            UUID goalId,
            String name,
            int priority,
            MobGoalActionType actionType,
            MobGoalSource source,
            Runnable taskRunnable
    ) {
        this.goalId = Objects.requireNonNull(goalId, "goalId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.priority = priority;
        this.actionType = Objects.requireNonNull(actionType, "actionType cannot be null");
        this.source = Objects.requireNonNull(source, "source cannot be null");
        this.taskRunnable = taskRunnable;
        this.executionState = MobGoalExecutionState.INACTIVE;
    }

    /**
     * Returns the unique UUID of this goal entry.
     *
     * @return the goal UUID
     */
    public UUID getGoalId() {
        return goalId;
    }

    /**
     * Returns the descriptive name of the goal.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the integer priority.
     *
     * @return priority level
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Returns the action type category.
     *
     * @return action type
     */
    public MobGoalActionType getActionType() {
        return actionType;
    }

    /**
     * Returns the source subsystem.
     *
     * @return goal source
     */
    public MobGoalSource getSource() {
        return source;
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return execution state
     */
    public MobGoalExecutionState getExecutionState() {
        return executionState;
    }

    /**
     * Updates the execution state safely.
     *
     * @param newState new state
     */
    public void setExecutionState(MobGoalExecutionState newState) {
        this.executionState = Objects.requireNonNull(newState, "newState cannot be null");
    }

    /**
     * Executes the task if runnable is present and state is RUNNING.
     */
    public void executeTick() {
        if (executionState == MobGoalExecutionState.RUNNING && taskRunnable != null) {
            taskRunnable.run();
        }
    }

    @Override
    public int compareTo(MobGoalEntry other) {
        int cmp = Integer.compare(this.priority, other.priority);
        if (cmp != 0) {
            return cmp;
        }
        return this.name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MobGoalEntry that = (MobGoalEntry) o;
        return Objects.equals(goalId, that.goalId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(goalId);
    }

    @Override
    public String toString() {
        return "MobGoalEntry[" + name + ", priority=" + priority + ", type=" + actionType + ", state=" + executionState + "]";
    }
}
