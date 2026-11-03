package net.minecraft.nbt;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hierarchical key-value NBT map tag ({@code net.minecraft.nbt.CompoundTag}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CompoundTag implements Tag {

    private final Map<String, Object> tags = new ConcurrentHashMap<>();

    /**
     * Constructs an empty {@code CompoundTag}.
     */
    public CompoundTag() {
    }

    @Override
    public byte getId() {
        return TAG_COMPOUND;
    }

    /**
     * Returns an unmodifiable view of all keys present in this compound tag.
     *
     * @return key set
     */
    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(tags.keySet());
    }

    /**
     * Returns the number of entries stored in this compound tag.
     *
     * @return entry count
     */
    public int size() {
        return tags.size();
    }

    /**
     * Returns whether this compound tag has no entries.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return tags.isEmpty();
    }

    /**
     * Checks whether an entry is present for the given key.
     *
     * @param key entry key
     * @return {@code true} if present
     */
    public boolean contains(String key) {
        return key != null && tags.containsKey(key);
    }

    /**
     * Stores an integer value under the specified key.
     *
     * @param key   non-null key
     * @param value integer value
     */
    public void putInt(String key, int value) {
        tags.put(Objects.requireNonNull(key, "key must not be null"), value);
    }

    /**
     * Retrieves an integer value by key, returning {@code 0} if absent or non-numeric.
     *
     * @param key entry key
     * @return integer value or {@code 0}
     */
    public int getInt(String key) {
        Object v = key != null ? tags.get(key) : null;
        return v instanceof Number n ? n.intValue() : 0;
    }

    /**
     * Stores a long value under the specified key.
     *
     * @param key   non-null key
     * @param value long value
     */
    public void putLong(String key, long value) {
        tags.put(Objects.requireNonNull(key, "key must not be null"), value);
    }

    /**
     * Retrieves a long value by key, returning {@code 0L} if absent or non-numeric.
     *
     * @param key entry key
     * @return long value or {@code 0L}
     */
    public long getLong(String key) {
        Object v = key != null ? tags.get(key) : null;
        return v instanceof Number n ? n.longValue() : 0L;
    }

    /**
     * Stores a double value under the specified key.
     *
     * @param key   non-null key
     * @param value double value
     */
    public void putDouble(String key, double value) {
        tags.put(Objects.requireNonNull(key, "key must not be null"), value);
    }

    /**
     * Retrieves a double value by key, returning {@code 0.0D} if absent or non-numeric.
     *
     * @param key entry key
     * @return double value or {@code 0.0D}
     */
    public double getDouble(String key) {
        Object v = key != null ? tags.get(key) : null;
        return v instanceof Number n ? n.doubleValue() : 0.0D;
    }

    /**
     * Stores a boolean value under the specified key.
     *
     * @param key   non-null key
     * @param value boolean value
     */
    public void putBoolean(String key, boolean value) {
        tags.put(Objects.requireNonNull(key, "key must not be null"), value);
    }

    /**
     * Retrieves a boolean value by key, returning {@code false} if absent.
     *
     * @param key entry key
     * @return boolean value
     */
    public boolean getBoolean(String key) {
        Object v = key != null ? tags.get(key) : null;
        return Boolean.TRUE.equals(v) || (v instanceof Number n && n.intValue() != 0);
    }

    /**
     * Stores a string value under the specified key.
     *
     * @param key   non-null key
     * @param value non-null string value
     */
    public void putString(String key, String value) {
        tags.put(
                Objects.requireNonNull(key, "key must not be null"),
                Objects.requireNonNull(value, "value must not be null"));
    }

    /**
     * Retrieves a string value by key, returning {@code ""} if absent or not a string.
     *
     * @param key entry key
     * @return string value or {@code ""}
     */
    public String getString(String key) {
        Object v = key != null ? tags.get(key) : null;
        return v instanceof String s ? s : "";
    }

    /**
     * Stores a nested {@link Tag} under the specified key.
     *
     * @param key   non-null key
     * @param value non-null tag value
     */
    public void put(String key, Tag value) {
        tags.put(
                Objects.requireNonNull(key, "key must not be null"),
                Objects.requireNonNull(value, "value must not be null"));
    }

    /**
     * Retrieves a nested {@link Tag} by key, or {@code null} if not a {@link Tag}.
     *
     * @param key entry key
     * @return nested tag or {@code null}
     */
    public Tag get(String key) {
        Object v = key != null ? tags.get(key) : null;
        return v instanceof Tag t ? t : null;
    }

    /**
     * Retrieves a nested {@code CompoundTag} by key, or returns a new empty {@code CompoundTag} if
     * absent.
     *
     * @param key entry key
     * @return nested {@code CompoundTag}
     */
    public CompoundTag getCompound(String key) {
        Object v = key != null ? tags.get(key) : null;
        return v instanceof CompoundTag ct ? ct : new CompoundTag();
    }

    /**
     * Removes any value stored under the specified key.
     *
     * @param key entry key to remove
     */
    public void remove(String key) {
        if (key != null) {
            tags.remove(key);
        }
    }

    @Override
    public String getAsString() {
        return tags.toString();
    }

    @Override
    public CompoundTag copy() {
        CompoundTag copied = new CompoundTag();
        for (Map.Entry<String, Object> entry : tags.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Tag tagVal) {
                copied.tags.put(entry.getKey(), tagVal.copy());
            } else {
                copied.tags.put(entry.getKey(), val);
            }
        }
        return copied;
    }

    @Override
    public String toString() {
        return getAsString();
    }
}
