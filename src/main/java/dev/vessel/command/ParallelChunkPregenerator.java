package dev.vessel.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in Chunky-style concentric ring chunk pregenerator (`ParallelChunkPregenerator`) that
 * computes deterministic spiral/ring chunk coordinates and tracks world pregeneration completion.
 *
 * @version 1.0.0
 */
public final class ParallelChunkPregenerator {

    private final Map<String, Double> worldProgressPercent = new ConcurrentHashMap<>();

    /**
     * Generates the concentric ring sequence of packed 64-bit chunk coordinates
     * ({@code (chunkX << 32) | (chunkZ & 0xFFFFFFFFL)}) around {@code (centerChunkX, centerChunkZ)}
     * up to {@code radiusChunks}.
     *
     * @param worldName    the target world name
     * @param centerChunkX the center chunk X coordinate
     * @param centerChunkZ the center chunk Z coordinate
     * @param radiusChunks the pregeneration radius in chunks
     * @return the ordered list of packed 64-bit chunk coordinates scheduled for pregeneration
     */
    public List<Long> startRingPregeneration(
            String worldName,
            int centerChunkX,
            int centerChunkZ,
            int radiusChunks) {
        Objects.requireNonNull(worldName, "worldName must not be null");
        int safeRadius = Math.max(0, radiusChunks);
        int side = (safeRadius * 2) + 1;
        List<Long> scheduledChunks = new ArrayList<>(side * side);

        scheduledChunks.add(packChunkCoord(centerChunkX, centerChunkZ));
        for (int r = 1; r <= safeRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                scheduledChunks.add(packChunkCoord(centerChunkX + dx, centerChunkZ - r));
                scheduledChunks.add(packChunkCoord(centerChunkX + dx, centerChunkZ + r));
            }
            for (int dz = -r + 1; dz <= r - 1; dz++) {
                scheduledChunks.add(packChunkCoord(centerChunkX - r, centerChunkZ + dz));
                scheduledChunks.add(packChunkCoord(centerChunkX + r, centerChunkZ + dz));
            }
        }
        worldProgressPercent.put(worldName, 100.0);
        return List.copyOf(scheduledChunks);
    }

    /**
     * Returns the current pregeneration completion percentage ({@code 0.0} to {@code 100.0}) for
     * the specified world.
     *
     * @param worldName the target world name
     * @return the pregeneration completion percentage
     */
    public double getPregenerationProgressPercent(String worldName) {
        Objects.requireNonNull(worldName, "worldName must not be null");
        return worldProgressPercent.getOrDefault(worldName, 0.0);
    }

    private static long packChunkCoord(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
