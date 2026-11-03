package net.minecraftforge.common;

import java.util.Objects;

/**
 * Forge bounded integer configuration entry ({@code net.minecraftforge.common.IntValue}) extending
 * {@link ConfigValue}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class IntValue extends ConfigValue<Integer> {

    private final int minValue;
    private final int maxValue;

    /**
     * Constructs a new bounded {@code IntValue} for the specified path, default value, range bounds,
     * and comment.
     *
     * @param path         dot-delimited configuration path
     * @param defaultValue default integer value
     * @param minValue     inclusive minimum allowed value
     * @param maxValue     inclusive maximum allowed value
     * @param comment      descriptive comment
     */
    public IntValue(String path, int defaultValue, int minValue, int maxValue, String comment) {
        super(path, defaultValue, comment);
        if (minValue > maxValue) {
            throw new IllegalArgumentException(
                    "minValue (" + minValue + ") must not exceed maxValue (" + maxValue + ")");
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Updates the current integer value after verifying that it lies within
     * {@code [minValue, maxValue]}.
     *
     * @param value non-null integer value within bounds
     * @throws IllegalArgumentException if {@code value} is outside {@code [minValue, maxValue]}
     */
    @Override
    public void set(Integer value) {
        Objects.requireNonNull(value, "value must not be null");
        int intVal = value;
        if (intVal < minValue || intVal > maxValue) {
            throw new IllegalArgumentException(
                    "Value " + intVal + " for '" + getPath() + "' out of range [" + minValue + ", " + maxValue + "]");
        }
        super.set(value);
    }

    /**
     * Returns the current primitive {@code int} value.
     *
     * @return current integer value
     */
    public int getAsInt() {
        return get();
    }

    /**
     * Returns the inclusive minimum allowed integer value.
     *
     * @return minimum value
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * Returns the inclusive maximum allowed integer value.
     *
     * @return maximum value
     */
    public int getMaxValue() {
        return maxValue;
    }
}
