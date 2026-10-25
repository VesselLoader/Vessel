package dev.vessel.kernel.asset.stream;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of StreamableAssetRepository.
 * Uses ConcurrentHashMap for lock-free parallel lookups by virtual path.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class InMemoryStreamableAssetRepository implements StreamableAssetRepository {

    private final Map<String, StreamableAssetResource> assets = new ConcurrentHashMap<>();

    @Override
    public void registerAsset(StreamableAssetResource resource) {
        Objects.requireNonNull(resource, "resource cannot be null");
        assets.put(normalizePath(resource.resourcePath()), resource);
    }

    @Override
    public Optional<StreamableAssetResource> findAsset(String path) {
        if (path == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(assets.get(normalizePath(path)));
    }

    @Override
    public boolean removeAsset(String path) {
        if (path == null) {
            return false;
        }
        return assets.remove(normalizePath(path)) != null;
    }

    @Override
    public int size() {
        return assets.size();
    }

    @Override
    public void clear() {
        assets.clear();
    }

    private String normalizePath(String path) {
        String normalized = path.strip();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }
}
