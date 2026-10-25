package dev.vessel.kernel.asset.stream;

import java.util.Optional;

/**
 * Repository interface for managing streamable server assets.
 * Allows atomic registration, lookup, and removal of assets.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface StreamableAssetRepository {

    /**
     * Registers an asset resource into the repository.
     *
     * @param resource streamable asset
     */
    void registerAsset(StreamableAssetResource resource);

    /**
     * Finds an asset resource by its virtual URI path.
     *
     * @param path request URI path
     * @return Optional containing resource if found, empty otherwise
     */
    Optional<StreamableAssetResource> findAsset(String path);

    /**
     * Removes an asset by its virtual URI path.
     *
     * @param path request URI path
     * @return true if an asset was removed, false otherwise
     */
    boolean removeAsset(String path);

    /**
     * Returns total number of registered assets.
     *
     * @return asset count
     */
    int size();

    /**
     * Clears all registered assets from memory.
     */
    void clear();
}
