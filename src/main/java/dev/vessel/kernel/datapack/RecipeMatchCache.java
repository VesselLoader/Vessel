package dev.vessel.kernel.datapack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-speed caching engine for crafting and recipe matching.
 * <p>
 * Crafting grids (2x2 player inventory, 3x3 crafting tables, crafters) repeatedly query
 * recipes during inventory interactions. This cache hashes crafting grid layouts into
 * 64-bit primitive keys, serving repeated lookups in sub-nanosecond time.
 *
 * @version 1.0.0
 */
public final class RecipeMatchCache {

    private final Map<Long, Integer> cache;
    private final int maxEntries;
    private final AtomicLong hitCount;
    private final AtomicLong missCount;

    /**
     * Constructs a recipe match cache with the default maximum capacity (4096 entries).
     */
    public RecipeMatchCache() {
        this(4096);
    }

    /**
     * Constructs a recipe match cache with the specified capacity limit.
     *
     * @param maxEntries maximum entries before clearing
     */
    public RecipeMatchCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive: " + maxEntries);
        }
        this.maxEntries = maxEntries;
        this.cache = new ConcurrentHashMap<>(maxEntries);
        this.hitCount = new AtomicLong(0L);
        this.missCount = new AtomicLong(0L);
    }

    /**
     * Computes a 64-bit primitive hash for a crafting grid matrix of item IDs.
     *
     * @param itemIds array of item IDs in the grid (e.g., 4 items for 2x2, 9 items for 3x3)
     * @param width   grid width
     * @param height  grid height
     * @return 64-bit layout hash
     */
    public static long computeGridHash(int[] itemIds, int width, int height) {
        if (itemIds == null || itemIds.length == 0) {
            return 0L;
        }

        long h = ((long) width << 32) ^ height;
        for (int i = 0; i < itemIds.length; i++) {
            h = 31L * h + (itemIds[i] & 0xFFFFFFFFL);
        }
        return h;
    }

    /**
     * Retrieves the cached recipe result for a given grid hash.
     *
     * @param gridHash grid layout hash
     * @return resulting item ID, or -1 if not cached
     */
    public int get(long gridHash) {
        Integer result = cache.get(gridHash);
        if (result != null) {
            hitCount.incrementAndGet();
            return result;
        }
        missCount.incrementAndGet();
        return -1;
    }

    /**
     * Stores a computed recipe result in the cache.
     *
     * @param gridHash     grid layout hash
     * @param resultItemId resulting item ID
     */
    public void put(long gridHash, int resultItemId) {
        if (cache.size() >= maxEntries) {
            cache.clear(); // Evict on overflow to bound memory
        }
        cache.put(gridHash, resultItemId);
    }

    /**
     * Returns total cache hits.
     *
     * @return hit count
     */
    public long getHitCount() {
        return hitCount.get();
    }

    /**
     * Returns total cache misses.
     *
     * @return miss count
     */
    public long getMissCount() {
        return missCount.get();
    }

    /**
     * Returns the cache hit ratio (0.0 to 1.0).
     *
     * @return hit ratio
     */
    public double getHitRatio() {
        long total = hitCount.get() + missCount.get();
        if (total == 0L) {
            return 1.0;
        }
        return (double) hitCount.get() / (double) total;
    }

    /**
     * Clears all cached recipe matches.
     */
    public void clear() {
        cache.clear();
    }
}
