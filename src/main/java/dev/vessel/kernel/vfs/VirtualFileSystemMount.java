package dev.vessel.kernel.vfs;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Interface representing a mounted virtual file system container in the VESSEL microkernel.
 *
 * <p>Enables transparent reading of Jar-in-Jar archives, nested resources, and dynamic assets
 * directly from memory without extracting unneeded files to disk, eliminating fragile host-specific
 * locator hacks.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-3)
 */
public interface VirtualFileSystemMount extends AutoCloseable {

    /**
     * Returns the virtual mount point prefix (e.g., "/mods/sodium.jar").
     *
     * @return the virtual mount point path
     */
    String mountPoint();

    /**
     * Reads the byte array content of a resource at the specified relative path within this mount.
     *
     * @param resourcePath relative path to the resource inside the mount
     * @return an Optional containing the byte array if found, or empty Optional if absent
     * @throws IOException if an I/O error occurs during retrieval
     */
    Optional<byte[]> readResource(String resourcePath) throws IOException;

    /**
     * Checks if a resource exists at the specified relative path.
     *
     * @param resourcePath relative path to check
     * @return true if the resource exists
     */
    boolean hasResource(String resourcePath);

    /**
     * Lists all resource paths directly contained under the specified relative directory path.
     *
     * @param directoryPath the directory path to list
     * @return unmodifiable set of relative child resource paths
     */
    Set<String> listResources(String directoryPath);

    /**
     * Returns whether this mount point is strictly read-only.
     *
     * @return true if read-only
     */
    boolean isReadOnly();

    @Override
    void close() throws IOException;
}
