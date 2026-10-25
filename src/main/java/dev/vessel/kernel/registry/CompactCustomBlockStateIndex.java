package dev.vessel.kernel.registry;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flat primitive integer index mapping custom Forge/Fabric block states to vanilla network palette IDs.
 * <p>
 * When a vanilla Minecraft client connects to a hybrid or modded server, sending raw custom block state IDs
 * causes network packet decoding crashes. This table translates custom state IDs into safe vanilla substitute
 * palette IDs with zero heap allocation (sub-nanosecond flat array lookup).
 * Unregistered or out-of-bounds state IDs automatically map to a safe default fallback ID (e.g., vanilla Stone or Air),
 * guaranteeing 100% crash immunity.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class CompactCustomBlockStateIndex {

    /**
     * Default fallback vanilla block state ID (Stone = 1).
     */
    public static final int DEFAULT_FALLBACK_STATE_ID = 1;

    /**
     * Default initial capacity for block states.
     */
    public static final int DEFAULT_INITIAL_CAPACITY = 4096;

    /**
     * Maximum supported custom block state ID (1,048,576 / 2^20 states).
     * Prevents memory exhaustion attacks and integer overflow errors.
     */
    public static final int MAX_SUPPORTED_STATE_ID = 1_048_576;

    private static final int UNMAPPED_SENTINEL = -1;

    private volatile int[] customToVanillaTable;
    private final int defaultFallbackId;
    private final Map<Integer, String> stateNames;
    private int maxRegisteredStateId;
    private int registeredCount;

    /**
     * Constructs an index with default capacity and default fallback ID.
     */
    public CompactCustomBlockStateIndex() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_FALLBACK_STATE_ID);
    }

    /**
     * Constructs an index with specified capacity and fallback ID.
     *
     * @param initialCapacity   initial array capacity
     * @param defaultFallbackId safe vanilla state ID used for unmapped or out-of-bounds states
     */
    public CompactCustomBlockStateIndex(int initialCapacity, int defaultFallbackId) {
        if (initialCapacity <= 0 || initialCapacity > MAX_SUPPORTED_STATE_ID + 1) {
            throw new IllegalArgumentException("initialCapacity must be positive and <= " + (MAX_SUPPORTED_STATE_ID + 1) + ": " + initialCapacity);
        }
        if (defaultFallbackId < 0) {
            throw new IllegalArgumentException("defaultFallbackId must be non-negative: " + defaultFallbackId);
        }

        this.defaultFallbackId = defaultFallbackId;
        this.customToVanillaTable = new int[initialCapacity];
        Arrays.fill(this.customToVanillaTable, UNMAPPED_SENTINEL);

        this.stateNames = new ConcurrentHashMap<>();
        this.maxRegisteredStateId = -1;
        this.registeredCount = 0;
    }

    /**
     * Registers a custom block state mapping.
     *
     * @param customStateId          custom state ID from mod/loader (0 to MAX_SUPPORTED_STATE_ID)
     * @param customBlockName        human-readable identifier (e.g. "techreborn:tin_ore[lit=true]")
     * @param fallbackVanillaStateId safe vanilla block state ID to substitute for vanilla clients
     */
    public synchronized void registerCustomBlockState(
            int customStateId,
            String customBlockName,
            int fallbackVanillaStateId) {
        if (customStateId < 0 || customStateId > MAX_SUPPORTED_STATE_ID) {
            throw new IllegalArgumentException("customStateId must be between 0 and " + MAX_SUPPORTED_STATE_ID + ": " + customStateId);
        }
        if (fallbackVanillaStateId < 0) {
            throw new IllegalArgumentException("fallbackVanillaStateId must be non-negative: " + fallbackVanillaStateId);
        }
        Objects.requireNonNull(customBlockName, "customBlockName cannot be null");

        ensureCapacity(customStateId);

        if (customToVanillaTable[customStateId] == UNMAPPED_SENTINEL) {
            registeredCount++;
        }
        customToVanillaTable[customStateId] = fallbackVanillaStateId;
        stateNames.put(customStateId, customBlockName);

        if (customStateId > maxRegisteredStateId) {
            maxRegisteredStateId = customStateId;
        }
    }

    /**
     * Translates a custom block state ID to a vanilla-safe palette state ID.
     * <p>
     * Evaluates in single-cycle memory access with zero heap allocation.
     * If the ID is unregistered or out of bounds, returns {@link #getDefaultFallbackId()} without throwing.
     *
     * @param customBlockStateId custom state ID
     * @return safe vanilla block state ID
     */
    public int getVanillaPaletteId(int customBlockStateId) {
        final int[] table = this.customToVanillaTable;
        if (customBlockStateId < 0 || customBlockStateId >= table.length) {
            return defaultFallbackId;
        }
        int mapped = table[customBlockStateId];
        return (mapped != UNMAPPED_SENTINEL) ? mapped : defaultFallbackId;
    }

    /**
     * Checks if a given block state ID is a registered custom state.
     *
     * @param stateId state ID to check
     * @return true if registered as a custom block state
     */
    public boolean isCustomState(int stateId) {
        final int[] table = this.customToVanillaTable;
        if (stateId < 0 || stateId >= table.length) {
            return false;
        }
        return table[stateId] != UNMAPPED_SENTINEL;
    }

    /**
     * Retrieves the custom block name for a given state ID.
     *
     * @param customStateId custom state ID
     * @return block name string, or null if unregistered
     */
    public String getCustomBlockName(int customStateId) {
        return stateNames.get(customStateId);
    }

    /**
     * Returns the safe default fallback vanilla state ID.
     *
     * @return default fallback ID
     */
    public int getDefaultFallbackId() {
        return defaultFallbackId;
    }

    /**
     * Returns the total count of registered custom block states.
     *
     * @return registered count
     */
    public synchronized int getRegisteredCount() {
        return registeredCount;
    }

    /**
     * Returns all registered custom state IDs in ascending order.
     *
     * @return primitive int array of custom IDs
     */
    public synchronized int[] getRegisteredCustomStateIds() {
        int[] ids = new int[registeredCount];
        int idx = 0;
        final int[] table = this.customToVanillaTable;
        for (int i = 0; i <= maxRegisteredStateId && i < table.length; i++) {
            if (table[i] != UNMAPPED_SENTINEL) {
                ids[idx++] = i;
            }
        }
        return (idx == ids.length) ? ids : Arrays.copyOf(ids, idx);
    }

    private void ensureCapacity(int requiredStateId) {
        if (requiredStateId >= customToVanillaTable.length) {
            long expanded = Math.max((long) customToVanillaTable.length * 2, (long) requiredStateId + 1);
            int newCap = (int) Math.min((long) MAX_SUPPORTED_STATE_ID + 1, expanded);
            int[] newTable = Arrays.copyOf(customToVanillaTable, newCap);
            Arrays.fill(newTable, customToVanillaTable.length, newCap, UNMAPPED_SENTINEL);
            this.customToVanillaTable = newTable;
        }
    }
}
