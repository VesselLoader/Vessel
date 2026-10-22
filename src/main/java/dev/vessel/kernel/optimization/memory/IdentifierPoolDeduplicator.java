package dev.vessel.kernel.optimization.memory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * FerriteCore-inspired memory deduplicator for namespaces, paths, and identifiers.
 * <p>
 * In large modded Minecraft environments, duplicate resource location strings and
 * registry namespaces consume tens to hundreds of megabytes of JVM heap.
 * This class interns identical string instances into a canonical memory pool,
 * ensuring duplicate keys point to the exact same heap reference.
 *
 * @version 1.0.0
 */
public final class IdentifierPoolDeduplicator {

    private final ConcurrentHashMap<String, String> stringPool;
    private final AtomicLong totalRequests;
    private final AtomicLong deduplicatedHits;
    private final AtomicLong estimatedSavedBytes;

    /**
     * Constructs a new deduplication pool with an initial default capacity.
     */
    public IdentifierPoolDeduplicator() {
        this(4096);
    }

    /**
     * Constructs a new deduplication pool with the specified initial capacity.
     *
     * @param initialCapacity initial bucket capacity
     */
    public IdentifierPoolDeduplicator(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be positive: " + initialCapacity);
        }
        this.stringPool = new ConcurrentHashMap<>(initialCapacity);
        this.totalRequests = new AtomicLong(0L);
        this.deduplicatedHits = new AtomicLong(0L);
        this.estimatedSavedBytes = new AtomicLong(0L);
    }

    /**
     * Canonicalizes and deduplicates a string.
     *
     * @param value the raw string to deduplicate
     * @return the canonical interned string instance
     */
    public String deduplicate(String value) {
        if (value == null) {
            return null;
        }
        totalRequests.incrementAndGet();

        String existing = stringPool.putIfAbsent(value, value);
        if (existing != null) {
            deduplicatedHits.incrementAndGet();
            // Estimate 2 bytes per char + object overhead saved
            estimatedSavedBytes.addAndGet(value.length() * 2L + 24L);
            return existing;
        }
        return value;
    }

    /**
     * Canonicalizes an identifier formed by a namespace and a path.
     *
     * @param namespace the resource namespace (e.g., "minecraft", "fabric")
     * @param path      the resource path (e.g., "stone", "block/dirt")
     * @return canonical formatted identifier "namespace:path"
     */
    public String deduplicateIdentifier(String namespace, String path) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(path, "path cannot be null");

        String canonicalNamespace = deduplicate(namespace);
        String canonicalPath = deduplicate(path);

        String combined = canonicalNamespace + ":" + canonicalPath;
        return deduplicate(combined);
    }

    /**
     * Returns the total number of deduplication requests processed.
     *
     * @return total requests
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }

    /**
     * Returns the number of requests that matched an existing canonical string.
     *
     * @return hit count
     */
    public long getDeduplicatedHits() {
        return deduplicatedHits.get();
    }

    /**
     * Returns the number of unique canonical strings currently held in the pool.
     *
     * @return pool size
     */
    public int getUniqueCount() {
        return stringPool.size();
    }

    /**
     * Returns the estimated heap memory saved in bytes.
     *
     * @return saved bytes
     */
    public long getEstimatedSavedBytes() {
        return estimatedSavedBytes.get();
    }

    /**
     * Clears all cached strings in the pool.
     */
    public void clear() {
        stringPool.clear();
    }
}
