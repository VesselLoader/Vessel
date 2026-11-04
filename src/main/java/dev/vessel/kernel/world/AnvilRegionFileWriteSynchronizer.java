package dev.vessel.kernel.world;

import java.lang.reflect.Method;

/**
 * Synchronizes and flushes physical world chunk data down to Anvil {@code .mca} region files
 * by invoking the underlying Mojang {@code ServerChunkCache.save(boolean)} method.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AnvilRegionFileWriteSynchronizer {

    public AnvilRegionFileWriteSynchronizer() {
    }

    /**
     * Flushes and synchronizes all active chunks in the primary level down to disk.
     *
     * @param flushAll {@code true} to flush all dirty chunks immediately
     * @return {@code true} if flush operation succeeded
     */
    public boolean flushWorldChunksToDisk(boolean flushAll) {
        Object activeServer = dev.vessel.command.VesselServerBridgeRegistry.getActiveMinecraftServer();
        if (activeServer == null) {
            return false;
        }

        try {
            Method overworldMethod = activeServer.getClass().getMethod("overworld");
            Object serverLevel = overworldMethod.invoke(activeServer);
            if (serverLevel == null) {
                return false;
            }

            Method getChunkSourceMethod = serverLevel.getClass().getMethod("getChunkSource");
            Object chunkSource = getChunkSourceMethod.invoke(serverLevel);
            if (chunkSource == null) {
                return false;
            }

            for (Method m : chunkSource.getClass().getMethods()) {
                if ("save".equals(m.getName()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == boolean.class) {
                    m.invoke(chunkSource, flushAll);
                    return true;
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
