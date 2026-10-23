package dev.vessel.adapter.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.migration.WorldDimensionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.7: {@link UniversalEntityTypeBridge},
 * {@link DataComponentPreservingMetaBridge}, {@link MultiDimensionWorldRegistryBridge},
 * {@link CustomDamageSourceHarmonizer}, {@link CrossRegionContraptionBridge},
 * {@link ClaimProtectionModInteractionBridge}, {@link CuriosAccessoriesInventoryBridge},
 * and {@link CustomPortalTeleportationBridge}.
 *
 * @version 1.0.0
 */
class HybridEntityClaimAndCuriosBridgeTest {

    @Test
    void testEntityTypeDataComponentDimensionAndDamageBridges() {
        UniversalEntityTypeBridge entityBridge = new UniversalEntityTypeBridge();
        String mappedType = entityBridge.mapModdedEntityToBukkitType("alexsmobs:grizzly_bear", true, false);
        assertEquals("MOD_CREATURE_ALEXSMOBS_GRIZZLY_BEAR", mappedType);
        assertTrue(entityBridge.isKnownModdedEntity("alexsmobs:grizzly_bear"));
        assertFalse(entityBridge.isKnownModdedEntity("alexsmobs:unknown"));

        DataComponentPreservingMetaBridge metaBridge = new DataComponentPreservingMetaBridge();
        Map<String, Object> original = Map.of(
                "mekanism:energy", 120000L,
                "apotheosis:sockets", 3);
        Map<String, Object> updated = metaBridge.applyBukkitMetaPreservingModComponents(
                original, "Atomic Disassembler [VIP]", List.of("Line 1", "Line 2"));
        assertEquals(120000L, updated.get("mekanism:energy"));
        assertEquals(3, updated.get("apotheosis:sockets"));
        assertEquals("Atomic Disassembler [VIP]", updated.get("minecraft:custom_name"));
        assertEquals(List.of("Line 1", "Line 2"), updated.get("minecraft:lore"));

        MultiDimensionWorldRegistryBridge worldBridge = new MultiDimensionWorldRegistryBridge();
        worldBridge.registerCustomModDimension("twilightforest:twilight_forest", WorldDimensionType.CUSTOM);
        assertTrue(worldBridge.getBukkitRegisteredWorlds().contains("twilightforest:twilight_forest"));
        assertEquals(
                WorldDimensionType.CUSTOM,
                worldBridge.getDimensionEnvironment("twilightforest:twilight_forest"));

        CustomDamageSourceHarmonizer damageHarmonizer = new CustomDamageSourceHarmonizer();
        assertEquals(
                "PROJECTILE",
                damageHarmonizer.mapModDamageSourceToBukkitCause("tacz:bullet", true, false, false));
        assertEquals(
                "FALLING_BLOCK",
                damageHarmonizer.mapModDamageSourceToBukkitCause("create:crush", false, false, false));
        assertTrue(damageHarmonizer
                .preserveCustomDeathMessage("Shiroz", "tacz:bullet", "SniperBot")
                .contains("[tacz:bullet]"));
    }

    @Test
    void testCrossRegionContraptionAndClaimProtectionBridge() {
        CrossRegionContraptionBridge contraptionBridge = new CrossRegionContraptionBridge();
        boolean crossedSameRegion = contraptionBridge.synchronizeContraptionBoundaryCrossing(77L, 0, 0, 5, 5);
        assertFalse(crossedSameRegion);
        boolean crossedNewRegion = contraptionBridge.synchronizeContraptionBoundaryCrossing(77L, 5, 5, 32, 32);
        assertTrue(crossedNewRegion);
        assertTrue(contraptionBridge.getActiveRegionOwnerHash(77L) != 0L);

        ClaimProtectionModInteractionBridge claimBridge = new ClaimProtectionModInteractionBridge();
        claimBridge.registerClaimPermissionHook((x, y, z) -> x < 100);

        UUID playerUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        assertTrue(claimBridge.canModWeaponOrDrillBreakBlock(playerUuid, 50, 64, 50, "create:mechanical_drill"));
        assertFalse(claimBridge.canModWeaponOrDrillBreakBlock(playerUuid, 150, 64, 50, "tacz:rpg_rocket"));
        assertFalse(claimBridge.canModProjectileDamageEntity(playerUuid, targetUuid, 150, 64, 50));
    }

    @Test
    void testCuriosAccessoriesLifecycleAndCustomPortalTeleportation() {
        CuriosAccessoriesInventoryBridge curiosBridge = new CuriosAccessoriesInventoryBridge();
        UUID playerUuid = UUID.randomUUID();
        curiosBridge.setAccessorySlotItem(playerUuid, "curios:back:0", "sophisticatedbackpacks:netherite_backpack");
        curiosBridge.setAccessorySlotItem(playerUuid, "curios:ring:0", "botania:mana_ring");

        Map<String, String> captured = curiosBridge.captureAccessoriesOnDeath(playerUuid, false);
        assertEquals(2, captured.size());
        assertTrue(curiosBridge.captureAccessoriesOnDeath(playerUuid, false).isEmpty());

        curiosBridge.restoreAccessoriesOnRespawn(playerUuid, captured);
        assertEquals(
                "botania:mana_ring",
                curiosBridge.captureAccessoriesOnDeath(playerUuid, true).get("curios:ring:0"));

        CustomPortalTeleportationBridge portalBridge = new CustomPortalTeleportationBridge();
        assertTrue(portalBridge.fireSafeModPortalTeleport(
                playerUuid,
                "minecraft:overworld",
                "twilightforest:twilight_forest",
                10.5,
                72.0,
                -20.25,
                false));
        assertFalse(portalBridge.fireSafeModPortalTeleport(
                playerUuid,
                "minecraft:overworld",
                "the_aether:the_aether",
                0.0,
                100.0,
                0.0,
                true));
    }
}
