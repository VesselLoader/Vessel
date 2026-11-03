package net.minecraftforge.common;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge declarative configuration specification ({@code net.minecraftforge.common.ForgeConfigSpec})
 * supporting hierarchical sections, comments, and typed {@link ConfigValue} definitions.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ForgeConfigSpec {

    private final Map<String, ConfigValue<?>> values = new ConcurrentHashMap<>();
    private final Deque<String> sectionStack = new ArrayDeque<>();
    private volatile String pendingComment = "";
    private volatile boolean built;

    /**
     * Constructs a new {@code ForgeConfigSpec}.
     */
    public ForgeConfigSpec() {
    }

    /**
     * Attaches a documentation comment to the next configuration entry defined.
     *
     * @param comment descriptive comment string
     * @return this specification instance for fluent chaining
     */
    public ForgeConfigSpec comment(String comment) {
        this.pendingComment = comment != null ? comment : "";
        return this;
    }

    /**
     * Pushes a nested configuration section onto the active path stack.
     *
     * @param section non-null section identifier
     * @return this specification instance for fluent chaining
     */
    public ForgeConfigSpec push(String section) {
        Objects.requireNonNull(section, "section must not be null");
        String trimmed = section.trim();
        if (!trimmed.isEmpty()) {
            sectionStack.addLast(trimmed);
        }
        return this;
    }

    /**
     * Pops the innermost configuration section from the active path stack.
     *
     * @return this specification instance for fluent chaining
     */
    public ForgeConfigSpec pop() {
        if (!sectionStack.isEmpty()) {
            sectionStack.pollLast();
        }
        return this;
    }

    /**
     * Defines a generic typed configuration entry at the current section path.
     *
     * @param key          non-null entry key
     * @param defaultValue non-null default value
     * @param <T>          configuration value type
     * @return the registered {@link ConfigValue} handle
     */
    public <T> ConfigValue<T> define(String key, T defaultValue) {
        String fullPath = buildPath(key);
        ConfigValue<T> entry = new ConfigValue<>(fullPath, defaultValue, pendingComment);
        values.put(fullPath, entry);
        this.pendingComment = "";
        return entry;
    }

    /**
     * Defines a boolean configuration entry at the current section path.
     *
     * @param key          non-null entry key
     * @param defaultValue default boolean state
     * @return the registered {@link BooleanValue} handle
     */
    public BooleanValue define(String key, boolean defaultValue) {
        String fullPath = buildPath(key);
        BooleanValue entry = new BooleanValue(fullPath, defaultValue, pendingComment);
        values.put(fullPath, entry);
        this.pendingComment = "";
        return entry;
    }

    /**
     * Defines a bounded integer configuration entry at the current section path.
     *
     * @param key          non-null entry key
     * @param defaultValue default integer value
     * @param min          inclusive minimum allowed value
     * @param max          inclusive maximum allowed value
     * @return the registered {@link IntValue} handle
     */
    public IntValue defineInRange(String key, int defaultValue, int min, int max) {
        String fullPath = buildPath(key);
        IntValue entry = new IntValue(fullPath, defaultValue, min, max, pendingComment);
        values.put(fullPath, entry);
        this.pendingComment = "";
        return entry;
    }

    /**
     * Marks this configuration specification as finalized and built.
     *
     * @return this finalized {@code ForgeConfigSpec}
     */
    public ForgeConfigSpec build() {
        this.built = true;
        return this;
    }

    /**
     * Returns {@code true} if {@link #build()} has been called on this specification.
     *
     * @return {@code true} if built
     */
    public boolean isBuilt() {
        return built;
    }

    /**
     * Returns an unmodifiable view of all defined configuration entries keyed by dot-delimited path.
     *
     * @return unmodifiable map of configuration values
     */
    public Map<String, ConfigValue<?>> getValues() {
        return Collections.unmodifiableMap(values);
    }

    private String buildPath(String key) {
        Objects.requireNonNull(key, "key must not be null");
        String trimmedKey = key.trim();
        if (sectionStack.isEmpty()) {
            return trimmedKey;
        }
        StringBuilder sb = new StringBuilder();
        for (String sec : sectionStack) {
            sb.append(sec).append('.');
        }
        sb.append(trimmedKey);
        return sb.toString();
    }
}
