package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.physics.EntityEquipmentChangeMask;
import dev.vessel.kernel.optimization.physics.VoxelShapeCollisionCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Lithium-inspired collision caching and entity equipment ticking optimizations.
 *
 * @version 1.0.0
 */
public class LithiumPhysicsOptimizationTest {

    @Test
    @DisplayName("VoxelShapeCollisionCache rapidly tests full cubes, slabs, and air with zero allocations")
    void testCollisionFastPath() {
        VoxelShapeCollisionCache cache = new VoxelShapeCollisionCache();

        // 1. Air (empty) never collides
        boolean airCollision = cache.testCollision(
                VoxelShapeCollisionCache.SHAPE_EMPTY,
                0.1, 0.0, 0.1, 0.9, 1.8, 0.9,
                0, 0, 0
        );
        assertThat(airCollision).isFalse();

        // 2. Full cube collision check
        boolean solidCollision = cache.testCollision(
                VoxelShapeCollisionCache.SHAPE_FULL_CUBE,
                0.2, 0.2, 0.2, 0.8, 1.8, 0.8,
                0, 0, 0
        );
        assertThat(solidCollision).isTrue();

        // 3. Non-intersecting full cube
        boolean distantCollision = cache.testCollision(
                VoxelShapeCollisionCache.SHAPE_FULL_CUBE,
                5.0, 5.0, 5.0, 6.0, 6.0, 6.0,
                0, 0, 0
        );
        assertThat(distantCollision).isFalse();

        // 4. Lower slab collision: entity is at Y [0.6, 1.8], slab is at Y [0.0, 0.5] -> no collision
        boolean slabNoCollision = cache.testCollision(
                VoxelShapeCollisionCache.SHAPE_LOWER_SLAB,
                0.1, 0.6, 0.1, 0.9, 1.8, 0.9,
                0, 0, 0
        );
        assertThat(slabNoCollision).isFalse();

        // 5. Lower slab collision: entity is at Y [0.2, 1.8] -> collision
        boolean slabCollision = cache.testCollision(
                VoxelShapeCollisionCache.SHAPE_LOWER_SLAB,
                0.1, 0.2, 0.1, 0.9, 1.8, 0.9,
                0, 0, 0
        );
        assertThat(slabCollision).isTrue();

        assertThat(cache.getTotalCollisionChecks()).isEqualTo(5L);
        assertThat(cache.getFastPathHits()).isEqualTo(5L);
        assertThat(cache.getComplexShapeFallbacks()).isEqualTo(0L);
        assertThat(cache.getFastPathRatio()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("EntityEquipmentChangeMask skips equipment tick and packet diffing when clean")
    void testEquipmentChangeMasking() {
        EntityEquipmentChangeMask mask = new EntityEquipmentChangeMask();

        // Initial state: clean
        assertThat(mask.hasChanges()).isFalse();
        assertThat(mask.getSkippedTicks()).isEqualTo(1L);

        // Multiple clean ticks
        for (int i = 0; i < 99; i++) {
            mask.hasChanges();
        }
        assertThat(mask.getTotalTickChecks()).isEqualTo(100L);
        assertThat(mask.getSkippedTicks()).isEqualTo(100L);
        assertThat(mask.getSkipRatio()).isEqualTo(1.0);

        // Mark slot CHEST dirty
        mask.markDirty(EntityEquipmentChangeMask.SLOT_CHEST);
        assertThat(mask.hasChanges()).isTrue();
        assertThat(mask.isSlotDirty(EntityEquipmentChangeMask.SLOT_CHEST)).isTrue();
        assertThat(mask.isSlotDirty(EntityEquipmentChangeMask.SLOT_MAIN_HAND)).isFalse();

        // Consume dirty state
        int dirtyBits = mask.consumeAndClear();
        assertThat(dirtyBits).isEqualTo(1 << EntityEquipmentChangeMask.SLOT_CHEST);

        // Mask is reset back to clean
        assertThat(mask.hasChanges()).isFalse();
    }

    @Test
    @DisplayName("EntityEquipmentChangeMask validates slot indices")
    void testSlotValidation() {
        EntityEquipmentChangeMask mask = new EntityEquipmentChangeMask();

        assertThatThrownBy(() -> mask.markDirty(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> mask.markDirty(6))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> mask.isSlotDirty(10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
