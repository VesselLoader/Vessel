package dev.vessel.kernel.optimization.world;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Enforces a deterministic Round-Robin Time-Slicing quota for autonomous modded NPC
 * workers (MineColonies Citizens, Touhou Little Maid, Ars Nouveau Starbuncles, Rats),
 * preventing a single 100-NPC colony from exhausting the server tick budget.
 *
 * @version 1.0.0
 */
public final class AutonomousNpcWorkerQuotaScheduler {

    private final Map<Integer, Queue<Runnable>> colonyJobQueues = new ConcurrentHashMap<>();

    /**
     * Creates a new autonomous NPC worker quota scheduler.
     */
    public AutonomousNpcWorkerQuotaScheduler() {
    }

    /**
     * Enqueues an AI job/crafting/logistics evaluation task for an NPC belonging to
     * {@code colonyId}.
     *
     * @param colonyId          numeric colony or owner group identifier
     * @param npcEntityId       numeric NPC entity identifier
     * @param jobEvaluationTask runnable performing the NPC's job evaluation
     */
    public void enqueueNpcJobEvaluation(
            int colonyId, long npcEntityId, Runnable jobEvaluationTask) {
        Objects.requireNonNull(jobEvaluationTask, "jobEvaluationTask must not be null");
        Queue<Runnable> queue =
                colonyJobQueues.computeIfAbsent(
                        colonyId, k -> new ConcurrentLinkedQueue<>());
        queue.add(jobEvaluationTask);
    }

    /**
     * Executes up to {@code maxEvaluationsPerTick} queued NPC job tasks for {@code colonyId},
     * deferring any remaining tasks to subsequent ticks.
     *
     * @param colonyId              numeric colony identifier
     * @param maxEvaluationsPerTick maximum NPC job evaluations allowed per tick (e.g., 5)
     * @return number of NPC job evaluations executed on this tick
     */
    public int processColonyTickQuota(int colonyId, int maxEvaluationsPerTick) {
        if (maxEvaluationsPerTick <= 0) {
            return 0;
        }
        Queue<Runnable> queue = colonyJobQueues.get(colonyId);
        if (queue == null || queue.isEmpty()) {
            return 0;
        }
        int executed = 0;
        while (executed < maxEvaluationsPerTick) {
            Runnable task = queue.poll();
            if (task == null) {
                break;
            }
            task.run();
            executed++;
        }
        return executed;
    }
}
