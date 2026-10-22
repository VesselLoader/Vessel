package dev.vessel.kernel.vfs;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * In-memory implementation of {@link VirtualFileSystemMount}.
 * <p>
 * Stores file byte arrays completely in memory using thread-safe data structures,
 * providing zero-disk-I/O resource lookups for runtime bytecode transformations and Jar-in-Jar modules.
 * </p>
 *
 * @version 1.0.1
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class InMemoryVirtualFileSystemMount implements VirtualFileSystemMount {

    private final String mountPoint;
    private final boolean readOnly;
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public InMemoryVirtualFileSystemMount(String mountPoint, boolean readOnly) {
        this.mountPoint = normalizeMountPoint(mountPoint);
        this.readOnly = readOnly;
    }

    public InMemoryVirtualFileSystemMount(String mountPoint) {
        this(mountPoint, false);
    }

    /**
     * Stores or updates a resource in the virtual mount.
     *
     * @param resourcePath relative path within the mount
     * @param content byte array content of the resource
     * @throws IllegalStateException if the mount is read-only or closed
     */
    public void putResource(String resourcePath, byte[] content) {
        ensureOpen();
        if (readOnly) {
            throw new IllegalStateException("Cannot write to read-only mount: " + mountPoint);
        }
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        Objects.requireNonNull(content, "content cannot be null");
        storage.put(normalizePath(resourcePath), content.clone());
    }

    @Override
    public String mountPoint() {
        return mountPoint;
    }

    @Override
    public Optional<byte[]> readResource(String resourcePath) throws IOException {
        ensureOpen();
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        byte[] bytes = storage.get(normalizePath(resourcePath));
        return bytes != null ? Optional.of(bytes.clone()) : Optional.empty();
    }

    /**
     * Reads resource wrapped into an immutable zero-copy buffer view, eliminating defensive array allocations.
     *
     * @param resourcePath relative path to resource
     * @return Optional containing ZeroCopyByteBuffer if found
     */
    public Optional<ZeroCopyByteBuffer> readResourceZeroCopy(String resourcePath) {
        ensureOpen();
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        byte[] bytes = storage.get(normalizePath(resourcePath));
        return bytes != null ? Optional.of(ZeroCopyByteBuffer.wrap(bytes)) : Optional.empty();
    }

    @Override
    public boolean hasResource(String resourcePath) {
        if (closed.get()) {
            return false;
        }
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        return storage.containsKey(normalizePath(resourcePath));
    }

    @Override
    public Set<String> listResources(String directoryPath) {
        ensureOpen();
        Objects.requireNonNull(directoryPath, "directoryPath cannot be null");
        String prefix = normalizePath(directoryPath);
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }

        Set<String> result = new HashSet<>();
        for (String path : storage.keySet()) {
            if (path.startsWith(prefix)) {
                String sub = path.substring(prefix.length());
                int slashIdx = sub.indexOf('/');
                if (slashIdx >= 0) {
                    result.add(sub.substring(0, slashIdx));
                } else if (!sub.isEmpty()) {
                    result.add(sub);
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            storage.clear();
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("VirtualFileSystemMount '" + mountPoint + "' is already closed");
        }
    }

    private static String normalizeMountPoint(String mp) {
        Objects.requireNonNull(mp, "mountPoint cannot be null");
        String s = mp.trim().replace('\\', '/');
        if (!s.startsWith("/")) {
            s = "/" + s;
        }
        if (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String normalizePath(String path) {
        String s = path.trim().replace('\\', '/');
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }
}
