package dev.vessel.kernel.entity.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UniversalEntityAiGoalMediator} and {@link CopyOnWriteMobGoalList}
 * under ERR-45 and EAI-0001..0800.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
class UniversalEntityAiGoalMediatorTest {

    @Test
    void testCopyOnWriteListThreadSafetyUnderConcurrentMutation() throws InterruptedException {
        CopyOnWriteMobGoalList list = new CopyOnWriteMobGoalList();
        int iterations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean threwCme = new AtomicBoolean(false);

        // Reader thread: loops over goals continuously
        executor.submit(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    list.forEachGoal(goal -> {
                        assertNotNull(goal.getName());
                    });
                }
            } catch (Exception ex) {
                threwCme.set(true);
            } finally {
                latch.countDown();
            }
        });

        // Writer thread: rapidly adds and removes goals
        executor.submit(() -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    MobGoalEntry entry = new MobGoalEntry(
                            UUID.randomUUID(), "Goal-" + i, i % 10,
                            MobGoalActionType.MOVE, MobGoalSource.PAPER_BUKKIT, null);
                    list.add(entry);
                    if (i % 2 == 0) {
                        list.remove(entry.getGoalId());
                    }
                }
            } catch (Exception ex) {
                threwCme.set(true);
            } finally {
                latch.countDown();
            }
        });

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(finished, "Concurrent execution timed out");
        assertFalse(threwCme.get(), "ConcurrentModificationException was thrown during iteration");
    }

    @Test
    void testGoalPriorityArbitrationBetweenPaperAndForge() {
        UniversalEntityAiGoalMediator mediator = new UniversalEntityAiGoalMediator();
        UUID zombieUuid = UUID.randomUUID();

        AtomicInteger paperExecutions = new AtomicInteger(0);
        AtomicInteger forgeExecutions = new AtomicInteger(0);

        // Paper registers a priority 1 MOVE goal (higher priority)
        MobGoalEntry paperGoal = mediator.registerPaperGoal(
                zombieUuid,
                "PaperFleeSun",
                1,
                MobGoalActionType.MOVE,
                paperExecutions::incrementAndGet
        );

        // Forge registers a priority 5 MOVE goal (lower priority)
        MobGoalEntry forgeGoal = mediator.registerForgeBehavior(
                zombieUuid,
                "ForgeWander",
                5,
                MobGoalActionType.MOVE,
                forgeExecutions::incrementAndGet
        );

        // Tick entity AI
        mediator.tickEntityAi(zombieUuid);

        // Paper goal must run, Forge goal must be paused due to MOVE domain collision
        assertEquals(MobGoalExecutionState.RUNNING, paperGoal.getExecutionState());
        assertEquals(MobGoalExecutionState.PAUSED, forgeGoal.getExecutionState());
        assertEquals(1, paperExecutions.get());
        assertEquals(0, forgeExecutions.get());
    }

    @Test
    void testIndependentActionDomainsExecuteConcurrently() {
        UniversalEntityAiGoalMediator mediator = new UniversalEntityAiGoalMediator();
        UUID skeletonUuid = UUID.randomUUID();

        AtomicInteger moveExecutions = new AtomicInteger(0);
        AtomicInteger targetExecutions = new AtomicInteger(0);

        MobGoalEntry moveGoal = mediator.registerPaperGoal(
                skeletonUuid, "Stroll", 2, MobGoalActionType.MOVE, moveExecutions::incrementAndGet);

        MobGoalEntry targetGoal = mediator.registerForgeBehavior(
                skeletonUuid, "AttackNearestPlayer", 1, MobGoalActionType.TARGET, targetExecutions::incrementAndGet);

        mediator.tickEntityAi(skeletonUuid);

        // Both distinct domains should execute without blocking each other
        assertEquals(MobGoalExecutionState.RUNNING, moveGoal.getExecutionState());
        assertEquals(MobGoalExecutionState.RUNNING, targetGoal.getExecutionState());
        assertEquals(1, moveExecutions.get());
        assertEquals(1, targetExecutions.get());
    }

    @Test
    void testEntityUnregisterCleansState() {
        UniversalEntityAiGoalMediator mediator = new UniversalEntityAiGoalMediator();
        UUID mobId = UUID.randomUUID();

        mediator.registerPaperGoal(mobId, "Swim", 0, MobGoalActionType.SWIM, null);
        assertEquals(1, mediator.getManagedEntityCount());

        mediator.unregisterEntity(mobId);
        assertEquals(0, mediator.getManagedEntityCount());
    }
}
