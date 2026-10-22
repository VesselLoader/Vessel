package dev.vessel.kernel.actor.primitive;

import java.util.Arrays;
import java.util.Objects;

/**
 * High-performance, primitive-keyed {@code long -> Object} hash map using open addressing
 * with linear probing.
 * <p>
 * Eliminates all autoboxing overhead and Node object allocations, providing sub-nanosecond
 * spatial lookups for {@link dev.vessel.kernel.actor.RegionCoordinateHash#toPackedKey()} in the
 * server tick loop.
 * </p>
 *
 * @param <V> value type
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public class LongPrimitive2ObjectMap<V> {

    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final long EMPTY_KEY = 0L;

    private long[] keys;
    private Object[] values;
    private boolean hasZeroKey;
    private Object zeroValue;
    private int size;
    private int mask;
    private int threshold;

    public LongPrimitive2ObjectMap(int initialCapacity) {
        int cap = 1;
        while (cap < initialCapacity) {
            cap <<= 1;
        }
        this.keys = new long[cap];
        this.values = new Object[cap];
        this.mask = cap - 1;
        this.threshold = (int) (cap * DEFAULT_LOAD_FACTOR);
    }

    public LongPrimitive2ObjectMap() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Associates the specified value with the primitive long key.
     *
     * @param key primitive 64-bit key
     * @param value value to associate
     * @return previous value or null
     */
    @SuppressWarnings("unchecked")
    public V put(long key, V value) {
        Objects.requireNonNull(value, "value cannot be null");

        if (key == EMPTY_KEY) {
            V old = (V) zeroValue;
            zeroValue = value;
            if (!hasZeroKey) {
                hasZeroKey = true;
                size++;
            }
            return old;
        }

        int index = mixKey(key) & mask;
        while (true) {
            long existingKey = keys[index];
            if (existingKey == EMPTY_KEY) {
                keys[index] = key;
                values[index] = value;
                size++;
                if (size >= threshold) {
                    rehash(keys.length << 1);
                }
                return null;
            } else if (existingKey == key) {
                V old = (V) values[index];
                values[index] = value;
                return old;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Returns the value associated with the primitive long key. Allocation-free.
     *
     * @param key primitive 64-bit key
     * @return value or null if not present
     */
    @SuppressWarnings("unchecked")
    public V get(long key) {
        if (key == EMPTY_KEY) {
            return hasZeroKey ? (V) zeroValue : null;
        }

        int index = mixKey(key) & mask;
        while (true) {
            long existingKey = keys[index];
            if (existingKey == EMPTY_KEY) {
                return null;
            }
            if (existingKey == key) {
                return (V) values[index];
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Checks if the map contains an entry for the primitive key.
     *
     * @param key primitive key
     * @return true if present
     */
    public boolean containsKey(long key) {
        return get(key) != null;
    }

    /**
     * Removes the entry for the specified primitive key.
     *
     * @param key primitive key
     * @return previous value or null
     */
    @SuppressWarnings("unchecked")
    public V remove(long key) {
        if (key == EMPTY_KEY) {
            if (hasZeroKey) {
                V old = (V) zeroValue;
                zeroValue = null;
                hasZeroKey = false;
                size--;
                return old;
            }
            return null;
        }

        int index = mixKey(key) & mask;
        while (true) {
            long existingKey = keys[index];
            if (existingKey == EMPTY_KEY) {
                return null;
            }
            if (existingKey == key) {
                V old = (V) values[index];
                keys[index] = EMPTY_KEY;
                values[index] = null;
                size--;
                rehashCluster(index);
                return old;
            }
            index = (index + 1) & mask;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        Arrays.fill(keys, EMPTY_KEY);
        Arrays.fill(values, null);
        hasZeroKey = false;
        zeroValue = null;
        size = 0;
    }

    private static int mixKey(long key) {
        long h = key ^ (key >>> 32);
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return (int) h;
    }

    @SuppressWarnings("unchecked")
    private void rehash(int newCapacity) {
        long[] oldKeys = keys;
        Object[] oldValues = values;

        this.keys = new long[newCapacity];
        this.values = new Object[newCapacity];
        this.mask = newCapacity - 1;
        this.threshold = (int) (newCapacity * DEFAULT_LOAD_FACTOR);

        for (int i = 0; i < oldKeys.length; i++) {
            long k = oldKeys[i];
            if (k != EMPTY_KEY) {
                int idx = mixKey(k) & mask;
                while (keys[idx] != EMPTY_KEY) {
                    idx = (idx + 1) & mask;
                }
                keys[idx] = k;
                values[idx] = oldValues[i];
            }
        }
    }

    private void rehashCluster(int emptyIndex) {
        int idx = (emptyIndex + 1) & mask;
        while (keys[idx] != EMPTY_KEY) {
            long keyToRehash = keys[idx];
            Object valToRehash = values[idx];
            keys[idx] = EMPTY_KEY;
            values[idx] = null;

            int reinsertedIdx = mixKey(keyToRehash) & mask;
            while (keys[reinsertedIdx] != EMPTY_KEY) {
                reinsertedIdx = (reinsertedIdx + 1) & mask;
            }
            keys[reinsertedIdx] = keyToRehash;
            values[reinsertedIdx] = valToRehash;

            idx = (idx + 1) & mask;
        }
    }
}
