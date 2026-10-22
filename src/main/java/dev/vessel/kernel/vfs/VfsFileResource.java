package dev.vessel.kernel.vfs;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Immutable descriptor and data container for a virtual file resource resolved from VFS layers.
 * Supports off-heap byte buffer slices and zero-copy access.
 *
 * @param path normalized virtual path
 * @param data direct or array-backed byte buffer
 * @param size size in bytes
 * @param lastModified timestamp of last modification
 * @param sourceLayerIdentifier identifier of the mount layer that supplied this resource
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VfsFileResource(
        String path,
        ByteBuffer data,
        long size,
        long lastModified,
        String sourceLayerIdentifier
) {
    public VfsFileResource {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(sourceLayerIdentifier, "sourceLayerIdentifier cannot be null");
    }

    /**
     * Extracts the raw bytes as a new byte array.
     *
     * @return copy of bytes
     */
    public byte[] toByteArray() {
        ByteBuffer slice = data.asReadOnlyBuffer();
        byte[] copy = new byte[slice.remaining()];
        slice.get(copy);
        return copy;
    }

    /**
     * Obtains a read-only view of the underlying ByteBuffer.
     *
     * @return read-only buffer slice
     */
    public ByteBuffer asReadOnlyBuffer() {
        return data.asReadOnlyBuffer();
    }
}
