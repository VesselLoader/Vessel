package net.minecraftforge.common.util;

/**
 * Forge non-null value supplier functional interface ({@code net.minecraftforge.common.util.NonNullSupplier}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> supplied value type
 * @version 1.0.0
 */
@FunctionalInterface
public interface NonNullSupplier<T> {

    /**
     * Supplies a non-null result.
     *
     * @return non-null value
     */
    T get();
}
