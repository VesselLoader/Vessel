package dev.vessel.kernel.vfs;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Layered Virtual File System mount combining multiple memory-mapped and archive layers.
 * Resolves resources according to descending layer priority, enabling seamless mod and plugin
 * asset virtualization without disk thrashing or excessive heap allocations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LayeredMemoryMappedVfsMount implements VirtualFileSystemMount {

    private final String mountPoint;
    private final List<VfsMountLayer> layers = new CopyOnWriteArrayList<>();
    private volatile boolean closed = false;

    /**
     * Constructs a LayeredMemoryMappedVfsMount with the specified mount point.
     *
     * @param mountPoint virtual mount point prefix (e.g., "/mods" or "/")
     */
    public LayeredMemoryMappedVfsMount(String mountPoint) {
        this.mountPoint = Objects.requireNonNull(mountPoint, "mountPoint cannot be null");
    }

    /**
     * Adds a mount layer and maintains priority ordering (highest priority first).
     *
     * @param layer the layer to add
     */
    public void addLayer(VfsMountLayer layer) {
        Objects.requireNonNull(layer, "layer cannot be null");
        layers.add(layer);
        layers.sort(null); // Uses Comparable natural ordering (descending priority)
    }

    /**
     * Removes a layer by its identifier.
     *
     * @param layerId unique layer identifier
     * @return true if removed
     */
    public boolean removeLayer(String layerId) {
        if (layerId == null) {
            return false;
        }
        return layers.removeIf(l -> l.layerId().equals(layerId));
    }

    /**
     * Returns the count of currently registered mount layers.
     *
     * @return layer count
     */
    public int layerCount() {
        return layers.size();
    }

    /**
     * Finds a resource across all registered layers in priority order.
     *
     * @param resourcePath relative resource path
     * @return resource descriptor if found
     * @throws IOException if I/O error occurs
     */
    public Optional<VfsFileResource> findResource(String resourcePath) throws IOException {
        checkNotClosed();
        for (VfsMountLayer layer : layers) {
            Optional<VfsFileResource> found = layer.find(resourcePath);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    @Override
    public String mountPoint() {
        return mountPoint;
    }

    @Override
    public Optional<byte[]> readResource(String resourcePath) throws IOException {
        checkNotClosed();
        Optional<VfsFileResource> res = findResource(resourcePath);
        return res.map(VfsFileResource::toByteArray);
    }

    @Override
    public boolean hasResource(String resourcePath) {
        if (closed || resourcePath == null) {
            return false;
        }
        for (VfsMountLayer layer : layers) {
            if (layer.contains(resourcePath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> listResources(String directoryPath) {
        if (closed) {
            return Collections.emptySet();
        }
        Set<String> allChildren = new HashSet<>();
        for (VfsMountLayer layer : layers) {
            allChildren.addAll(layer.listChildren(directoryPath));
        }
        return Collections.unmodifiableSet(allChildren);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        for (VfsMountLayer layer : layers) {
            layer.close();
        }
        layers.clear();
    }

    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("LayeredMemoryMappedVfsMount is already closed");
        }
    }
}
