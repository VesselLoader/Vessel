package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.memory.CompactStatePropertyTable;
import dev.vessel.kernel.optimization.memory.IdentifierPoolDeduplicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for FerriteCore-inspired memory compaction optimizations.
 *
 * @version 1.0.0
 */
public class FerriteCoreMemoryOptimizationTest {

    @Test
    @DisplayName("IdentifierPoolDeduplicator returns canonical string reference for identical strings")
    void testStringDeduplication() {
        IdentifierPoolDeduplicator deduplicator = new IdentifierPoolDeduplicator(128);

        String str1 = new String("minecraft:stone");
        String str2 = new String("minecraft:stone");
        assertThat(str1).isNotSameAs(str2);

        String canonical1 = deduplicator.deduplicate(str1);
        String canonical2 = deduplicator.deduplicate(str2);

        assertThat(canonical1).isSameAs(canonical2);
        assertThat(deduplicator.getTotalRequests()).isEqualTo(2L);
        assertThat(deduplicator.getDeduplicatedHits()).isEqualTo(1L);
        assertThat(deduplicator.getUniqueCount()).isEqualTo(1);
        assertThat(deduplicator.getEstimatedSavedBytes()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("IdentifierPoolDeduplicator canonicalizes namespace and path pairs")
    void testNamespaceAndPathDeduplication() {
        IdentifierPoolDeduplicator deduplicator = new IdentifierPoolDeduplicator();

        String id1 = deduplicator.deduplicateIdentifier(new String("fabric"), new String("copper_wire"));
        String id2 = deduplicator.deduplicateIdentifier(new String("fabric"), new String("copper_wire"));

        assertThat(id1).isEqualTo("fabric:copper_wire");
        assertThat(id1).isSameAs(id2);
        assertThat(deduplicator.getDeduplicatedHits()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("CompactStatePropertyTable bit-packs and unpacks multi-property blockstates")
    void testCompactStatePacking() {
        Map<String, Integer> properties = new LinkedHashMap<>();
        properties.put("facing", 6);       // needs 3 bits (0..5)
        properties.put("waterlogged", 2);  // needs 1 bit (0..1)
        properties.put("powered", 2);      // needs 1 bit (0..1)

        CompactStatePropertyTable table = new CompactStatePropertyTable(properties);
        assertThat(table.getPropertyCount()).isEqualTo(3);
        assertThat(table.getTotalBitsUsed()).isEqualTo(5); // 3 + 1 + 1 = 5 bits

        // Pack: facing=3 (south), waterlogged=1 (true), powered=0 (false)
        long packed = table.packState(new int[]{3, 1, 0});

        assertThat(table.unpackValueIndex(packed, 0)).isEqualTo(3);
        assertThat(table.unpackValueIndex(packed, "facing")).isEqualTo(3);
        assertThat(table.unpackValueIndex(packed, 1)).isEqualTo(1);
        assertThat(table.unpackValueIndex(packed, "waterlogged")).isEqualTo(1);
        assertThat(table.unpackValueIndex(packed, 2)).isEqualTo(0);
        assertThat(table.unpackValueIndex(packed, "powered")).isEqualTo(0);

        // Mutate facing to 5 (down)
        long updated = table.withValue(packed, 0, 5);
        assertThat(table.unpackValueIndex(updated, 0)).isEqualTo(5);
        assertThat(table.unpackValueIndex(updated, 1)).isEqualTo(1);
        assertThat(table.unpackValueIndex(updated, 2)).isEqualTo(0);
    }

    @Test
    @DisplayName("CompactStatePropertyTable enforces boundary validations")
    void testBoundaryValidations() {
        Map<String, Integer> properties = new LinkedHashMap<>();
        properties.put("facing", 6);

        CompactStatePropertyTable table = new CompactStatePropertyTable(properties);

        assertThatThrownBy(() -> table.packState(new int[]{6}))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> table.packState(new int[]{-1}))
                .isInstanceOf(IndexOutOfBoundsException.class);

        assertThatThrownBy(() -> table.packState(new int[]{1, 2}))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> table.unpackValueIndex(0L, "unknown_prop"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
