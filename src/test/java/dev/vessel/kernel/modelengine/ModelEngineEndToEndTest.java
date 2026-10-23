package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.BoneNode;
import dev.vessel.kernel.modelengine.driver.CustomDropItem;
import dev.vessel.kernel.modelengine.driver.DroppedItemEntry;
import dev.vessel.kernel.modelengine.hitbox.DamageRelayResult;
import dev.vessel.kernel.modelengine.hitbox.DynamicHitbox;
import dev.vessel.kernel.modelengine.hitbox.HitboxType;
import dev.vessel.kernel.modelengine.lod.ModelAnimationLODTier;
import dev.vessel.kernel.optimization.entity.PlayerSpatialCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test suite verifying the complete Model Engine architecture on VESSEL:
 * root driver mob, scalable health, drop suppression, dynamic hitboxes, bone hierarchy,
 * passenger mounting anti-lag, and activation range LOD.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class ModelEngineEndToEndTest {

    @Test
    @DisplayName("End-to-end lifecycle: spawn, passenger mount, LOD throttling, localized headshot, and drop suppression")
    void testCompleteModelEngineLifecycle() {
        ModelEngineManager manager = new ModelEngineManager();

        // 1. Register model template with bone hierarchy
        BoneHierarchyTree dragonTemplate = new BoneHierarchyTree();

        BoneNode torso = new BoneNode("torso", null);
        torso.setTranslation(0.0f, 1.5f, 0.0f);
        torso.setCustomModelData(50001);
        dragonTemplate.registerBone(torso);

        BoneNode head = new BoneNode("head", "torso");
        head.setTranslation(0.0f, 1.0f, 2.0f);
        head.setCustomModelData(50002);
        dragonTemplate.registerBone(head);

        BoneNode leftWing = new BoneNode("left_wing", "torso");
        leftWing.setTranslation(-2.0f, 0.5f, 0.0f);
        leftWing.setCustomModelData(50003);
        dragonTemplate.registerBone(leftWing);

        BoneNode rightWing = new BoneNode("right_wing", "torso");
        rightWing.setTranslation(2.0f, 0.5f, 0.0f);
        rightWing.setCustomModelData(50004);
        dragonTemplate.registerBone(rightWing);

        manager.registerModelTemplate("nether_dragon", dragonTemplate);

        // 2. Spawn instance: Pig driver mob with 300 HP, invisible, silent, vanilla drops cancelled, custom drops
        CustomDropItem customHeart = new CustomDropItem("mymod:dragon_heart", 1, 1, 1.0f, 0.0f);
        ActiveModelInstance instance = manager.spawnInstance(
                "instance_dragon_1",
                "nether_dragon",
                1000,
                UUID.randomUUID(),
                "minecraft:pig",
                100.0, 64.0, 100.0,
                300.0f,
                true,
                List.of(customHeart)
        );

        assertThat(manager.getActiveInstanceCount()).isEqualTo(1);
        assertThat(instance.getDriverController().isAlive()).isTrue();
        assertThat(instance.getDriverController().getState().getCurrentHealth()).isEqualTo(300.0f);
        assertThat(instance.getDriverController().getState().isInvisible()).isTrue();
        assertThat(instance.getDriverController().getState().isSilent()).isTrue();
        assertThat(instance.getDriverController().getDropSuppressor().shouldSuppressVanillaLoot()).isTrue();

        // 3. Attach dynamic hitboxes (Interaction entities visible in F3+B)
        DynamicHitbox headHitbox = new DynamicHitbox("head_box", 2001, HitboxType.INTERACTION, "head", 1.2f, 1.2f);
        headHitbox.setDamageMultiplier(2.5f); // 2.5x localized headshot multiplier
        instance.addHitbox(headHitbox);
        manager.indexHitbox(2001, instance);

        DynamicHitbox bodyHitbox = new DynamicHitbox("body_box", 2002, HitboxType.INTERACTION, "torso", 2.0f, 2.0f);
        bodyHitbox.setDamageMultiplier(1.0f);
        instance.addHitbox(bodyHitbox);
        manager.indexHitbox(2002, instance);

        // Verify passenger mounting: 4 bones + 2 hitboxes = 6 passengers
        assertThat(instance.getPassengerTracker().getPassengerCount()).isEqualTo(6);
        assertThat(instance.getPassengerTracker().calculatePacketReductionRatio()).isGreaterThanOrEqualTo(0.85);

        // 4. Tick with player at near distance (<16m) -> LOD NEAR (20 TPS)
        List<PlayerSpatialCoordinates> nearPlayers = List.of(new PlayerSpatialCoordinates(105.0, 64.0, 100.0));
        manager.tick(1L, nearPlayers);

        assertThat(instance.getActiveTier()).isEqualTo(ModelAnimationLODTier.NEAR);
        assertThat(instance.getBoneUpdatesRun()).isGreaterThanOrEqualTo(1);

        // Tick with player at far distance (>32m) -> LOD FAR (frozen bones, 0 updates)
        long updatesBefore = instance.getBoneUpdatesRun();
        List<PlayerSpatialCoordinates> farPlayers = List.of(new PlayerSpatialCoordinates(200.0, 64.0, 200.0));
        manager.tick(2L, farPlayers);

        assertThat(instance.getActiveTier()).isEqualTo(ModelAnimationLODTier.FAR);
        assertThat(instance.getBoneUpdatesRun()).isEqualTo(updatesBefore); // Frozen!

        // 5. Damage relay: Attack head hitbox with raw 40 damage -> 40 * 2.5 = 100 damage relayed!
        DamageRelayResult headDamage = manager.relayAttack(2001, 40.0f);
        assertThat(headDamage).isNotNull();
        assertThat(headDamage.hitboxId()).isEqualTo("head_box");
        assertThat(headDamage.localizedMultiplier()).isEqualTo(2.5f);
        assertThat(headDamage.finalDamage()).isEqualTo(100.0f);
        assertThat(headDamage.remainingHealth()).isEqualTo(200.0f);
        assertThat(headDamage.fatal()).isFalse();

        // 6. Fatal attack: Raw 80 damage to head -> 80 * 2.5 = 200 damage against 200 HP remaining -> FATAL!
        DamageRelayResult fatalAttack = manager.relayAttack(2001, 80.0f);
        assertThat(fatalAttack).isNotNull();
        assertThat(fatalAttack.finalDamage()).isEqualTo(200.0f);
        assertThat(fatalAttack.remainingHealth()).isEqualTo(0.0f);
        assertThat(fatalAttack.fatal()).isTrue();

        // 7. Handle death and custom drops
        List<DroppedItemEntry> drops = manager.handleDeath(1000, 0);
        assertThat(drops).hasSize(1);
        assertThat(drops.get(0).itemId()).isEqualTo("mymod:dragon_heart");
        assertThat(drops.get(0).count()).isEqualTo(1);

        // 8. Verify instance and hitboxes purged
        assertThat(manager.getActiveInstanceCount()).isEqualTo(0);
        assertThat(manager.getInstanceByDriverId(1000)).isNull();
        assertThat(manager.getInstanceByHitboxId(2001)).isNull();
        assertThat(manager.getInstanceByHitboxId(2002)).isNull();
    }
}
