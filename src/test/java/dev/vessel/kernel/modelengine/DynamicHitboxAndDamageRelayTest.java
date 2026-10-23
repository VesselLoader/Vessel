package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.modelengine.bone.QuaternionRotation;
import dev.vessel.kernel.modelengine.driver.DriverDropSuppressor;
import dev.vessel.kernel.modelengine.driver.DriverHealthScaler;
import dev.vessel.kernel.modelengine.driver.DriverInvisibilityGovernor;
import dev.vessel.kernel.modelengine.driver.DriverMobController;
import dev.vessel.kernel.modelengine.driver.DriverMobState;
import dev.vessel.kernel.modelengine.hitbox.DamageRelayCoordinator;
import dev.vessel.kernel.modelengine.hitbox.DamageRelayResult;
import dev.vessel.kernel.modelengine.hitbox.DynamicHitbox;
import dev.vessel.kernel.modelengine.hitbox.HitboxInteractionEvent;
import dev.vessel.kernel.modelengine.hitbox.HitboxInteractionRelay;
import dev.vessel.kernel.modelengine.hitbox.HitboxSpatialCalculator;
import dev.vessel.kernel.modelengine.hitbox.HitboxType;
import dev.vessel.kernel.modelengine.hitbox.LegacyCompoundHitboxSimulator;
import dev.vessel.kernel.modelengine.hitbox.ModernInteractionHitboxSimulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * Verification test suite for dynamic hitboxes, F3+B debug rendering simulation,
 * and localized damage/interaction relay.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class DynamicHitboxAndDamageRelayTest {

    @Test
    @DisplayName("DynamicHitbox calculates AABB bounds visible under F3 + B")
    void testDynamicHitboxBoundingBox() {
        DynamicHitbox hitbox = new DynamicHitbox("torso", HitboxType.INTERACTION, "body", 1.2f, 1.8f);
        hitbox.setWorldPosition(10.0, 64.0, 20.0);

        float[] aabb = hitbox.computeBoundingBox();
        // [minX, minY, minZ, maxX, maxY, maxZ]
        assertThat(aabb[0]).isEqualTo(9.4f);  // 10.0 - 0.6
        assertThat(aabb[1]).isEqualTo(64.0f); // 64.0
        assertThat(aabb[2]).isEqualTo(19.4f); // 20.0 - 0.6
        assertThat(aabb[3]).isEqualTo(10.6f); // 10.0 + 0.6
        assertThat(aabb[4]).isEqualTo(65.8f); // 64.0 + 1.8
        assertThat(aabb[5]).isEqualTo(20.6f); // 20.0 + 0.6
    }

    @Test
    @DisplayName("ModernInteractionHitboxSimulator tests point containment and raycast intersection")
    void testModernInteractionHitboxSimulation() {
        DynamicHitbox hitbox = new DynamicHitbox("head", HitboxType.INTERACTION, "head_bone", 1.0f, 1.0f);
        hitbox.setWorldPosition(0.0, 10.0, 0.0);

        ModernInteractionHitboxSimulator simulator = new ModernInteractionHitboxSimulator(hitbox);

        // Point containment
        assertThat(simulator.containsPoint(0.0, 10.5, 0.0)).isTrue();
        assertThat(simulator.containsPoint(5.0, 10.5, 0.0)).isFalse();

        // Raycast hit: ray starting at (0, 10.5, -5) looking towards positive Z (0, 0, 1)
        boolean hit = simulator.intersectsRay(0.0, 10.5, -5.0, 0.0, 0.0, 1.0, 10.0);
        assertThat(hit).isTrue();

        // Raycast miss: ray pointing away in negative Z
        boolean miss = simulator.intersectsRay(0.0, 10.5, -5.0, 0.0, 0.0, -1.0, 10.0);
        assertThat(miss).isFalse();
    }

    @Test
    @DisplayName("LegacyCompoundHitboxSimulator computes discrete slime sizes for legacy compatibility")
    void testLegacyCompoundHitboxSimulation() {
        DynamicHitbox hitbox = new DynamicHitbox("legacy_part", HitboxType.LEGACY_SLIME, null, 1.5f, 1.5f);
        LegacyCompoundHitboxSimulator simulator = new LegacyCompoundHitboxSimulator(hitbox);

        int slimeSize = simulator.computeSlimeSize(1.5f, 1.5f);
        assertThat(slimeSize).isGreaterThanOrEqualTo(3);

        simulator.applySlimeSize(4);
        assertThat(simulator.getSlimeSize()).isEqualTo(4);
        assertThat(hitbox.getWidth()).isEqualTo(4 * LegacyCompoundHitboxSimulator.SLIME_SIZE_FACTOR);
    }

    @Test
    @DisplayName("HitboxSpatialCalculator rotates relative offsets according to driver mob yaw")
    void testHitboxSpatialCalculation() {
        HitboxSpatialCalculator calculator = new HitboxSpatialCalculator();
        DynamicHitbox hitbox = new DynamicHitbox("nose", HitboxType.INTERACTION, null, 0.5f, 0.5f);
        hitbox.setOffsets(0.0, 1.0, 2.0); // 2 blocks in front

        // Root at (10, 20, 30) facing yaw 0 (positive Z in Minecraft)
        calculator.updateRootHitboxPosition(hitbox, 10.0, 20.0, 30.0, 0.0f);
        assertThat(hitbox.getWorldX()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1e-4));
        assertThat(hitbox.getWorldY()).isEqualTo(21.0);
        assertThat(hitbox.getWorldZ()).isCloseTo(32.0, org.assertj.core.data.Offset.offset(1e-4));

        // Facing yaw 90 (facing negative X in Minecraft standard yaw convention)
        calculator.updateRootHitboxPosition(hitbox, 10.0, 20.0, 30.0, 90.0f);
        assertThat(hitbox.getWorldX()).isCloseTo(8.0, org.assertj.core.data.Offset.offset(1e-4));
        assertThat(hitbox.getWorldZ()).isCloseTo(30.0, org.assertj.core.data.Offset.offset(1e-4));
    }

    @Test
    @DisplayName("DamageRelayCoordinator applies localized multipliers (headshot 2.0x, limbs 0.75x) to driver mob")
    void testLocalizedDamageRelay() {
        DriverMobState state = new DriverMobState(501, UUID.randomUUID(), "minecraft:husk");
        DriverHealthScaler healthScaler = new DriverHealthScaler(100.0f);
        DriverMobController controller = new DriverMobController(
                state,
                healthScaler,
                new DriverInvisibilityGovernor(),
                new DriverDropSuppressor()
        );

        DynamicHitbox headHitbox = new DynamicHitbox("head", HitboxType.INTERACTION, "head", 0.8f, 0.8f);
        headHitbox.setDamageMultiplier(2.0f); // Headshot multiplier

        DynamicHitbox legHitbox = new DynamicHitbox("leg", HitboxType.INTERACTION, "leg", 0.6f, 1.0f);
        legHitbox.setDamageMultiplier(0.75f); // Limb damage reduction

        DamageRelayCoordinator coordinator = new DamageRelayCoordinator();

        // Raw 10 damage to head -> 20 damage applied to mob (100 -> 80 HP)
        DamageRelayResult headResult = coordinator.relayDamage(headHitbox, controller, 10.0f);
        assertThat(headResult.hitboxId()).isEqualTo("head");
        assertThat(headResult.rawDamage()).isEqualTo(10.0f);
        assertThat(headResult.localizedMultiplier()).isEqualTo(2.0f);
        assertThat(headResult.finalDamage()).isEqualTo(20.0f);
        assertThat(headResult.remainingHealth()).isEqualTo(80.0f);
        assertThat(headResult.fatal()).isFalse();

        // Raw 20 damage to leg -> 15 damage applied to mob (80 -> 65 HP)
        DamageRelayResult legResult = coordinator.relayDamage(legHitbox, controller, 20.0f);
        assertThat(legResult.hitboxId()).isEqualTo("leg");
        assertThat(legResult.rawDamage()).isEqualTo(20.0f);
        assertThat(legResult.localizedMultiplier()).isEqualTo(0.75f);
        assertThat(legResult.finalDamage()).isEqualTo(15.0f);
        assertThat(legResult.remainingHealth()).isEqualTo(65.0f);
        assertThat(legResult.fatal()).isFalse();

        // Fatal attack to head (40 raw * 2.0 = 80 damage against 65 HP remaining)
        DamageRelayResult fatalResult = coordinator.relayDamage(headHitbox, controller, 40.0f);
        assertThat(fatalResult.finalDamage()).isEqualTo(80.0f);
        assertThat(fatalResult.remainingHealth()).isEqualTo(0.0f);
        assertThat(fatalResult.fatal()).isTrue();
        assertThat(controller.isAlive()).isFalse();
    }

    @Test
    @DisplayName("HitboxInteractionRelay forwards player right-click interaction events")
    void testHitboxInteractionRelay() {
        DriverMobState state = new DriverMobState(601, UUID.randomUUID(), "minecraft:horse");
        DriverMobController controller = new DriverMobController(
                state,
                new DriverHealthScaler(50.0f),
                new DriverInvisibilityGovernor(),
                new DriverDropSuppressor()
        );

        DynamicHitbox saddleHitbox = new DynamicHitbox("saddle", HitboxType.INTERACTION, null, 1.0f, 1.0f);
        HitboxInteractionRelay relay = new HitboxInteractionRelay();

        HitboxInteractionEvent event = relay.relayInteraction(saddleHitbox, controller, 1001, true, 5.0, 1.2, -3.4);

        assertThat(event.hitboxId()).isEqualTo("saddle");
        assertThat(event.playerId()).isEqualTo(1001);
        assertThat(event.isMainHand()).isTrue();
        assertThat(event.clickX()).isEqualTo(5.0);
        assertThat(event.clickY()).isEqualTo(1.2);
        assertThat(event.clickZ()).isEqualTo(-3.4);
    }

    @Test
    @DisplayName("Deactivated hitbox suppresses point containment, raycast, damage relay, and interaction")
    void testDeactivatedHitboxIgnoresAttacksAndInteractions() {
        DynamicHitbox disabledHitbox = new DynamicHitbox("shield_part", HitboxType.INTERACTION, null, 1.0f, 1.0f);
        disabledHitbox.setWorldPosition(0.0, 10.0, 0.0);
        disabledHitbox.setActive(false);

        ModernInteractionHitboxSimulator simulator = new ModernInteractionHitboxSimulator(disabledHitbox);
        assertThat(simulator.containsPoint(0.0, 10.5, 0.0)).isFalse();
        assertThat(simulator.intersectsRay(0.0, 10.5, -5.0, 0.0, 0.0, 1.0, 10.0)).isFalse();

        DriverMobState state = new DriverMobState(701, UUID.randomUUID(), "minecraft:iron_golem");
        DriverMobController controller = new DriverMobController(
                state,
                new DriverHealthScaler(100.0f),
                new DriverInvisibilityGovernor(),
                new DriverDropSuppressor()
        );

        DamageRelayCoordinator coordinator = new DamageRelayCoordinator();
        DamageRelayResult result = coordinator.relayDamage(disabledHitbox, controller, 50.0f);
        assertThat(result.finalDamage()).isEqualTo(0.0f);
        assertThat(result.remainingHealth()).isEqualTo(100.0f);
        assertThat(controller.getState().getCurrentHealth()).isEqualTo(100.0f);

        HitboxInteractionRelay relay = new HitboxInteractionRelay();
        HitboxInteractionEvent event = relay.relayInteraction(disabledHitbox, controller, 1002, true, 0.0, 10.5, 0.0);
        assertThat(event).isNull();
    }

    @Test
    @DisplayName("HitboxSpatialCalculator rotates hitbox local offsets by bone quaternion rotation")
    void testHitboxSpatialCalculatorWithBoneQuaternionRotation() {
        HitboxSpatialCalculator calculator = new HitboxSpatialCalculator();

        DynamicHitbox hitbox = new DynamicHitbox("snout", HitboxType.INTERACTION, "head", 0.5f, 0.5f);
        // Hitbox offset +2 blocks forward along Z relative to bone
        hitbox.setOffsets(0.0, 0.0, 2.0);

        // Bone rotated 90 deg around Y axis (yaw = PI/2)
        QuaternionRotation boneRot90 = QuaternionRotation.fromEuler(0.0f, (float) (Math.PI / 2.0), 0.0f);

        // Driver entity at (10, 64, 10) with rootYaw = 0 (facing south)
        // Bone translation at (0, 1.5, 0)
        calculator.updateBoneHitboxPosition(
                hitbox,
                10.0, 64.0, 10.0,
                0.0f,
                0.0f, 1.5f, 0.0f,
                boneRot90
        );

        // (0, 0, 2) rotated 90 deg around Y in right-handed system points to (2, 0, 0) in boneRot
        // Vector rotation: (0, 0, 2) with yaw 90 -> (+2, 0, 0)
        // World coordinates = (10 + 2, 64 + 1.5, 10 + 0) = (12.0, 65.5, 10.0)
        assertThat(hitbox.getWorldX()).isCloseTo(12.0, offset(1e-4));
        assertThat(hitbox.getWorldY()).isCloseTo(65.5, offset(1e-4));
        assertThat(hitbox.getWorldZ()).isCloseTo(10.0, offset(1e-4));
    }

    @Test
    @DisplayName("ModernInteractionHitboxSimulator correctly handles negative-zero ray direction without sign flip")
    void testModernInteractionHitboxNegativeZeroRaycast() {
        DynamicHitbox hitbox = new DynamicHitbox("chest", HitboxType.INTERACTION, null, 2.0f, 2.0f);
        hitbox.setWorldPosition(0.0, 5.0, 10.0);

        ModernInteractionHitboxSimulator simulator = new ModernInteractionHitboxSimulator(hitbox);

        // Ray from (0, 6.0, 0) pointing along +Z towards target at Z=10, with rayDirX = -0.0
        double rayDirX = -0.0;
        double rayDirY = 0.0;
        double rayDirZ = 1.0;

        boolean hit = simulator.intersectsRay(0.0, 6.0, 0.0, rayDirX, rayDirY, rayDirZ, 15.0);
        assertThat(hit).isTrue();

        // Ray pointing away (Z = -1) with rayDirX = -0.0 should miss
        boolean miss = simulator.intersectsRay(0.0, 6.0, 0.0, rayDirX, rayDirY, -1.0, 15.0);
        assertThat(miss).isFalse();
    }
}
