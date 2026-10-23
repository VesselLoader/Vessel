package dev.vessel.kernel.optimization.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.10: all 10 world event batching, natural spawner, and
 * memory sanitizer engines in {@code dev.vessel.kernel.optimization.world}.
 *
 * @version 1.0.1
 */
class WorldEventAndMemorySanitizerTest {

    @Test
    void testUltimineItemXpMergeSeasonalBatcherDynamicLightAdvancementsAndSoundCuller() {
        ItemAndXpOrbSpatialMergeAccelerator mergeEngine =
                new ItemAndXpOrbSpatialMergeAccelerator();
        assertEquals(
                500,
                mergeEngine.coalesceItemDropBeforeSpawn(100L, "minecraft:cobblestone", 500, 65536));
        assertEquals(
                1500,
                mergeEngine.coalesceItemDropBeforeSpawn(100L, "minecraft:cobblestone", 1000, 65536));
        assertEquals(64, mergeEngine.extractPickupStack(100L, "minecraft:cobblestone", 64));
        assertEquals(100L, mergeEngine.mergeExperienceOrbInCell(200L, 100));
        assertEquals(150L, mergeEngine.mergeExperienceOrbInCell(200L, 50));

        SeasonalWeatherAndRandomTickBatcher seasonalBatcher =
                new SeasonalWeatherAndRandomTickBatcher();
        assertTrue(seasonalBatcher.queueSeasonalBlockChange(55L, (short) 10, 80, 2));
        assertTrue(seasonalBatcher.queueSeasonalBlockChange(55L, (short) 11, 81, 2));
        assertFalse(seasonalBatcher.queueSeasonalBlockChange(55L, (short) 12, 82, 2));
        Map<Short, Integer> flushed = seasonalBatcher.flushSectionMultiBlockPacket(55L);
        assertEquals(2, flushed.size());
        assertTrue(seasonalBatcher.shouldSuppressVisualNeighborUpdate("serene_seasons:snow_melt"));

        DynamicLightAndFlareUpdateSuppressor lightSuppressor =
                new DynamicLightAndFlareUpdateSuppressor();
        assertTrue(lightSuppressor.updateEphemeralLightSource(1L, 900L, 14, 100L, 10));
        assertFalse(lightSuppressor.updateEphemeralLightSource(1L, 901L, 14, 105L, 10));
        assertEquals(14, lightSuppressor.getEphemeralLightLevel(900L));
        assertTrue(lightSuppressor.updateEphemeralLightSource(1L, 901L, 14, 115L, 10));
        assertEquals(0, lightSuppressor.getEphemeralLightLevel(900L));
        assertEquals(14, lightSuppressor.getEphemeralLightLevel(901L));

        IndexedAdvancementTriggerAccelerator advAccelerator =
                new IndexedAdvancementTriggerAccelerator();
        advAccelerator.registerItemCriterion("atm9:obtain_allthemodium", 777);
        UUID playerUuid = UUID.randomUUID();
        List<String> unlocked = advAccelerator.evaluateDirtyInventorySlotOnly(playerUuid, 4, 777);
        assertEquals(List.of("atm9:obtain_allthemodium"), unlocked);
        assertTrue(advAccelerator.evaluateDirtyInventorySlotOnly(playerUuid, 4, 777).isEmpty());

        SpatialSoundAndParticleAcousticCuller soundCuller =
                new SpatialSoundAndParticleAcousticCuller();
        assertTrue(soundCuller.shouldBroadcastMachineSound(10, 64, 10, 5, 100.0, 4096.0, 2));
        assertTrue(soundCuller.shouldBroadcastMachineSound(11, 64, 11, 5, 100.0, 4096.0, 2));
        assertFalse(soundCuller.shouldBroadcastMachineSound(12, 64, 12, 5, 100.0, 4096.0, 2));
        soundCuller.resetAcousticWindow();
        assertTrue(soundCuller.shouldBroadcastMachineSound(12, 64, 12, 5, 100.0, 4096.0, 2));
    }

    @Test
    void testByteBufLeakGuardScoreboardSanitizerAndNpcQuotaScheduler() {
        DirectByteBufAndEventBusLeakGuard leakGuard = new DirectByteBufAndEventBusLeakGuard();
        AtomicInteger refCnt = new AtomicInteger(2);
        leakGuard.executeWithByteBufReleaseGuard(
                () -> {},
                refCnt::get,
                () -> refCnt.decrementAndGet());
        assertEquals(0, refCnt.get());

        AtomicBoolean removedFlag = new AtomicBoolean(false);
        Object dummyEntity = new Object();
        leakGuard.registerWeakEntityEventListener(dummyEntity, removedFlag::get, () -> {});
        assertEquals(0, leakGuard.evictRemovedEntityListeners());
        removedFlag.set(true);
        assertEquals(1, leakGuard.evictRemovedEntityListeners());

        UUID fake1 = leakGuard.acquirePooledFakePlayer("minecraft:overworld", "[Create_Deployer]");
        UUID fake2 = leakGuard.acquirePooledFakePlayer("minecraft:overworld", "[Create_Deployer]");
        assertEquals(fake1, fake2);

        ScoreboardTeamAndMultipartEntitySanitizer sanitizer =
                new ScoreboardTeamAndMultipartEntitySanitizer();
        UUID aliveBoss = UUID.randomUUID();
        UUID deadBoss = UUID.randomUUID();
        sanitizer.registerMobScoreboardTeam("apotheosis_01", aliveBoss);
        sanitizer.registerMobScoreboardTeam("apotheosis_02", deadBoss);
        assertEquals(1, sanitizer.purgeOrphanedMobTeams(id -> id.equals(aliveBoss)));

        sanitizer.registerMultipartSubEntities(aliveBoss, 333L, new int[]{101, 102, 103});
        assertEquals(3, sanitizer.cleanupUnloadedMultipartHitboxes(333L));

        AutonomousNpcWorkerQuotaScheduler npcScheduler = new AutonomousNpcWorkerQuotaScheduler();
        AtomicInteger jobsRun = new AtomicInteger(0);
        npcScheduler.enqueueNpcJobEvaluation(1, 10L, jobsRun::incrementAndGet);
        npcScheduler.enqueueNpcJobEvaluation(1, 11L, jobsRun::incrementAndGet);
        npcScheduler.enqueueNpcJobEvaluation(1, 12L, jobsRun::incrementAndGet);
        assertEquals(2, npcScheduler.processColonyTickQuota(1, 2));
        assertEquals(1, npcScheduler.processColonyTickQuota(1, 2));
        assertEquals(3, jobsRun.get());
    }

    @Test
    void testNaturalSpawnerGovernorAndInstancedLootWorldMapGovernor() {
        NaturalSpawnerAndModdedCreatureGovernor spawnerGov =
                new NaturalSpawnerAndModdedCreatureGovernor();
        UUID playerUuid = UUID.randomUUID();
        assertTrue(spawnerGov.enforcePerPlayerMobCap(playerUuid, "MONSTER", 40, 70));
        assertFalse(spawnerGov.enforcePerPlayerMobCap(playerUuid, "MONSTER", 70, 70));

        long[] mask = new long[64];
        mask[0] = 1L;
        spawnerGov.updateSpawnableSurfaceBitSet(10L, mask);
        assertTrue(spawnerGov.isSurfaceSpawnValid(10L, 0, 0, 0));
        assertFalse(spawnerGov.isSurfaceSpawnValid(10L, 1, 0, 0));
        assertTrue(spawnerGov.shouldThrottleModdedCreatureSpawner("cobblemon", playerUuid, 0.85, 13L));
        assertFalse(spawnerGov.shouldThrottleModdedCreatureSpawner("cobblemon", playerUuid, 0.85, 20L));

        InstancedLootAndWorldMapSyncGovernor lootGov =
                new InstancedLootAndWorldMapSyncGovernor();
        lootGov.markPlayerOpenedInstancedContainer(777L, playerUuid);
        byte[] sampleNbt = "LootrSampleInventoryDataPayload".repeat(8).getBytes(StandardCharsets.UTF_8);
        assertNull(lootGov.evictIdleInstancedLootToZstdCache(777L, sampleNbt, 100L, 5000L));
        byte[] compressed = lootGov.evictIdleInstancedLootToZstdCache(777L, sampleNbt, 100L, 20000L);
        assertNotNull(compressed);
        assertTrue(compressed.length < sampleNbt.length);

        assertEquals(64, lootGov.throttleDistantHorizonsLodStream(playerUuid, 500, 262144));
        assertTrue(lootGov.shouldAllowWorldMapRadarScan(playerUuid, 100L, 20));
        assertFalse(lootGov.shouldAllowWorldMapRadarScan(playerUuid, 110L, 20));
        assertTrue(lootGov.shouldAllowWorldMapRadarScan(playerUuid, 125L, 20));
    }
}
