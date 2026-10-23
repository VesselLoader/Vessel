package dev.vessel.kernel.optimization.crosschunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advances unloaded Create trains, Steam 'n' Rails locomotives, and Little Logistics
 * barges along virtual RAM track splines with zero chunk loading when no player is nearby,
 * and applies hysteresis to forced chunk tickets to eliminate ticket thrashing.
 *
 * @version 1.0.0
 */
public final class TrainAndContraptionChunkTicketCoalescer {

    private final Map<Long, Double> virtualSplineProgressByTrain = new ConcurrentHashMap<>();
    private final Map<Long, Long> activeTicketTimestampByChunk = new ConcurrentHashMap<>();

    /**
     * Creates a new train and contraption chunk ticket coalescer.
     */
    public TrainAndContraptionChunkTicketCoalescer() {
    }

    /**
     * Advances an unloaded train along its virtual RAM track spline without loading chunks.
     *
     * @param trainId              unique train identifier
     * @param currentTrackProgress current spline progress in blocks
     * @param speedBlocksPerTick   train speed in blocks per tick
     * @param totalSplineLength    total length of the track spline loop or segment
     * @return updated spline progress in blocks
     */
    public double advanceUnloadedTrainOnVirtualSpline(
            long trainId,
            double currentTrackProgress,
            double speedBlocksPerTick,
            double totalSplineLength) {
        if (totalSplineLength <= 0.0) {
            return 0.0;
        }
        double updated = (currentTrackProgress + speedBlocksPerTick) % totalSplineLength;
        if (updated < 0.0) {
            updated += totalSplineLength;
        }
        virtualSplineProgressByTrain.put(trainId, updated);
        return updated;
    }

    /**
     * Determines whether a virtualized train should materialize its physical carriage
     * entities based on proximity to the nearest player.
     *
     * @param trainX             train world X coordinate
     * @param trainZ             train world Z coordinate
     * @param nearestPlayerX     nearest player world X coordinate
     * @param nearestPlayerZ     nearest player world Z coordinate
     * @param viewDistanceBlocks materialization radius in blocks
     * @return {@code true} if the train is within materialization range of a player
     */
    public boolean shouldMaterializeTrainEntity(
            double trainX,
            double trainZ,
            double nearestPlayerX,
            double nearestPlayerZ,
            double viewDistanceBlocks) {
        double dx = trainX - nearestPlayerX;
        double dz = trainZ - nearestPlayerZ;
        double distSq = dx * dx + dz * dz;
        return distSq <= (viewDistanceBlocks * viewDistanceBlocks);
    }

    /**
     * Records or refreshes a train chunk load ticket timestamp.
     *
     * @param packedChunkCoord 64-bit packed chunk coordinate
     * @param currentTick      current server tick
     */
    public void recordTrainChunkTicket(long packedChunkCoord, long currentTick) {
        activeTicketTimestampByChunk.put(packedChunkCoord, currentTick);
    }

    /**
     * Releases and returns all chunk tickets whose age exceeds {@code hysteresisTicks}
     * (e.g., 300 ticks = 15 seconds) to prevent rapid load/unload ticket thrashing.
     *
     * @param currentTick     current server tick
     * @param hysteresisTicks minimum ticket retention window in ticks
     * @return unmodifiable list of released packed chunk coordinates
     */
    public List<Long> releaseExpiredTicketsWithHysteresis(
            long currentTick, long hysteresisTicks) {
        List<Long> released = new ArrayList<>();
        Iterator<Map.Entry<Long, Long>> iterator =
                activeTicketTimestampByChunk.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            long recordedTick = entry.getValue();
            if (currentTick - recordedTick >= hysteresisTicks) {
                released.add(entry.getKey());
                iterator.remove();
            }
        }
        return Collections.unmodifiableList(released);
    }
}
