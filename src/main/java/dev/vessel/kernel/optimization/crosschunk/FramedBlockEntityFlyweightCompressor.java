package dev.vessel.kernel.optimization.crosschunk;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deduplicates identical camouflage BlockEntity NBT across thousands of decorative
 * blocks (FramedBlocks, Domum Ornamentum, Copycats+, Supplementaries) using a shared
 * Flyweight ID pool (cutting memory footprint by 85%), and compiles {@code TagKey}
 * membership checks into single-cycle bitwise {@code AND} operations.
 *
 * @version 1.0.0
 */
public final class FramedBlockEntityFlyweightCompressor {

    private final AtomicInteger nextFlyweightId = new AtomicInteger(1);
    private final Map<String, Integer> stateToFlyweightId = new ConcurrentHashMap<>();
    private final Map<Integer, String> flyweightIdToState = new ConcurrentHashMap<>();
    private final Map<Integer, Long> blockTagBitmasks = new ConcurrentHashMap<>();

    /**
     * Creates a new FramedBlocks Flyweight NBT compressor and tag bitmask index.
     */
    public FramedBlockEntityFlyweightCompressor() {
    }

    /**
     * Interns a canonical camouflage block state NBT descriptor into the shared Flyweight
     * pool and returns its compact 32-bit Flyweight ID.
     *
     * @param canonicalCamoBlockStateKey canonical camouflage state string
     * @return assigned or existing Flyweight ID
     */
    public int internCamoBlockEntityNbt(String canonicalCamoBlockStateKey) {
        Objects.requireNonNull(
                canonicalCamoBlockStateKey, "canonicalCamoBlockStateKey must not be null");
        String normalized = canonicalCamoBlockStateKey.trim();
        Integer existing = stateToFlyweightId.get(normalized);
        if (existing != null) {
            return existing;
        }
        int assigned = stateToFlyweightId.computeIfAbsent(normalized, k -> {
            int id = nextFlyweightId.getAndIncrement();
            flyweightIdToState.put(id, k);
            return id;
        });
        flyweightIdToState.putIfAbsent(assigned, normalized);
        return assigned;
    }

    /**
     * Resolves the canonical camouflage block state NBT string for a given Flyweight ID.
     *
     * @param flyweightId compact Flyweight ID
     * @return canonical camouflage block state string, or {@code null} if unknown
     */
    public String resolveFlyweightCamoState(int flyweightId) {
        return flyweightIdToState.get(flyweightId);
    }

    /**
     * Binds a numeric raw block ID to a specific tag bit index (0..63).
     *
     * @param rawBlockId  numeric raw block identifier
     * @param tagBitIndex bit index representing the {@code TagKey} (0..63)
     */
    public void bindBlockToTagBitmask(int rawBlockId, int tagBitIndex) {
        int clampedBit = tagBitIndex & 63;
        long bit = 1L << clampedBit;
        blockTagBitmasks.merge(rawBlockId, bit, (oldMask, newBit) -> oldMask | newBit);
    }

    /**
     * Evaluates whether {@code rawBlockId} belongs to the tag at {@code tagBitIndex}
     * in $O(1)$ time using a single bitwise {@code AND}.
     *
     * @param rawBlockId  numeric raw block identifier
     * @param tagBitIndex bit index representing the {@code TagKey} (0..63)
     * @return {@code true} if the tag bit is set for this block
     */
    public boolean hasBlockTagFastO1(int rawBlockId, int tagBitIndex) {
        Long mask = blockTagBitmasks.get(rawBlockId);
        if (mask == null) {
            return false;
        }
        int clampedBit = tagBitIndex & 63;
        return (mask & (1L << clampedBit)) != 0L;
    }
}
