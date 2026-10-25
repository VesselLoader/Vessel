package dev.vessel.kernel.datapack;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Thread-safe mutable definition container for a Minecraft datapack tag.
 * <p>
 * Manages whether the tag replaces upstream definitions ({@code replace: true})
 * and the ordered set of entry identifiers (blocks, items, fluids, etc.).
 * Strictly isolated in its own compilation unit in accordance with Rule 17.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DatapackTagDefinition {

    private boolean replace;
    private final LinkedHashSet<String> values;

    /**
     * Constructs an empty tag definition with {@code replace = false}.
     */
    public DatapackTagDefinition() {
        this(false);
    }

    /**
     * Constructs an empty tag definition with specified replace flag.
     *
     * @param replace whether this definition replaces prior tag entries
     */
    public DatapackTagDefinition(boolean replace) {
        this.replace = replace;
        this.values = new LinkedHashSet<>();
    }

    /**
     * Returns whether this tag replaces upstream entries.
     *
     * @return true if replace is enabled
     */
    public synchronized boolean isReplace() {
        return replace;
    }

    /**
     * Sets whether this tag replaces upstream entries.
     *
     * @param replace replace flag
     */
    public synchronized void setReplace(boolean replace) {
        this.replace = replace;
    }

    /**
     * Returns an unmodifiable snapshot of the tag's member values.
     *
     * @return set of tag value strings
     */
    public synchronized Set<String> getValues() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    /**
     * Adds an entry value to the tag.
     *
     * @param value tag entry identifier (e.g. "minecraft:iron_ingot" or "#c:iron_ores")
     */
    public synchronized void addValue(String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    /**
     * Appends a collection of entry values to the tag.
     *
     * @param newValues collection of entry identifiers
     */
    public synchronized void addAllValues(Collection<String> newValues) {
        if (newValues != null) {
            for (String val : newValues) {
                addValue(val);
            }
        }
    }

    /**
     * Clears all values from this tag definition.
     */
    public synchronized void clearValues() {
        values.clear();
    }

    /**
     * Returns the count of entry values in this tag.
     *
     * @return entry count
     */
    public synchronized int size() {
        return values.size();
    }
}
