package dev.vessel.kernel.optimization.crosschunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Provides high-speed spatial POI (Point of Interest) lookup for villagers using a chunk-partitioned
 * spatial hash grid, eliminating full-world linear scans from O(N) to O(1) local chunk bucket queries,
 * and lobotomizes 1x1 trading-hall villagers between scheduled restock intervals.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class PoiSpatialIndexAndVillagerLobotomizer {

    private final Map<Long, Integer> poiTypeByPos = new ConcurrentHashMap<>();
    private final Map<Long, Integer> poiFreeTicketsByPos = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> poiPositionsByChunk = new ConcurrentHashMap<>();

    /**
     * Creates a new POI spatial index and villager lobotomizer.
     */
    public PoiSpatialIndexAndVillagerLobotomizer() {
    }

    /**
     * Registers or updates a POI node at {@code packedBlockPos}.
     *
     * @param packedBlockPos 64-bit packed block position (standard Minecraft 26-bit X, 26-bit Z, 12-bit Y)
     * @param poiTypeId      numeric POI type identifier
     * @param freeTickets    number of available reservation tickets
     */
    public void registerPoiNode(long packedBlockPos, int poiTypeId, int freeTickets) {
        poiTypeByPos.put(packedBlockPos, poiTypeId);
        poiFreeTicketsByPos.put(packedBlockPos, Math.max(0, freeTickets));

        int chunkX = unpackX(packedBlockPos) >> 4;
        int chunkZ = unpackZ(packedBlockPos) >> 4;
        long chunkKey = packChunkKey(chunkX, chunkZ);

        List<Long> chunkPois = poiPositionsByChunk.computeIfAbsent(chunkKey, k -> new CopyOnWriteArrayList<>());
        if (!chunkPois.contains(packedBlockPos)) {
            chunkPois.add(packedBlockPos);
        }
    }

    /**
     * Unregisters a POI node from the spatial index.
     *
     * @param packedBlockPos 64-bit packed block position
     */
    public void unregisterPoiNode(long packedBlockPos) {
        poiTypeByPos.remove(packedBlockPos);
        poiFreeTicketsByPos.remove(packedBlockPos);

        int chunkX = unpackX(packedBlockPos) >> 4;
        int chunkZ = unpackZ(packedBlockPos) >> 4;
        long chunkKey = packChunkKey(chunkX, chunkZ);

        List<Long> chunkPois = poiPositionsByChunk.get(chunkKey);
        if (chunkPois != null) {
            chunkPois.remove(packedBlockPos);
        }
    }

    /**
     * Finds the nearest available POI node matching {@code poiTypeId} within {@code maxRadius}
     * using Chunk Spatial Hash Grid lookup, inspecting only adjacent chunk buckets rather than
     * scanning the entire world POI registry.
     *
     * @param originX   search origin X
     * @param originY   search origin Y
     * @param originZ   search origin Z
     * @param poiTypeId required POI type identifier
     * @param maxRadius maximum search radius in blocks
     * @return 64-bit packed block position of the nearest matching POI, or {@code Long.MIN_VALUE} if none
     */
    public long findNearestAvailablePoiO1(
            int originX, int originY, int originZ, int poiTypeId, int maxRadius) {
        long maxDistSq = (long) maxRadius * maxRadius;
        long bestPos = Long.MIN_VALUE;
        long bestDistSq = Long.MAX_VALUE;

        int minChunkX = (originX - maxRadius) >> 4;
        int maxChunkX = (originX + maxRadius) >> 4;
        int minChunkZ = (originZ - maxRadius) >> 4;
        int maxChunkZ = (originZ + maxRadius) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                long chunkKey = packChunkKey(cx, cz);
                List<Long> positions = poiPositionsByChunk.get(chunkKey);
                if (positions == null || positions.isEmpty()) {
                    continue;
                }

                for (int i = 0; i < positions.size(); i++) {
                    long pos = positions.get(i);
                    Integer type = poiTypeByPos.get(pos);
                    if (type == null || type != poiTypeId) {
                        continue;
                    }
                    Integer tickets = poiFreeTicketsByPos.get(pos);
                    if (tickets == null || tickets <= 0) {
                        continue;
                    }

                    int x = unpackX(pos);
                    int y = unpackY(pos);
                    int z = unpackZ(pos);
                    long dx = (long) x - originX;
                    long dy = (long) y - originY;
                    long dz = (long) z - originZ;
                    long distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq <= maxDistSq && distSq < bestDistSq) {
                        bestDistSq = distSq;
                        bestPos = pos;
                    }
                }
            }
        }
        return bestPos;
    }

    /**
     * Determines whether a villager enclosed on all four cardinal sides in a 1x1 trading
     * cell should have its pathfinding/brain tick suppressed on {@code currentTick},
     * waking only on exact {@code restockIntervalTicks} boundaries.
     *
     * @param northSolid           whether the north neighbor block is solid/impassable
     * @param southSolid           whether the south neighbor block is solid/impassable
     * @param eastSolid            whether the east neighbor block is solid/impassable
     * @param westSolid            whether the west neighbor block is solid/impassable
     * @param currentTick          current server tick
     * @param restockIntervalTicks interval between restock wake-up ticks (e.g., 1200)
     * @return {@code true} if the villager should be lobotomized on this tick
     */
    public boolean shouldLobotomizeTrappedVillager(
            boolean northSolid,
            boolean southSolid,
            boolean eastSolid,
            boolean westSolid,
            long currentTick,
            int restockIntervalTicks) {
        if (!northSolid || !southSolid || !eastSolid || !westSolid) {
            return false;
        }
        int interval = Math.max(1, restockIntervalTicks);
        return (currentTick % interval) != 0L;
    }

    private static long packChunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (((long) chunkZ) & 0xFFFFFFFFL);
    }

    private static int unpackX(long packedPos) {
        return (int) (packedPos >> 38);
    }

    private static int unpackY(long packedPos) {
        return (int) ((packedPos << 52) >> 52);
    }

    private static int unpackZ(long packedPos) {
        return (int) ((packedPos << 26) >> 38);
    }
}
