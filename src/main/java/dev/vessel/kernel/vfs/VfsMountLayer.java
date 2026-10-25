package dev.vessel.kernel.vfs;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Interface representing a distinct mount layer in a layered virtual file system.
 * Higher priority layers override conflicting paths in lower priority layers.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface VfsMountLayer extends Closeable, Comparable<VfsMountLayer> {

    /**
     * Unique identifier for this layer (e.g. "mod:create:0.5.1" or "filesystem:config").
     *
     * @return layer identifier string
     */
    String layerId();

    /**
     * Priority tier for resolution ordering. Higher values take precedence.
     *
     * @return priority integer
     */
    int priority();

    /**
     * Checks if a resource exists in this layer at the specified normalized path.
     *
     * @param normalizedPath normalized relative path
     * @return true if resource exists
     */
    boolean contains(String normalizedPath);

    /**
     * Retrieves a resource from this layer.
     *
     * @param normalizedPath normalized relative path
     * @return resource descriptor if found
     * @throws IOException if I/O read failure occurs
     */
    Optional<VfsFileResource> find(String normalizedPath) throws IOException;

    /**
     * Lists child paths under the given directory prefix.
     *
     * @param directoryPrefix relative directory path
     * @return set of child resource paths
     */
    Set<String> listChildren(String directoryPrefix);

    @Override
    default int compareTo(VfsMountLayer other) {
        // Sort descending by priority (higher priority first)
        return Integer.compare(other.priority(), this.priority());
    }
}
