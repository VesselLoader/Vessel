package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.chunk.FastChunkSectionPalette;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Lithium-inspired chunk section palette optimization.
 *
 * @version 1.0.0
 */
public class ChunkOptimizationTest {

    @Test
    @DisplayName("FastChunkSectionPalette starts homogeneous and adapts upon mutation")
    void testHomogeneousAndAdaptation() {
        int stoneStateId = 1;
        FastChunkSectionPalette section = new FastChunkSectionPalette(stoneStateId);

        assertThat(section.isHomogeneous()).isTrue();
        assertThat(section.getSingleStateId()).isEqualTo(stoneStateId);
        assertThat(section.getNonAirBlockCount()).isEqualTo(FastChunkSectionPalette.VOLUME);
        assertThat(section.getBlockStateId(0, 0, 0)).isEqualTo(stoneStateId);
        assertThat(section.getBlockStateId(15, 15, 15)).isEqualTo(stoneStateId);

        // Mutate one block to diamond (id=100)
        int diamondStateId = 100;
        int oldState = section.setBlockStateId(3, 5, 7, diamondStateId);
        assertThat(oldState).isEqualTo(stoneStateId);
        assertThat(section.isHomogeneous()).isFalse();

        // Target position is updated, others remain stone
        assertThat(section.getBlockStateId(3, 5, 7)).isEqualTo(diamondStateId);
        assertThat(section.getBlockStateId(0, 0, 0)).isEqualTo(stoneStateId);

        // Setting same value does not change state
        int prev = section.setBlockStateId(3, 5, 7, diamondStateId);
        assertThat(prev).isEqualTo(diamondStateId);
    }

    @Test
    @DisplayName("FastChunkSectionPalette tracks air counts and compacts back to homogeneous")
    void testCompaction() {
        FastChunkSectionPalette section = new FastChunkSectionPalette(0); // all air
        assertThat(section.getNonAirBlockCount()).isEqualTo(0);

        // Add two blocks
        section.setBlockStateId(0, 0, 0, 5);
        section.setBlockStateId(1, 1, 1, 5);
        assertThat(section.getNonAirBlockCount()).isEqualTo(2);

        // Remove one block
        section.setBlockStateId(0, 0, 0, 0);
        assertThat(section.getNonAirBlockCount()).isEqualTo(1);

        // Remove other block
        section.setBlockStateId(1, 1, 1, 0);
        assertThat(section.getNonAirBlockCount()).isEqualTo(0);

        // Compact back
        section.compact();
        assertThat(section.isHomogeneous()).isTrue();
        assertThat(section.getSingleStateId()).isEqualTo(0);
    }

    @Test
    @DisplayName("FastChunkSectionPalette validates coordinate bounds")
    void testBoundsValidation() {
        FastChunkSectionPalette section = new FastChunkSectionPalette(0);

        assertThatThrownBy(() -> section.getBlockStateId(-1, 0, 0))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> section.getBlockStateId(0, 16, 0))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> section.setBlockStateId(0, 0, 20, 1))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
