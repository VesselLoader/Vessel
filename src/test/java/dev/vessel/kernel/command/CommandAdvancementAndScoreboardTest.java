package dev.vessel.kernel.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link AdvancementJsonAtomicWriter}, {@link ScoreboardThreadSafeMirror},
 * and {@link BossBarMultithreadedFlickerGuard} under ERR-93, ERR-94, ERR-95, ENC-0001..0400.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
class CommandAdvancementAndScoreboardTest {

    @Test
    void testAdvancementAtomicWriteAndBackup(@TempDir Path tempDir) throws IOException {
        AdvancementJsonAtomicWriter writer = new AdvancementJsonAtomicWriter();
        UUID playerUuid = UUID.randomUUID();
        Path targetPath = tempDir.resolve("advancements").resolve(playerUuid + ".json");

        String initialJson = "{\"minecraft:story/root\":{\"done\":true}}";
        writer.writeAdvancementJson(targetPath, initialJson, false);

        assertTrue(Files.exists(targetPath));
        assertEquals(initialJson, Files.readString(targetPath));

        // Second write with backup enabled
        String updatedJson = "{\"minecraft:story/root\":{\"done\":true},\"minecraft:story/mine_stone\":{\"done\":true}}";
        writer.writeAdvancementJson(targetPath, updatedJson, true);

        assertEquals(updatedJson, Files.readString(targetPath));

        Path backupPath = tempDir.resolve("advancements").resolve(playerUuid + ".json.bak");
        assertTrue(Files.exists(backupPath));
        assertEquals(initialJson, Files.readString(backupPath));
    }

    @Test
    void testScoreboardThreadSafeMirrorMultiThreadedAccess() throws InterruptedException {
        ScoreboardThreadSafeMirror mirror = new ScoreboardThreadSafeMirror();

        ScoreboardObjectiveSnapshot objective = new ScoreboardObjectiveSnapshot(
                "kills", "Player Kills", "dummy", "integer");
        mirror.putObjective(objective);

        ScoreboardTeamSnapshot redTeam = new ScoreboardTeamSnapshot(
                "red", "Red Team", "[Red] ", "", "red", true, true, Set.of("Player1", "Player2"));
        mirror.putTeam(redTeam);

        int threadCount = 4;
        int updatesPerThread = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < updatesPerThread; i++) {
                        String player = "Player" + (threadId * 10 + (i % 5));
                        mirror.setScore(player, "kills", i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(done);
        Map<String, Integer> scores = mirror.getScoresForObjective("kills");
        assertNotNull(scores);
        assertFalse(scores.isEmpty());

        assertEquals(1, mirror.getObjectives().size());
        assertEquals(1, mirror.getTeams().size());

        mirror.removeObjective("kills");
        assertTrue(mirror.getScoresForObjective("kills").isEmpty());
    }

    @Test
    void testBossBarMultithreadedFlickerGuardThrottling() {
        // Guard with 100ms throttle window
        BossBarMultithreadedFlickerGuard guard = new BossBarMultithreadedFlickerGuard(100_000_000L);
        UUID bossUuid = UUID.randomUUID();

        BossBarSnapshot initial = guard.registerBar(
                bossUuid, "Ender Dragon", 1.0f, BossBarColor.PURPLE, BossBarOverlay.PROGRESS);
        assertNotNull(initial);
        assertEquals(1, guard.getActiveBarCount());

        // Immediate next update within throttle window should be coalesced (return false)
        boolean immediateUpdate = guard.updateProgress(bossUuid, 0.95f);
        assertFalse(immediateUpdate, "Update within throttle window should be coalesced to prevent flicker");

        // Lethal/terminal update (progress <= 0.0) must bypass throttle and trigger immediate broadcast
        boolean deathUpdate = guard.updateProgress(bossUuid, 0.0f);
        assertTrue(deathUpdate, "Boss death update should bypass throttle window immediately");

        // Verify title change and snapshot consistency
        guard.updateTitle(bossUuid, "Enraged Ender Dragon");
        Optional<BossBarSnapshot> snap = guard.getSnapshot(bossUuid);
        assertTrue(snap.isPresent());
        assertEquals("Enraged Ender Dragon", snap.get().title());
        assertEquals(0.0f, snap.get().progress());

        guard.removeBar(bossUuid);
        assertEquals(0, guard.getActiveBarCount());
    }
}
