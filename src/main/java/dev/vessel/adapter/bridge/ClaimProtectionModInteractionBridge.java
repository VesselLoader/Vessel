package dev.vessel.adapter.bridge;

import dev.vessel.kernel.optimization.block.TriIntPredicate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enforces Bukkit/Paper land claim protection plugins (WorldGuard, Towny, Lands,
 * GriefPrevention, Residence) against modded firearms (TACZ, Point Blank), magic spells
 * (Iron's Spells, Ars Nouveau), and automated block-breaking drills (Create, Mekanism).
 *
 * @version 1.0.0
 */
public final class ClaimProtectionModInteractionBridge {

    private final List<TriIntPredicate> claimPermissionHooks = new CopyOnWriteArrayList<>();

    /**
     * Creates a new claim protection mod interaction bridge.
     */
    public ClaimProtectionModInteractionBridge() {
    }

    /**
     * Registers a spatial claim permission predicate returning {@code true} if modification
     * at {@code (x, y, z)} is permitted, or {@code false} if protected by a claim plugin.
     *
     * @param blockModifyPermissionPredicate zero-allocation spatial permission predicate
     */
    public void registerClaimPermissionHook(TriIntPredicate blockModifyPermissionPredicate) {
        Objects.requireNonNull(
                blockModifyPermissionPredicate,
                "blockModifyPermissionPredicate must not be null");
        claimPermissionHooks.add(blockModifyPermissionPredicate);
    }

    /**
     * Evaluates whether a modded firearm bullet, spell, or mechanical drill owned by
     * {@code shooterOrOwnerUuid} is permitted to break or modify the block at
     * {@code (blockX, blockY, blockZ)}.
     *
     * @param shooterOrOwnerUuid UUID of the player or FakePlayer owner
     * @param blockX             target block X
     * @param blockY             target block Y
     * @param blockZ             target block Z
     * @param weaponOrMachineId  namespaced weapon or machine ID
     * @return {@code true} if allowed by all registered claim hooks, {@code false} if denied
     */
    public boolean canModWeaponOrDrillBreakBlock(
            UUID shooterOrOwnerUuid,
            int blockX,
            int blockY,
            int blockZ,
            String weaponOrMachineId) {
        Objects.requireNonNull(shooterOrOwnerUuid, "shooterOrOwnerUuid must not be null");
        Objects.requireNonNull(weaponOrMachineId, "weaponOrMachineId must not be null");
        for (TriIntPredicate hook : claimPermissionHooks) {
            if (!hook.test(blockX, blockY, blockZ)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluates whether a modded projectile or spell cast by {@code shooterUuid} is permitted
     * to damage {@code targetEntityUuid} at {@code (targetX, targetY, targetZ)}.
     *
     * @param shooterUuid      UUID of the attacking player
     * @param targetEntityUuid UUID of the target entity
     * @param targetX          target entity block X
     * @param targetY          target entity block Y
     * @param targetZ          target entity block Z
     * @return {@code true} if allowed by all registered claim hooks, {@code false} if denied
     */
    public boolean canModProjectileDamageEntity(
            UUID shooterUuid,
            UUID targetEntityUuid,
            int targetX,
            int targetY,
            int targetZ) {
        Objects.requireNonNull(shooterUuid, "shooterUuid must not be null");
        Objects.requireNonNull(targetEntityUuid, "targetEntityUuid must not be null");
        for (TriIntPredicate hook : claimPermissionHooks) {
            if (!hook.test(targetX, targetY, targetZ)) {
                return false;
            }
        }
        return true;
    }
}
