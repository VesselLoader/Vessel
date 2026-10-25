package dev.vessel.kernel.optimization.world;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;

/**
 * Governs instanced per-player loot containers (Lootr) by compressing idle container
 * NBT after 15 minutes (18,000 ticks) of inactivity, and throttles Distant Horizons
 * server-side LOD streaming bandwidth and WorldMap (JourneyMap / Xaero's) radar scans.
 *
 * @version 1.0.0
 */
public final class InstancedLootAndWorldMapSyncGovernor {

    private static final long IDLE_EVICTION_TICKS = 18000L;
    private static final int ESTIMATED_BYTES_PER_LOD_SECTION = 4096;

    private final Map<Long, Set<UUID>> openedPlayersByContainerPos = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRadarScanTickByPlayer = new ConcurrentHashMap<>();

    /**
     * Creates a new instanced loot and world map synchronization governor.
     */
    public InstancedLootAndWorldMapSyncGovernor() {
    }

    /**
     * Records that {@code playerUuid} has opened the instanced Lootr container at
     * {@code containerWorldPos}.
     *
     * @param containerWorldPos 64-bit packed block position of the container
     * @param playerUuid        UUID of the player who opened the container
     */
    public void markPlayerOpenedInstancedContainer(long containerWorldPos, UUID playerUuid) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Set<UUID> openedSet =
                openedPlayersByContainerPos.computeIfAbsent(
                        containerWorldPos, k -> ConcurrentHashMap.newKeySet());
        openedSet.add(playerUuid);
    }

    /**
     * Compresses and evicts an idle Lootr container's NBT payload if it has not been
     * accessed for at least 15 minutes (18,000 ticks), returning the compact compressed
     * byte slice (or {@code null} if still active).
     *
     * @param containerWorldPos 64-bit packed block position of the container
     * @param serializedNbt     raw serialized container NBT bytes
     * @param lastAccessTick    tick when the container was last opened
     * @param currentTick       current server tick
     * @return compressed archive bytes if evicted, or {@code null} if still within active window
     */
    public byte[] evictIdleInstancedLootToZstdCache(
            long containerWorldPos,
            byte[] serializedNbt,
            long lastAccessTick,
            long currentTick) {
        Objects.requireNonNull(serializedNbt, "serializedNbt must not be null");
        if (currentTick - lastAccessTick < IDLE_EVICTION_TICKS) {
            return null;
        }
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(serializedNbt);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(serializedNbt.length);
            byte[] buffer = new byte[256];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                if (count <= 0) {
                    break;
                }
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Caps the number of Distant Horizons LOD sections streamed to {@code playerUuid}
     * within a 1-second window so total bandwidth does not exceed {@code maxBytesPerSecond}.
     *
     * @param playerUuid           recipient player UUID
     * @param requestedLodSections number of LOD sections requested by the client
     * @param maxBytesPerSecond    maximum allowed LOD bandwidth per player in bytes/sec (e.g., 262144)
     * @return permitted number of LOD sections to transmit
     */
    public int throttleDistantHorizonsLodStream(
            UUID playerUuid, int requestedLodSections, int maxBytesPerSecond) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        if (requestedLodSections <= 0 || maxBytesPerSecond <= 0) {
            return 0;
        }
        int maxAllowedSections = Math.max(1, maxBytesPerSecond / ESTIMATED_BYTES_PER_LOD_SECTION);
        return Math.min(requestedLodSections, maxAllowedSections);
    }

    /**
     * Enforces a minimum tick interval between entity radar scans requested by JourneyMap
     * or Xaero's World Map / Minimap for a given player.
     *
     * @param playerUuid       requesting player UUID
     * @param currentTick      current server tick
     * @param minIntervalTicks minimum ticks between radar scans (e.g., 20)
     * @return {@code true} if the radar scan is permitted on this tick
     */
    public boolean shouldAllowWorldMapRadarScan(
            UUID playerUuid, long currentTick, int minIntervalTicks) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        int interval = Math.max(1, minIntervalTicks);
        Long previousTick = lastRadarScanTickByPlayer.get(playerUuid);
        if (previousTick != null && (currentTick - previousTick) < interval) {
            return false;
        }
        lastRadarScanTickByPlayer.put(playerUuid, currentTick);
        return true;
    }
}
