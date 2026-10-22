package dev.vessel.kernel.network.bedrock;

import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;

/**
 * High-performance, primitive bidirectional lookup table mapping Java Entity IDs (int)
 * to Bedrock Runtime Entity IDs (long) with zero object allocation during lookups.
 * <p>
 * Employs open-addressing primitive arrays with Knuth/Murmur hash scrambling, optimistic read locks,
 * and bidirectional consistency enforcement for high-throughput packet routing.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class BedrockRuntimeEntityIdIndex {

    public static final long BEDROCK_NOT_FOUND = -1L;
    public static final int JAVA_NOT_FOUND = -1;

    private static final byte STATE_EMPTY = 0;
    private static final byte STATE_OCCUPIED = 1;
    private static final byte STATE_REMOVED = 2;

    private static final float LOAD_FACTOR = 0.70f;
    private static final int DEFAULT_CAPACITY = 256;

    private final StampedLock lock = new StampedLock();

    private int capacity;
    private int mask;
    private int size;
    private int tombstoneCount;
    private int threshold;

    // Forward map: Java (int) -> Bedrock (long)
    private int[] javaKeys;
    private long[] bedrockValues;
    private byte[] forwardStates;

    // Reverse map: Bedrock (long) -> Java (int)
    private long[] bedrockKeys;
    private int[] javaValues;
    private byte[] reverseStates;

    /**
     * Constructs a new index with default initial capacity (256 entries).
     */
    public BedrockRuntimeEntityIdIndex() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a new index with specified initial capacity.
     *
     * @param initialCapacity requested capacity
     */
    public BedrockRuntimeEntityIdIndex(int initialCapacity) {
        int cap = Math.max(16, nextPowerOfTwo(initialCapacity));
        initStorage(cap);
    }

    private void initStorage(int cap) {
        this.capacity = cap;
        this.mask = cap - 1;
        this.size = 0;
        this.tombstoneCount = 0;
        this.threshold = (int) (cap * LOAD_FACTOR);

        this.javaKeys = new int[cap];
        this.bedrockValues = new long[cap];
        this.forwardStates = new byte[cap];

        this.bedrockKeys = new long[cap];
        this.javaValues = new int[cap];
        this.reverseStates = new byte[cap];
    }

    /**
     * Associates a Java Entity ID with a Bedrock Runtime Entity ID bidirectionally.
     *
     * @param javaId Java entity ID
     * @param bedrockId Bedrock runtime entity ID
     */
    public void put(int javaId, long bedrockId) {
        long stamp = lock.writeLock();
        try {
            // Check if javaId already exists
            int existingForwardIdx = findForwardIndex(javaId);
            if (existingForwardIdx >= 0) {
                long oldBedrockId = bedrockValues[existingForwardIdx];
                if (oldBedrockId == bedrockId) {
                    return; // Exact match already present
                }
                removeReverseInternal(oldBedrockId);
                removeForwardInternal(javaId);
                size--;
            }

            // Check if bedrockId already exists
            int existingReverseIdx = findReverseIndex(bedrockId);
            if (existingReverseIdx >= 0) {
                int oldJavaId = javaValues[existingReverseIdx];
                removeForwardInternal(oldJavaId);
                removeReverseInternal(bedrockId);
                size--;
            }

            if ((size + tombstoneCount) >= threshold) {
                // Rehash: expand if active size is large, compact if tombstones dominate
                rehash(size >= (threshold >> 1) ? capacity * 2 : capacity);
            }

            insertForward(javaId, bedrockId);
            insertReverse(bedrockId, javaId);
            size++;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Looks up Bedrock Runtime Entity ID for the given Java Entity ID.
     * Zero-allocation, lock-free optimistic read with graceful pessimistic fallback.
     *
     * @param javaId Java entity ID
     * @return Bedrock runtime ID, or {@link #BEDROCK_NOT_FOUND} if absent
     */
    public long getBedrockId(int javaId) {
        long stamp = lock.tryOptimisticRead();
        long result = BEDROCK_NOT_FOUND;
        boolean valid = false;

        try {
            int curMask = this.mask;
            byte[] states = this.forwardStates;
            int[] keys = this.javaKeys;
            long[] values = this.bedrockValues;

            int hash = mixInt(javaId);
            int idx = hash & curMask;
            int initialIdx = idx;

            while (true) {
                byte state = states[idx];
                if (state == STATE_EMPTY) {
                    result = BEDROCK_NOT_FOUND;
                    break;
                }
                if (state == STATE_OCCUPIED && keys[idx] == javaId) {
                    result = values[idx];
                    break;
                }
                idx = (idx + 1) & curMask;
                if (idx == initialIdx) {
                    result = BEDROCK_NOT_FOUND;
                    break;
                }
            }
            valid = lock.validate(stamp);
        } catch (Throwable ignored) {
            valid = false;
        }

        if (!valid) {
            stamp = lock.readLock();
            try {
                result = getBedrockIdInternal(javaId);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    private long getBedrockIdInternal(int javaId) {
        int idx = findForwardIndex(javaId);
        return (idx >= 0) ? bedrockValues[idx] : BEDROCK_NOT_FOUND;
    }

    /**
     * Looks up Java Entity ID for the given Bedrock Runtime Entity ID.
     * Zero-allocation, lock-free optimistic read with graceful pessimistic fallback.
     *
     * @param bedrockId Bedrock runtime entity ID
     * @return Java entity ID, or {@link #JAVA_NOT_FOUND} if absent
     */
    public int getJavaId(long bedrockId) {
        long stamp = lock.tryOptimisticRead();
        int result = JAVA_NOT_FOUND;
        boolean valid = false;

        try {
            int curMask = this.mask;
            byte[] states = this.reverseStates;
            long[] keys = this.bedrockKeys;
            int[] values = this.javaValues;

            int hash = mixLong(bedrockId);
            int idx = hash & curMask;
            int initialIdx = idx;

            while (true) {
                byte state = states[idx];
                if (state == STATE_EMPTY) {
                    result = JAVA_NOT_FOUND;
                    break;
                }
                if (state == STATE_OCCUPIED && keys[idx] == bedrockId) {
                    result = values[idx];
                    break;
                }
                idx = (idx + 1) & curMask;
                if (idx == initialIdx) {
                    result = JAVA_NOT_FOUND;
                    break;
                }
            }
            valid = lock.validate(stamp);
        } catch (Throwable ignored) {
            valid = false;
        }

        if (!valid) {
            stamp = lock.readLock();
            try {
                result = getJavaIdInternal(bedrockId);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    private int getJavaIdInternal(long bedrockId) {
        int idx = findReverseIndex(bedrockId);
        return (idx >= 0) ? javaValues[idx] : JAVA_NOT_FOUND;
    }

    /**
     * Checks if the index contains a mapping for the specified Java Entity ID.
     *
     * @param javaId Java entity ID
     * @return true if present
     */
    public boolean containsJavaId(int javaId) {
        return getBedrockId(javaId) != BEDROCK_NOT_FOUND;
    }

    /**
     * Checks if the index contains a mapping for the specified Bedrock Runtime Entity ID.
     *
     * @param bedrockId Bedrock runtime entity ID
     * @return true if present
     */
    public boolean containsBedrockId(long bedrockId) {
        return getJavaId(bedrockId) != JAVA_NOT_FOUND;
    }

    /**
     * Removes mapping associated with the given Java Entity ID.
     *
     * @param javaId Java entity ID
     * @return true if an entry was removed
     */
    public boolean removeByJavaId(int javaId) {
        long stamp = lock.writeLock();
        try {
            int idx = findForwardIndex(javaId);
            if (idx < 0) {
                return false;
            }
            long bedrockId = bedrockValues[idx];
            removeForwardInternal(javaId);
            removeReverseInternal(bedrockId);
            size--;
            return true;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Removes mapping associated with the given Bedrock Runtime Entity ID.
     *
     * @param bedrockId Bedrock runtime entity ID
     * @return true if an entry was removed
     */
    public boolean removeByBedrockId(long bedrockId) {
        long stamp = lock.writeLock();
        try {
            int idx = findReverseIndex(bedrockId);
            if (idx < 0) {
                return false;
            }
            int javaId = javaValues[idx];
            removeReverseInternal(bedrockId);
            removeForwardInternal(javaId);
            size--;
            return true;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns total number of active bidirectional mappings.
     *
     * @return entry count
     */
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int curSize = this.size;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                curSize = this.size;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return curSize;
    }

    /**
     * Returns whether the index contains zero active mappings.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Resets the entire index, discarding all mappings without reallocating arrays.
     */
    public void clear() {
        long stamp = lock.writeLock();
        try {
            Arrays.fill(forwardStates, STATE_EMPTY);
            Arrays.fill(reverseStates, STATE_EMPTY);
            size = 0;
            tombstoneCount = 0;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Returns current allocated capacity.
     *
     * @return capacity
     */
    public int getCapacity() {
        return capacity;
    }

    // -------------------------------------------------------------------------
    // Internal Hash & Probe Helpers
    // -------------------------------------------------------------------------

    private int findForwardIndex(int javaId) {
        int hash = mixInt(javaId);
        int idx = hash & mask;
        int initialIdx = idx;

        while (true) {
            byte state = forwardStates[idx];
            if (state == STATE_EMPTY) {
                return -1;
            }
            if (state == STATE_OCCUPIED && javaKeys[idx] == javaId) {
                return idx;
            }
            idx = (idx + 1) & mask;
            if (idx == initialIdx) {
                return -1;
            }
        }
    }

    private void insertForward(int javaId, long bedrockId) {
        int hash = mixInt(javaId);
        int idx = hash & mask;

        while (true) {
            byte state = forwardStates[idx];
            if (state != STATE_OCCUPIED) {
                if (state == STATE_REMOVED) {
                    tombstoneCount--;
                }
                forwardStates[idx] = STATE_OCCUPIED;
                javaKeys[idx] = javaId;
                bedrockValues[idx] = bedrockId;
                return;
            }
            if (javaKeys[idx] == javaId) {
                bedrockValues[idx] = bedrockId;
                return;
            }
            idx = (idx + 1) & mask;
        }
    }

    private void removeForwardInternal(int javaId) {
        int idx = findForwardIndex(javaId);
        if (idx >= 0) {
            forwardStates[idx] = STATE_REMOVED;
            tombstoneCount++;
        }
    }

    private int findReverseIndex(long bedrockId) {
        int hash = mixLong(bedrockId);
        int idx = hash & mask;
        int initialIdx = idx;

        while (true) {
            byte state = reverseStates[idx];
            if (state == STATE_EMPTY) {
                return -1;
            }
            if (state == STATE_OCCUPIED && bedrockKeys[idx] == bedrockId) {
                return idx;
            }
            idx = (idx + 1) & mask;
            if (idx == initialIdx) {
                return -1;
            }
        }
    }

    private void insertReverse(long bedrockId, int javaId) {
        int hash = mixLong(bedrockId);
        int idx = hash & mask;

        while (true) {
            byte state = reverseStates[idx];
            if (state != STATE_OCCUPIED) {
                reverseStates[idx] = STATE_OCCUPIED;
                bedrockKeys[idx] = bedrockId;
                javaValues[idx] = javaId;
                return;
            }
            if (bedrockKeys[idx] == bedrockId) {
                javaValues[idx] = javaId;
                return;
            }
            idx = (idx + 1) & mask;
        }
    }

    private void removeReverseInternal(long bedrockId) {
        int idx = findReverseIndex(bedrockId);
        if (idx >= 0) {
            reverseStates[idx] = STATE_REMOVED;
        }
    }

    private void rehash(int newCapacity) {
        int oldCap = this.capacity;
        int[] oldJavaKeys = this.javaKeys;
        long[] oldBedrockValues = this.bedrockValues;
        byte[] oldForwardStates = this.forwardStates;

        initStorage(newCapacity);

        for (int i = 0; i < oldCap; i++) {
            if (oldForwardStates[i] == STATE_OCCUPIED) {
                int javaId = oldJavaKeys[i];
                long bedrockId = oldBedrockValues[i];
                insertForward(javaId, bedrockId);
                insertReverse(bedrockId, javaId);
                size++;
            }
        }
    }

    private static int mixInt(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private static int mixLong(long v) {
        v ^= v >>> 33;
        v *= 0xff51afd7ed558ccdL;
        v ^= v >>> 33;
        v *= 0xc4ceb9fe1a85ec53L;
        v ^= v >>> 33;
        return (int) v;
    }

    private static int nextPowerOfTwo(int n) {
        if (n <= 0) return 16;
        int count = n - 1;
        count |= count >>> 1;
        count |= count >>> 2;
        count |= count >>> 4;
        count |= count >>> 8;
        count |= count >>> 16;
        return count + 1;
    }
}
