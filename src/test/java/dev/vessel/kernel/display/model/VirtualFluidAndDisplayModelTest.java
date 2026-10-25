package dev.vessel.kernel.display.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VirtualFluidDisplaySurfaceManager, IronSpellsBloodCauldronPresenter,
 * and DisplayEntityInterpolationSmoother.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VirtualFluidAndDisplayModelTest {

    @Test
    @DisplayName("Should smoothly lerp fluid surface elevation and generate valid display frames")
    public void testFluidSurfaceManager() {
        VirtualFluidDisplaySurfaceManager manager = new VirtualFluidDisplaySurfaceManager();
        manager.setTargetLevel(0.8f);

        // Advance simulation by 5 frames (60 FPS = ~0.016s)
        VirtualFluidDisplayFrame lastFrame = null;
        for (int i = 0; i < 5; i++) {
            lastFrame = manager.update(0.0166f);
        }

        assertThat(lastFrame).isNotNull();
        assertThat(lastFrame.displayEntityId()).isPositive();
        assertThat(lastFrame.blockStateId()).isEqualTo(VirtualFluidDisplaySurfaceManager.DEFAULT_WATER_BLOCK_STATE);
        assertThat(manager.getCurrentLevel()).isGreaterThan(0.0f);
        assertThat(lastFrame.scaleY()).isGreaterThan(0.0f);

        // Snap level immediately
        manager.snapLevel(0.5f);
        assertThat(manager.getCurrentLevel()).isEqualTo(0.5f);
        assertThat(manager.getTargetLevel()).isEqualTo(0.5f);
    }

    @Test
    @DisplayName("Should simulate Blood Cauldron thermal dynamics and emit particle bursts")
    public void testBloodCauldronThermodynamics() {
        IronSpellsBloodCauldronPresenter presenter = new IronSpellsBloodCauldronPresenter(101, 10.0, 64.0, 10.0);
        presenter.setBloodLevel(0.9f);
        presenter.getSurfaceManager().snapLevel(0.9f);

        // Initial state cold
        assertThat(presenter.evaluateTemperatureState()).isEqualTo(CauldronTemperatureState.COLD);

        // Apply heat source
        presenter.setHeatSourceActive(true);
        for (int i = 0; i < 30; i++) {
            presenter.tick(0.05f);
        }

        // Temperature should have risen to BOILING or OVERHEATED
        CauldronTemperatureState state = presenter.evaluateTemperatureState();
        assertThat(state).isIn(CauldronTemperatureState.BOILING, CauldronTemperatureState.OVERHEATED);

        List<CauldronParticleDescriptor> particles = presenter.tick(0.05f);
        assertThat(particles).isNotEmpty();

        BloodCauldronSnapshot snapshot = presenter.createSnapshot();
        assertThat(snapshot.blockEntityId()).isEqualTo(101);
        assertThat(snapshot.boiling()).isTrue();
        assertThat(snapshot.heatSourcePresent()).isTrue();
    }

    @Test
    @DisplayName("Should adapt interpolation duration dynamically based on rolling TPS")
    public void testDisplayEntityInterpolationSmoother() {
        DisplayEntityInterpolationSmoother smoother = new DisplayEntityInterpolationSmoother(3, 20);

        // At nominal 20 TPS, duration should remain base 3 ticks
        InterpolationTimingMetric metric20 = smoother.updateTps(20.0);
        assertThat(metric20.calculatedDurationTicks()).isEqualTo(3);
        assertThat(smoother.getInterpolationDurationTicks()).isEqualTo(3);

        // Under severe lag (5 TPS), duration should scale up to smooth client stutter
        for (int i = 0; i < 10; i++) {
            smoother.updateTps(5.0);
        }

        assertThat(smoother.getInterpolationDurationTicks()).isGreaterThan(3);
        assertThat(smoother.getInterpolationDurationTicks()).isLessThanOrEqualTo(20);
        assertThat(smoother.getCurrentLerpFactor()).isLessThan(1.0f);
    }
}
