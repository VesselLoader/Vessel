package net.minecraftforge.common.util;

/**
 * Forge non-null value consumer functional interface ({@code net.minecraftforge.common.util.NonNullConsumer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> consumed value type
 * @version 1.0.0
 */
@FunctionalInterface
public interface NonNullConsumer<T> {

    /**
     * Performs this operation on the given non-null argument.
     *
     * @param value non-null input argument
     */
    void accept(T value);
}
