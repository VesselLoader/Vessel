package net.minecraftforge.common;

/**
 * Forge boolean configuration entry ({@code net.minecraftforge.common.BooleanValue}) extending
 * {@link ConfigValue}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BooleanValue extends ConfigValue<Boolean> {

    /**
     * Constructs a new {@code BooleanValue} for the specified path, default boolean state, and comment.
     *
     * @param path         dot-delimited configuration path
     * @param defaultValue default boolean value
     * @param comment      descriptive comment
     */
    public BooleanValue(String path, boolean defaultValue, String comment) {
        super(path, defaultValue, comment);
    }

    /**
     * Returns the current boolean primitive value.
     *
     * @return current boolean value
     */
    public boolean getAsBoolean() {
        return Boolean.TRUE.equals(get());
    }
}
