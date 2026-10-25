package dev.vessel.kernel.optimization.supermod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.actor.primitive.RegionStrandRouter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.12: all 10 super-mod combat, ephemeral RAM worlds, Valkyrien Skies
 * physics sleep, Lua virtual FS, and NaN/static sanitizer engines in
 * {@code dev.vessel.kernel.optimization.supermod} (Part 2).
 *
 * @version 1.0.0
 */
class SuperModCombatWorldAndSanitizerTest {

    @Test
    void testUncappedArmorEphemeralRamWorldsLeakSentinelAndLuaStrandThrottler() {
        UniversalCombatAndArmorHarmonizer combatHarmonizer =
                new UniversalCombatAndArmorHarmonizer();
        float damageAt30 = combatHarmonizer.calculateUncappedArmorDamageReduction(100.0f, 30.0f, 10.0f);
        float damageAt100 = combatHarmonizer.calculateUncappedArmorDamageReduction(100.0f, 100.0f, 20.0f);
        assertTrue(damageAt100 < damageAt30);
        assertTrue(damageAt100 >= 4.0f);
        UUID defender = UUID.randomUUID();
        assertTrue(combatHarmonizer.bridgeModdedShieldBlockToBukkit(defender, 50.0f, 0.8f));
        assertEquals(40.0f, combatHarmonizer.getLastShieldAbsorbedDamage(defender), 1e-4f);

        EphemeralWorldInstanceEngine ramWorldEngine = new EphemeralWorldInstanceEngine();
        String uri = ramWorldEngine.cloneTemplateWorldInRam(
                "bedwars_solo",
                "bw_match_101",
                new byte[]{1, 2, 3, 4});
        assertEquals("vessel-ram://bedwars_solo/bw_match_101", uri);
        assertEquals(1, ramWorldEngine.getActiveEphemeralWorldCount());
        assertTrue(ramWorldEngine.destroyEphemeralWorldInRam("bw_match_101"));
        assertEquals(0, ramWorldEngine.getActiveEphemeralWorldCount());

        MemoryLeakSentinelWatchdog leakWatchdog = new MemoryLeakSentinelWatchdog();
        Object retainedWorld = new Object();
        leakWatchdog.trackUnloadedResource("twilightforest", retainedWorld);
        assertEquals(List.of("twilightforest"), leakWatchdog.scanForRetainedDeadReferences());

        LuaSandboxStrandThrottler luaThrottler = new LuaSandboxStrandThrottler();
        assertTrue(luaThrottler.consumeInstructionQuota(1, 6000, 10000));
        assertFalse(luaThrottler.consumeInstructionQuota(1, 5000, 10000));
        luaThrottler.resetTickQuotas();
        assertTrue(luaThrottler.consumeInstructionQuota(1, 5000, 10000));
    }

    @Test
    void testValkyrienBarrierNanSanitizerStaticMapSanitizerAndBlockSearchLimiter() {
        RegionStrandRouter router = new RegionStrandRouter();
        RegionalActorStrand strand = new RegionalActorStrand(new RegionCoordinateHash(0, 0));
        router.register(strand);

        ValkyrienPhysicsStrandBarrier vsBarrier = new ValkyrienPhysicsStrandBarrier();
        assertTrue(vsBarrier.synchronizeShipPhysicsFrame(77L, 32.5, 80.0, 64.2, router));
        assertFalse(vsBarrier.synchronizeShipPhysicsFrame(77L, 99999.0, 80.0, 99999.0, router));

        NanSanitizingDamageEngine nanEngine = new NanSanitizingDamageEngine();
        assertEquals(0.0f, nanEngine.sanitizeDamageValue(Float.NaN, 10000.0f), 1e-4f);
        assertEquals(10000.0f, nanEngine.sanitizeDamageValue(Float.POSITIVE_INFINITY, 10000.0f), 1e-4f);
        assertEquals(20.0f, nanEngine.sanitizeEntityHealth(Float.NaN, 20.0f), 1e-4f);
        assertEquals(0.0f, nanEngine.sanitizeEntityHealth(-50.0f, 20.0f), 1e-4f);

        StaticWorldReferenceSanitizer staticSanitizer = new StaticWorldReferenceSanitizer();
        Object unloadedLevel = new Object();
        Map<Object, Object> modStaticCache = new HashMap<>();
        modStaticCache.put(unloadedLevel, "cached_data");
        modStaticCache.put("player_key", unloadedLevel);
        modStaticCache.put("active_key", "active_value");
        assertEquals(2, staticSanitizer.purgeRetainedObjectFromStaticMap(modStaticCache, unloadedLevel));
        assertEquals(1, modStaticCache.size());

        ChunkBlockSearchRateLimiter searchLimiter = new ChunkBlockSearchRateLimiter();
        List<Long> candidates = List.of(10L, 20L, 30L, 40L, 50L);
        List<Long> found = searchLimiter.executeRateLimitedBlockSearch(
                candidates,
                pos -> (pos % 20L) == 0L,
                3);
        assertEquals(List.of(20L), found);
    }

    @Test
    void testValkyrienVoxelShipSleepGovernorAndLuaCoroutinePeripheralGovernor() {
        ValkyrienPhysicsAndVoxelShipCollisionGovernor shipGov =
                new ValkyrienPhysicsAndVoxelShipCollisionGovernor();
        assertTrue(shipGov.evaluateShipMicroVibrationSleep(501L, 1e-7, 1e-7, 25));
        assertEquals(0, shipGov.budgetShipVoxelCollisionSweeps(501L, 64, 16));
        shipGov.wakePhysicsShipOnInteraction(501L, 12345L);
        assertEquals(16, shipGov.budgetShipVoxelCollisionSweeps(501L, 64, 16));
        assertTrue(shipGov.coalesceShipTransformToRegionThread(
                501L, 100.0, 72.0, -50.0, 90.0f, 0.0f, 0.0f));

        LuaCoroutineAndPeripheralQuotaGovernor luaGov =
                new LuaCoroutineAndPeripheralQuotaGovernor();
        assertTrue(luaGov.trackComputerCpuSliceNanos(12, 150_000L, 250_000L));
        assertFalse(luaGov.shouldSoftYieldCoroutine(12));
        assertFalse(luaGov.trackComputerCpuSliceNanos(12, 150_000L, 250_000L));
        assertTrue(luaGov.shouldSoftYieldCoroutine(12));

        assertFalse(luaGov.shouldThrottlePeripheralInventoryScan(12, 900L, 100L));
        assertTrue(luaGov.shouldThrottlePeripheralInventoryScan(12, 900L, 102L));
        assertFalse(luaGov.shouldThrottlePeripheralInventoryScan(12, 900L, 105L));

        luaGov.bufferVirtualFsWriteBehind(
                12,
                "/startup.lua",
                "print('hello')".getBytes(StandardCharsets.UTF_8),
                100L);
        assertEquals(0, luaGov.flushDirtyVirtualFilesToDisk(400L));
        assertEquals(1, luaGov.flushDirtyVirtualFilesToDisk(705L));
    }
}
