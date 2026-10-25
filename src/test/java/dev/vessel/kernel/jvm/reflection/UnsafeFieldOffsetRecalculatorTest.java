package dev.vessel.kernel.jvm.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying Unsafe memory offset recalculation and dynamic cache synchronization.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class UnsafeFieldOffsetRecalculatorTest {

    @Test
    @DisplayName("UnsafeFieldOffsetRecalculator computes positive memory offsets for instance and static fields")
    public void testOffsetCalculation() throws NoSuchFieldException {
        UnsafeFieldOffsetRecalculator recalculator = new UnsafeFieldOffsetRecalculator();

        FieldOffsetEntry entryA = recalculator.recalculate(SampleFieldDummyTarget.class, "instanceFieldA");
        assertThat(entryA).isNotNull();
        assertThat(entryA.offset()).isGreaterThan(0L);
        assertThat(entryA.isStatic()).isFalse();

        FieldOffsetEntry entryStatic = recalculator.recalculate(SampleFieldDummyTarget.class, "staticCounter");
        assertThat(entryStatic).isNotNull();
        assertThat(entryStatic.offset()).isGreaterThan(0L);
        assertThat(entryStatic.isStatic()).isTrue();
    }

    @Test
    @DisplayName("UnsafeFieldOffsetRecalculator caches calculated offsets for subsequent queries")
    public void testCachedOffsetQuery() {
        UnsafeFieldOffsetRecalculator recalculator = new UnsafeFieldOffsetRecalculator();

        long firstQuery = recalculator.getOffset(SampleFieldDummyTarget.class, "instanceFieldB");
        long secondQuery = recalculator.getOffset(SampleFieldDummyTarget.class, "instanceFieldB");

        assertThat(firstQuery).isEqualTo(secondQuery);
        assertThat(firstQuery).isGreaterThan(0L);
    }

    @Test
    @DisplayName("UnsafeFieldOffsetRecalculator updates static cache holder field with fresh offset")
    public void testUpdateStaticCacheHolder() throws NoSuchFieldException, IllegalAccessException {
        UnsafeFieldOffsetRecalculator recalculator = new UnsafeFieldOffsetRecalculator();

        recalculator.updateStaticCacheHolder(
                SampleCacheHolder.class,
                "CACHED_FIELD_A_OFFSET",
                SampleFieldDummyTarget.class,
                "instanceFieldA"
        );

        long calculated = recalculator.getOffset(SampleFieldDummyTarget.class, "instanceFieldA");
        assertThat(SampleCacheHolder.CACHED_FIELD_A_OFFSET).isEqualTo(calculated);
    }

    @Test
    @DisplayName("UnsafeFieldOffsetRecalculator throws exception on non-existent field")
    public void testNonExistentField() {
        UnsafeFieldOffsetRecalculator recalculator = new UnsafeFieldOffsetRecalculator();

        assertThatThrownBy(() -> recalculator.getOffset(SampleFieldDummyTarget.class, "phantomField"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
