package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.spatial.ChunkSectionSpatialBitSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Sodium-inspired spatial bitmask tracker.
 *
 * @version 1.0.0
 */
public class SpatialBitSetTest {

    @Test
    @DisplayName("ChunkSectionSpatialBitSet accurately sets, queries, and counts sections")
    void testSectionBitOperations() {
        ChunkSectionSpatialBitSet bitSet = new ChunkSectionSpatialBitSet();

        assertThat(bitSet.countActiveSections()).isEqualTo(0);
        assertThat(bitSet.getLowestActiveSection()).isEqualTo(-1);
        assertThat(bitSet.getHighestActiveSection()).isEqualTo(-1);

        // Set sections 4, 10, 23 (typical Overworld non-empty section indices)
        bitSet.setSectionActive(4);
        bitSet.setSectionActive(10);
        bitSet.setSectionActive(23);

        assertThat(bitSet.countActiveSections()).isEqualTo(3);
        assertThat(bitSet.hasSection(4)).isTrue();
        assertThat(bitSet.hasSection(10)).isTrue();
        assertThat(bitSet.hasSection(23)).isTrue();
        assertThat(bitSet.hasSection(5)).isFalse();

        assertThat(bitSet.getLowestActiveSection()).isEqualTo(4);
        assertThat(bitSet.getHighestActiveSection()).isEqualTo(23);

        // Clear section 4
        bitSet.clearSection(4);
        assertThat(bitSet.hasSection(4)).isFalse();
        assertThat(bitSet.countActiveSections()).isEqualTo(2);
        assertThat(bitSet.getLowestActiveSection()).isEqualTo(10);

        // Clear all
        bitSet.clearAll();
        assertThat(bitSet.countActiveSections()).isEqualTo(0);
    }

    @Test
    @DisplayName("ChunkSectionSpatialBitSet detects spatial intersections")
    void testIntersections() {
        ChunkSectionSpatialBitSet setA = new ChunkSectionSpatialBitSet();
        ChunkSectionSpatialBitSet setB = new ChunkSectionSpatialBitSet();

        setA.setSectionActive(5);
        setA.setSectionActive(12);

        setB.setSectionActive(7);
        setB.setSectionActive(15);

        // Disjoint sets
        assertThat(setA.intersects(setB)).isFalse();

        // Overlapping sets
        setB.setSectionActive(12);
        assertThat(setA.intersects(setB)).isTrue();
    }

    @Test
    @DisplayName("ChunkSectionSpatialBitSet validates index range (0 to 63)")
    void testRangeValidation() {
        ChunkSectionSpatialBitSet bitSet = new ChunkSectionSpatialBitSet();

        assertThatThrownBy(() -> bitSet.setSectionActive(-1))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> bitSet.setSectionActive(64))
                .isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> bitSet.hasSection(100))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
