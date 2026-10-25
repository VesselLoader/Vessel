package dev.vessel.kernel.folia.blockentity;

/**
 * Invalidation and detachment callback for modded BlockEntities on chunk unload.
 * <p>
 * Directly resolves ERR-36 and FOL-061..080.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface BlockEntityLifecycleCallback {

    /**
     * Invoked when the BlockEntity is safely detached prior to chunk unloading.
     *
     * @param chunkKey packed 64-bit coordinate of the parent chunk
     * @param blockX world X coordinate
     * @param blockY world Y coordinate
     * @param blockZ world Z coordinate
     * @param blockEntityId namespaced identifier of the BlockEntity
     */
    void onBlockEntityDetached(long chunkKey, int blockX, int blockY, int blockZ, String blockEntityId);

    /**
     * Invoked when the parent chunk completes its memory purge.
     *
     * @param chunkKey packed 64-bit coordinate of the unloaded chunk
     */
    void onChunkUnloaded(long chunkKey);
}
