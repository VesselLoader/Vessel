package dev.vessel.adapter.bridge;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Governs modded chunk loaders (Mekanism Anchor Upgrade, FTB Chunks, ChickenChunks,
 * Railcraft Worldspike) by enforcing per-player forced-chunk quotas and suspending
 * forced tickets when the owning player has been offline beyond a configured threshold.
 *
 * @version 1.0.0
 */
public final class ChunkLoaderGovernor {

    private final Map<UUID, Set<Long>> playerOwnedChunks = new ConcurrentHashMap<>();
    private final Set<Long> activelyForcedChunks = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new chunk loader governor.
     */
    public ChunkLoaderGovernor() {
    }

    /**
     * Requests a forced chunk ticket for a player-owned machine or chunk claim.
     *
     * @param ownerPlayerUuid   UUID of the player owning the chunk loader
     * @param packedChunkCoord  64-bit packed chunk coordinate
     * @param maxQuotaPerPlayer maximum number of forced chunks allowed per player
     * @return {@code true} if the ticket was granted within quota, {@code false} if rejected
     */
    public boolean requestForcedChunkTicket(
            UUID ownerPlayerUuid, long packedChunkCoord, int maxQuotaPerPlayer) {
        Objects.requireNonNull(ownerPlayerUuid, "ownerPlayerUuid must not be null");
        if (maxQuotaPerPlayer <= 0) {
            return false;
        }
        Set<Long> owned =
                playerOwnedChunks.computeIfAbsent(
                        ownerPlayerUuid, k -> ConcurrentHashMap.newKeySet());
        synchronized (owned) {
            if (!owned.contains(packedChunkCoord) && owned.size() >= maxQuotaPerPlayer) {
                return false;
            }
            owned.add(packedChunkCoord);
        }
        activelyForcedChunks.add(packedChunkCoord);
        return true;
    }

    /**
     * Suspends all active forced chunk tickets owned by a player if their offline duration
     * meets or exceeds {@code sleepThresholdMinutes}.
     *
     * @param ownerPlayerUuid        UUID of the offline player
     * @param offlineDurationMinutes minutes elapsed since the player disconnected
     * @param sleepThresholdMinutes  configured offline sleep threshold in minutes
     * @return number of forced chunks transitioned into sleep mode
     */
    public int suspendChunksForOfflinePlayer(
            UUID ownerPlayerUuid, long offlineDurationMinutes, long sleepThresholdMinutes) {
        Objects.requireNonNull(ownerPlayerUuid, "ownerPlayerUuid must not be null");
        if (offlineDurationMinutes < sleepThresholdMinutes) {
            return 0;
        }
        Set<Long> owned = playerOwnedChunks.get(ownerPlayerUuid);
        if (owned == null || owned.isEmpty()) {
            return 0;
        }
        int suspended = 0;
        for (Long chunkCoord : owned) {
            if (activelyForcedChunks.remove(chunkCoord)) {
                suspended++;
            }
        }
        return suspended;
    }

    /**
     * Wakes and restores all forced chunk tickets belonging to a player upon login.
     *
     * @param ownerPlayerUuid UUID of the logging-in player
     * @return number of forced chunks woken up
     */
    public int wakeChunksOnPlayerLogin(UUID ownerPlayerUuid) {
        Objects.requireNonNull(ownerPlayerUuid, "ownerPlayerUuid must not be null");
        Set<Long> owned = playerOwnedChunks.get(ownerPlayerUuid);
        if (owned == null || owned.isEmpty()) {
            return 0;
        }
        int woken = 0;
        for (Long chunkCoord : owned) {
            if (activelyForcedChunks.add(chunkCoord)) {
                woken++;
            }
        }
        return woken;
    }

    /**
     * Checks whether the specified packed chunk coordinate is currently actively force-loaded.
     *
     * @param packedChunkCoord 64-bit packed chunk coordinate
     * @return {@code true} if actively forced
     */
    public boolean isChunkActivelyForced(long packedChunkCoord) {
        return activelyForcedChunks.contains(packedChunkCoord);
    }
}
