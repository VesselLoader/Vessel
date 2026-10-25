package dev.vessel.kernel.network.bridge;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Harmonizes legitimate rapid inventory sorting bursts from client mods (Mouse Tweaks, Inventory
 * Profiles Next, Inventory Tweaks Renewed) within a bounded 500 ms window so Paper/Spigot
 * packet-rate limiters do not falsely kick modded players, while blocking unbounded packet floods.
 *
 * @version 1.0.0
 */
public final class ContainerTransactionBatchAcceptor {

    private static final long SORT_WINDOW_MS = 500L;

    private final Map<UUID, Long> windowStartTimestampMs = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> windowClickCounts = new ConcurrentHashMap<>();

    /**
     * Evaluates whether a rapid container slot click packet from a player should be accepted
     * within the current 500 ms sorting window.
     *
     * @param playerUuid                the player's unique identifier
     * @param currentTimestampMs        the current monotonic timestamp in milliseconds
     * @param maxAllowedClicksPerWindow the maximum container clicks allowed per 500 ms window
     * @return {@code true} if the click is accepted as part of a valid inventory batch
     */
    public boolean acceptRapidSortClick(
            UUID playerUuid,
            long currentTimestampMs,
            int maxAllowedClicksPerWindow) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        if (maxAllowedClicksPerWindow <= 0) {
            return false;
        }
        Long windowStart = windowStartTimestampMs.get(playerUuid);
        if (windowStart == null || (currentTimestampMs - windowStart) >= SORT_WINDOW_MS) {
            windowStartTimestampMs.put(playerUuid, currentTimestampMs);
            windowClickCounts.put(playerUuid, 1);
            return true;
        }
        int updated = windowClickCounts.merge(playerUuid, 1, Integer::sum);
        return updated <= maxAllowedClicksPerWindow;
    }
}
