package dev.vessel.kernel.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * High-performance in-memory ZIP/JAR archive mount layer.
 * Indexes archive entries into memory-backed ByteBuffer slices for instantaneous
 * zero-copy resource retrieval.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ArchiveMountLayer implements VfsMountLayer {

    private final String layerId;
    private final int priority;
    private final Map<String, VfsFileResource> entries = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    /**
     * Constructs an ArchiveMountLayer by parsing a ZIP/JAR stream.
     *
     * @param layerId unique layer identifier
     * @param priority resolution priority
     * @param archiveStream input stream containing the archive bytes
     * @throws IOException if error reading archive
     */
    public ArchiveMountLayer(String layerId, int priority, InputStream archiveStream) throws IOException {
        this.layerId = Objects.requireNonNull(layerId, "layerId cannot be null");
        this.priority = priority;
        indexStream(archiveStream);
    }

    /**
     * Constructs an ArchiveMountLayer from a byte array.
     *
     * @param layerId unique layer identifier
     * @param priority resolution priority
     * @param archiveBytes raw archive bytes
     * @throws IOException if error reading archive
     */
    public ArchiveMountLayer(String layerId, int priority, byte[] archiveBytes) throws IOException {
        this(layerId, priority, new ByteArrayInputStream(Objects.requireNonNull(archiveBytes, "archiveBytes cannot be null")));
    }

    private void indexStream(InputStream stream) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String normPath = normalize(entry.getName());
                    byte[] data = zis.readAllBytes();
                    ByteBuffer buf = ByteBuffer.wrap(data).asReadOnlyBuffer();
                    VfsFileResource res = new VfsFileResource(normPath, buf, data.length, entry.getTime(), layerId);
                    entries.put(normPath, res);
                }
                zis.closeEntry();
            }
        }
    }

    private static String normalize(String path) {
        String p = path.replace('\\', '/').trim();
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        return p;
    }

    @Override
    public String layerId() {
        return layerId;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean contains(String normalizedPath) {
        if (closed || normalizedPath == null) {
            return false;
        }
        return entries.containsKey(normalize(normalizedPath));
    }

    @Override
    public Optional<VfsFileResource> find(String normalizedPath) {
        if (closed || normalizedPath == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(entries.get(normalize(normalizedPath)));
    }

    @Override
    public Set<String> listChildren(String directoryPrefix) {
        if (closed) {
            return Collections.emptySet();
        }
        String prefix = normalize(directoryPrefix);
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix += "/";
        }
        Set<String> children = new HashSet<>();
        for (String key : entries.keySet()) {
            if (prefix.isEmpty() || key.startsWith(prefix)) {
                children.add(key);
            }
        }
        return Collections.unmodifiableSet(children);
    }

    @Override
    public void close() {
        closed = true;
        entries.clear();
    }
}
