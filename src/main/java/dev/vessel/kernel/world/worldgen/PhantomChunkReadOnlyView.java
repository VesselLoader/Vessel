package dev.vessel.kernel.world.worldgen;

import dev.vessel.kernel.tick.RegionalChunkCoordinateHasher;

import java.util.Objects;

/**
 * Read-Only Phantom View providing safe, non-recursive inspection of adjacent neighbor chunks.
 * <p>
 * Eliminates cascading world generation deadlocks (ERR-34 and WGN-0251..0500)
 * by allowing feature decorators to query neighbor block states, biomes, and heightmaps
 * without ever issuing chunk load tickets or escalating to synchronous chunk generation.
 * </p>
 * <p>
 * Any attempt to write or mutate this view throws {@link UnsupportedOperationException}.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PhantomChunkReadOnlyView {

    /**
     * Default sea level used for synthetic terrain fallback calculations.
     */
    public static final int DEFAULT_SEA_LEVEL = 63;

    /**
     * Default plains biome identifier for synthetic fallbacks.
     */
    public static final int DEFAULT_PLAINS_BIOME_ID = 1;

    /**
     * Minimum valid world height coordinate in standard modern Minecraft.
     */
    public static final int MIN_WORLD_HEIGHT = -64;

    /**
     * Maximum valid world height coordinate in standard modern Minecraft.
     */
    public static final int MAX_WORLD_HEIGHT = 320;

    private final int chunkX;
    private final int chunkZ;
    private final long chunkKey;
    private final PhantomChunkAccessMode accessMode;
    private final PhantomChunkProvider provider;
    private final int seaLevel;

    /**
     * Constructs a phantom view for the specified chunk coordinates using synthetic terrain mode.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     */
    public PhantomChunkReadOnlyView(int chunkX, int chunkZ) {
        this(chunkX, chunkZ, PhantomChunkAccessMode.SYNTHETIC_TERRAIN, null, DEFAULT_SEA_LEVEL);
    }

    /**
     * Constructs a phantom view with custom access mode and optional loaded chunk provider.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param accessMode fallback and access mode
     * @param provider optional loaded chunk provider (may be null)
     */
    public PhantomChunkReadOnlyView(int chunkX, int chunkZ, PhantomChunkAccessMode accessMode, PhantomChunkProvider provider) {
        this(chunkX, chunkZ, accessMode, provider, DEFAULT_SEA_LEVEL);
    }

    /**
     * Full constructor for PhantomChunkReadOnlyView.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param accessMode fallback and access mode
     * @param provider optional loaded chunk provider (may be null)
     * @param seaLevel reference sea level for synthetic terrain
     */
    public PhantomChunkReadOnlyView(
            int chunkX,
            int chunkZ,
            PhantomChunkAccessMode accessMode,
            PhantomChunkProvider provider,
            int seaLevel
    ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkKey = RegionalChunkCoordinateHasher.toPackedChunkKey(chunkX, chunkZ);
        this.accessMode = Objects.requireNonNull(accessMode, "accessMode cannot be null");
        this.provider = provider;
        this.seaLevel = seaLevel;
    }

    /**
     * Queries the block snapshot at local chunk coordinates (localX, y, localZ).
     *
     * @param localX block X within chunk (0..15)
     * @param y world Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return immutable block snapshot
     */
    public PhantomBlockSnapshot getBlock(int localX, int y, int localZ) {
        validateCoordinates(localX, y, localZ);

        if (accessMode == PhantomChunkAccessMode.DELEGATE_IF_LOADED && provider != null && provider.isChunkLoaded(chunkKey)) {
            PhantomBlockSnapshot snapshot = provider.getBlock(chunkKey, localX, y, localZ);
            if (snapshot != null) {
                return snapshot;
            }
        }

        return switch (accessMode) {
            case SYNTHETIC_AIR, SYNTHETIC_VOID -> new PhantomBlockSnapshot(
                    localX, y, localZ, "minecraft:air", 0, 15
            );
            case SYNTHETIC_TERRAIN, DELEGATE_IF_LOADED -> {
                if (y <= seaLevel) {
                    yield new PhantomBlockSnapshot(localX, y, localZ, "minecraft:stone", 1, 0);
                } else {
                    yield new PhantomBlockSnapshot(localX, y, localZ, "minecraft:air", 0, 15);
                }
            }
        };
    }

    /**
     * Queries the namespaced block identifier at the given location.
     *
     * @param localX block X within chunk (0..15)
     * @param y world Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return namespaced block identifier string
     */
    public String getBlockId(int localX, int y, int localZ) {
        return getBlock(localX, y, localZ).blockId();
    }

    /**
     * Queries the integer block state ID at the given location.
     *
     * @param localX block X within chunk (0..15)
     * @param y world Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return block state integer ID
     */
    public int getBlockStateId(int localX, int y, int localZ) {
        return getBlock(localX, y, localZ).blockStateId();
    }

    /**
     * Queries the light level (0..15) at the given location.
     *
     * @param localX block X within chunk (0..15)
     * @param y world Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return light level
     */
    public int getLightLevel(int localX, int y, int localZ) {
        return getBlock(localX, y, localZ).lightLevel();
    }

    /**
     * Queries the biome ID at the given column.
     *
     * @param localX block X within chunk (0..15)
     * @param y world Y coordinate
     * @param localZ block Z within chunk (0..15)
     * @return biome identifier
     */
    public int getBiomeId(int localX, int y, int localZ) {
        validateCoordinates(localX, y, localZ);

        if (accessMode == PhantomChunkAccessMode.DELEGATE_IF_LOADED && provider != null && provider.isChunkLoaded(chunkKey)) {
            int biome = provider.getBiomeId(chunkKey, localX, y, localZ);
            if (biome >= 0) {
                return biome;
            }
        }

        return DEFAULT_PLAINS_BIOME_ID;
    }

    /**
     * Queries the highest surface terrain height at the given column.
     *
     * @param localX block X within chunk (0..15)
     * @param localZ block Z within chunk (0..15)
     * @return highest non-air block Y coordinate
     */
    public int getHeight(int localX, int localZ) {
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            throw new IndexOutOfBoundsException("Column coordinates must be 0..15");
        }

        if (accessMode == PhantomChunkAccessMode.DELEGATE_IF_LOADED && provider != null && provider.isChunkLoaded(chunkKey)) {
            int height = provider.getHeight(chunkKey, localX, localZ);
            if (height >= MIN_WORLD_HEIGHT) {
                return height;
            }
        }

        return switch (accessMode) {
            case SYNTHETIC_AIR, SYNTHETIC_VOID -> MIN_WORLD_HEIGHT;
            case SYNTHETIC_TERRAIN, DELEGATE_IF_LOADED -> seaLevel;
        };
    }

    /**
     * Mutation attempt rejected: phantom views are strictly read-only.
     *
     * @param localX block X within chunk
     * @param y world Y coordinate
     * @param localZ block Z within chunk
     * @param blockId target block identifier
     * @throws UnsupportedOperationException always
     */
    public void setBlock(int localX, int y, int localZ, String blockId) {
        throw new UnsupportedOperationException(
                "PhantomChunkReadOnlyView is strictly immutable and read-only to prevent cascading chunk generation deadlocks"
        );
    }

    /**
     * Always returns {@code true} indicating this view cannot mutate world state.
     *
     * @return {@code true}
     */
    public boolean isReadOnly() {
        return true;
    }

    /**
     * Returns the chunk X coordinate.
     *
     * @return chunk X
     */
    public int chunkX() {
        return chunkX;
    }

    /**
     * Returns the chunk Z coordinate.
     *
     * @return chunk Z
     */
    public int chunkZ() {
        return chunkZ;
    }

    /**
     * Returns the packed 64-bit chunk key.
     *
     * @return 64-bit packed key
     */
    public long chunkKey() {
        return chunkKey;
    }

    /**
     * Returns the configured access mode for this view.
     *
     * @return phantom access mode
     */
    public PhantomChunkAccessMode accessMode() {
        return accessMode;
    }

    private void validateCoordinates(int localX, int y, int localZ) {
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) {
            throw new IndexOutOfBoundsException("Local chunk coordinates must be in range 0..15");
        }
        if (y < MIN_WORLD_HEIGHT || y > MAX_WORLD_HEIGHT) {
            throw new IndexOutOfBoundsException("World Y coordinate " + y + " outside valid bounds [" + MIN_WORLD_HEIGHT + ".." + MAX_WORLD_HEIGHT + "]");
        }
    }
}
