package net.minecraftforge.common;

import java.util.Objects;

/**
 * Forge configuration entry holder ({@code net.minecraftforge.common.ConfigValue}) representing
 * a typed configuration property defined on a {@link ForgeConfigSpec}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> configuration value type
 * @version 1.0.0
 */
public class ConfigValue<T> {

    private final String path;
    private final T defaultValue;
    private volatile T currentValue;
    private final String comment;

    /**
     * Constructs a new {@code ConfigValue} with the specified dot-delimited path, default value,
     * and documentation comment.
     *
     * @param path         dot-delimited configuration path
     * @param defaultValue non-null default configuration value
     * @param comment      descriptive comment (may be empty or {@code null})
     */
    public ConfigValue(String path, T defaultValue, String comment) {
        this.path = Objects.requireNonNull(path, "path must not be null");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        this.currentValue = defaultValue;
        this.comment = comment != null ? comment : "";
    }

    /**
     * Returns the dot-delimited configuration key path.
     *
     * @return non-null configuration path
     */
    public final String getPath() {
        return path;
    }

    /**
     * Returns the default value configured for this entry.
     *
     * @return non-null default value
     */
    public final T getDefault() {
        return defaultValue;
    }

    /**
     * Returns the currently active value for this configuration entry, falling back to
     * {@link #getDefault()} if unset.
     *
     * @return non-null current configuration value
     */
    public T get() {
        T current = currentValue;
        return current != null ? current : defaultValue;
    }

    /**
     * Updates the current value of this configuration entry.
     *
     * @param value non-null new value
     */
    public void set(T value) {
        this.currentValue = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Returns the documentation comment associated with this configuration entry.
     *
     * @return non-null comment string
     */
    public final String getComment() {
        return comment;
    }
}
