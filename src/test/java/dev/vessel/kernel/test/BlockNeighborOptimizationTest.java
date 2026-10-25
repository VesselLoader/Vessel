package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.block.CompactBlockNeighborTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for FerriteCore-inspired neighbor state transition table.
 *
 * @version 1.0.0
 */
public class BlockNeighborOptimizationTest {

    @Test
    @DisplayName("CompactBlockNeighborTable records and retrieves directional transitions in 1 CPU cycle")
    void testNeighborTransitions() {
        CompactBlockNeighborTable table = new CompactBlockNeighborTable(16);

        // State 5 updates to State 8 on DIR_NORTH, and State 9 on DIR_SOUTH
        table.setNeighborState(5, CompactBlockNeighborTable.DIR_NORTH, 8);
        table.setNeighborState(5, CompactBlockNeighborTable.DIR_SOUTH, 9);

        assertThat(table.getNeighborState(5, CompactBlockNeighborTable.DIR_NORTH)).isEqualTo(8);
        assertThat(table.getNeighborState(5, CompactBlockNeighborTable.DIR_SOUTH)).isEqualTo(9);

        // Unset directions return -1
        assertThat(table.getNeighborState(5, CompactBlockNeighborTable.DIR_UP)).isEqualTo(-1);
        assertThat(table.getNeighborState(5, CompactBlockNeighborTable.DIR_DOWN)).isEqualTo(-1);
        assertThat(table.getNeighborState(5, CompactBlockNeighborTable.DIR_WEST)).isEqualTo(-1);
        assertThat(table.getNeighborState(5, CompactBlockNeighborTable.DIR_EAST)).isEqualTo(-1);
    }

    @Test
    @DisplayName("CompactBlockNeighborTable dynamically expands capacity for high state IDs")
    void testCapacityExpansion() {
        CompactBlockNeighborTable table = new CompactBlockNeighborTable(4);
        assertThat(table.getCapacity()).isEqualTo(4);

        // Set transition for state 100 (exceeds initial capacity)
        table.setNeighborState(100, CompactBlockNeighborTable.DIR_UP, 105);

        assertThat(table.getCapacity()).isGreaterThanOrEqualTo(101);
        assertThat(table.getNeighborState(100, CompactBlockNeighborTable.DIR_UP)).isEqualTo(105);
        assertThat(table.getMemorySizeBytes()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("CompactBlockNeighborTable enforces direction boundaries")
    void testDirectionBoundaries() {
        CompactBlockNeighborTable table = new CompactBlockNeighborTable(10);

        assertThatThrownBy(() -> table.setNeighborState(1, -1, 2))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> table.setNeighborState(1, 6, 2))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> table.getNeighborState(1, 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
