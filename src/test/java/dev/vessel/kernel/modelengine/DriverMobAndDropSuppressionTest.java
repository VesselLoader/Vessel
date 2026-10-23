package dev.vessel.kernel.modelengine;

import dev.vessel.kernel.modelengine.driver.CustomDropItem;
import dev.vessel.kernel.modelengine.driver.DriverDropSuppressor;
import dev.vessel.kernel.modelengine.driver.DriverHealthScaler;
import dev.vessel.kernel.modelengine.driver.DriverInvisibilityGovernor;
import dev.vessel.kernel.modelengine.driver.DriverMobController;
import dev.vessel.kernel.modelengine.driver.DriverMobState;
import dev.vessel.kernel.modelengine.driver.DroppedItemEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for Driver Mob AI root controller, scalable health, and drop suppression.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DriverMobAndDropSuppressionTest {

    @Test
    @DisplayName("DriverMobState tracks positions, rotations, and alive status")
    void testDriverMobStateLifecycle() {
        UUID uuid = UUID.randomUUID();
        DriverMobState state = new DriverMobState(101, uuid, "minecraft:zombie");

        assertThat(state.getEntityId()).isEqualTo(101);
        assertThat(state.getUniqueId()).isEqualTo(uuid);
        assertThat(state.getEntityType()).isEqualTo("minecraft:zombie");
        assertThat(state.isAlive()).isTrue();
        assertThat(state.isInvisible()).isTrue();
        assertThat(state.isSilent()).isTrue();

        state.setPosition(10.5, 64.0, -25.2);
        state.setRotation(180.0f, 15.0f);
        state.setMotionX(0.1);
        state.setMotionY(-0.05);
        state.setMotionZ(0.2);

        assertThat(state.getPosX()).isEqualTo(10.5);
        assertThat(state.getPosY()).isEqualTo(64.0);
        assertThat(state.getPosZ()).isEqualTo(-25.2);
        assertThat(state.getYaw()).isEqualTo(180.0f);
        assertThat(state.getPitch()).isEqualTo(15.0f);
        assertThat(state.getMotionX()).isEqualTo(0.1);
        assertThat(state.getMotionY()).isEqualTo(-0.05);
        assertThat(state.getMotionZ()).isEqualTo(0.2);
    }

    @Test
    @DisplayName("DriverHealthScaler scales custom health to 100, 200, 500 HP and computes percentage")
    void testDriverHealthScaling() {
        DriverHealthScaler scaler100 = new DriverHealthScaler(100.0f);
        DriverHealthScaler scaler200 = new DriverHealthScaler(200.0f);
        DriverHealthScaler scaler500 = new DriverHealthScaler(500.0f);

        assertThat(scaler100.computeEffectiveMaxHealth()).isEqualTo(100.0f);
        assertThat(scaler200.computeEffectiveMaxHealth()).isEqualTo(200.0f);
        assertThat(scaler500.computeEffectiveMaxHealth()).isEqualTo(500.0f);

        // Scaling multiplier
        scaler200.setScalingMultiplier(1.5f);
        assertThat(scaler200.computeEffectiveMaxHealth()).isEqualTo(300.0f);

        // Apply to driver state
        DriverMobState state = new DriverMobState(201, UUID.randomUUID(), "minecraft:pig");
        scaler500.configureDriverHealth(state);

        assertThat(state.getMaxHealth()).isEqualTo(500.0f);
        assertThat(state.getCurrentHealth()).isEqualTo(500.0f);
        assertThat(scaler500.getHealthPercentage(state)).isEqualTo(1.0f);

        state.setCurrentHealth(250.0f);
        assertThat(scaler500.getHealthPercentage(state)).isEqualTo(0.5f);

        // Boundary: negative health throws
        assertThatThrownBy(() -> new DriverHealthScaler(-10.0f))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("DriverInvisibilityGovernor enforces invisible 1b and silent 1b data watcher flags")
    void testDriverInvisibilityGovernor() {
        DriverInvisibilityGovernor governor = new DriverInvisibilityGovernor();
        DriverMobState state = new DriverMobState(301, UUID.randomUUID(), "minecraft:cow");

        state.setInvisible(false);
        state.setSilent(false);

        governor.governDriver(state);
        assertThat(state.isInvisible()).isTrue();
        assertThat(state.isSilent()).isTrue();

        byte mask = governor.computeDataWatcherMask((byte) 0x00);
        assertThat((mask & DriverInvisibilityGovernor.METADATA_FLAG_INVISIBLE)).isEqualTo(DriverInvisibilityGovernor.METADATA_FLAG_INVISIBLE);
    }

    @Test
    @DisplayName("DriverDropSuppressor cancels vanilla drops and rolls custom item drops")
    void testDriverDropSuppressionAndCustomDrops() {
        DriverDropSuppressor suppressor = new DriverDropSuppressor();
        assertThat(suppressor.shouldSuppressVanillaLoot()).isTrue();

        CustomDropItem guaranteedDrop = new CustomDropItem("mymod:boss_crystal", 2, 4, 1.0f, 0.0f);
        CustomDropItem rareDrop = new CustomDropItem("mymod:rare_gem", 1, 1, 0.0f, 0.5f); // only drops with looting

        suppressor.addCustomDrop(guaranteedDrop);
        suppressor.addCustomDrop(rareDrop);

        // Looting 0: guaranteed drop always rolls 2-4 items, rare drop rolls 0
        List<DroppedItemEntry> dropsLooting0 = suppressor.generateCustomDrops(0);
        assertThat(dropsLooting0).hasSize(1);
        assertThat(dropsLooting0.get(0).itemId()).isEqualTo("mymod:boss_crystal");
        assertThat(dropsLooting0.get(0).count()).isBetween(2, 4);

        // Looting 2: rare drop chance = 0.0 + 2 * 0.5 = 1.0 (guaranteed!)
        List<DroppedItemEntry> dropsLooting2 = suppressor.generateCustomDrops(2);
        assertThat(dropsLooting2).hasSize(2);
        assertThat(dropsLooting2.stream().anyMatch(d -> d.itemId().equals("mymod:rare_gem"))).isTrue();
    }

    @Test
    @DisplayName("DriverMobController coordinates damage, healing, and death handling")
    void testDriverMobControllerLifecycle() {
        DriverMobState state = new DriverMobState(401, UUID.randomUUID(), "minecraft:iron_golem");
        DriverHealthScaler healthScaler = new DriverHealthScaler(250.0f);
        DriverInvisibilityGovernor invisibilityGovernor = new DriverInvisibilityGovernor();
        DriverDropSuppressor dropSuppressor = new DriverDropSuppressor();
        dropSuppressor.addCustomDrop(new CustomDropItem("mymod:golem_core", 1, 1, 1.0f, 0.0f));

        DriverMobController controller = new DriverMobController(
                state,
                healthScaler,
                invisibilityGovernor,
                dropSuppressor
        );

        assertThat(controller.isAlive()).isTrue();
        assertThat(state.getCurrentHealth()).isEqualTo(250.0f);

        // Apply partial damage
        boolean stillAlive = controller.applyDamage(50.0f);
        assertThat(stillAlive).isTrue();
        assertThat(state.getCurrentHealth()).isEqualTo(200.0f);

        // Heal
        controller.heal(25.0f);
        assertThat(state.getCurrentHealth()).isEqualTo(225.0f);

        // Fatal damage
        boolean fatalResult = controller.applyDamage(300.0f);
        assertThat(fatalResult).isFalse();
        assertThat(controller.isAlive()).isFalse();
        assertThat(state.getCurrentHealth()).isEqualTo(0.0f);

        // Death drops
        List<DroppedItemEntry> drops = controller.onDeath(1);
        assertThat(drops).hasSize(1);
        assertThat(drops.get(0).itemId()).isEqualTo("mymod:golem_core");
    }
}
