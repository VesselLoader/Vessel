package dev.vessel.kernel.mixin.remap;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Disk-based cache coordinator for VSTM remapped Mixin refmaps.
 *
 * <p>Persists remapped JSON files under {@code cache/vstm-refmaps/<sha1>.json},
 * allowing zero-latency instant startup on subsequent boots.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RefmapDiskCacheCoordinator {

    private final Path cacheDirectory;

    /**
     * Constructs a disk cache coordinator with the specified cache directory.
     *
     * @param cacheDirectory target directory path
     */
    public RefmapDiskCacheCoordinator(Path cacheDirectory) {
        this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory cannot be null");
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(cacheDirectory)) {
                Files.createDirectories(cacheDirectory);
            }
        } catch (IOException ignored) {
            // Non-fatal, falls back to memory
        }
    }

    /**
     * Attempts to read a cached remapped refmap by mod SHA-1 hash.
     *
     * @param sha1Hash SHA-1 hash string of the mod jar
     * @return optional containing remapped JSON content
     */
    public Optional<String> readCachedRefmap(String sha1Hash) {
        Objects.requireNonNull(sha1Hash, "sha1Hash cannot be null");
        Path file = cacheDirectory.resolve(sha1Hash + ".refmap.json");
        if (Files.isRegularFile(file)) {
            try {
                return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // Fall back to recomputation
            }
        }
        return Optional.empty();
    }

    /**
     * Writes remapped refmap JSON to disk cache.
     *
     * @param sha1Hash mod jar SHA-1
     * @param json     remapped JSON content
     */
    public void writeCachedRefmap(String sha1Hash, String json) {
        Objects.requireNonNull(sha1Hash, "sha1Hash cannot be null");
        Objects.requireNonNull(json, "json cannot be null");
        Path file = cacheDirectory.resolve(sha1Hash + ".refmap.json");
        try {
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Non-fatal
        }
    }
}
