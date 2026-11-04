package dev.vessel.kernel.resourcepack;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable in-memory archive entry representation for deterministic packaging.
 *
 * @param path normalized relative path inside the zip archive (forward slashes)
 * @param data raw byte payload of the entry
 * @version 1.0.1
 * @author VESSEL Engine Team
 * <!-- Changelog:
 *   1.0.0: Initial implementation of immutable zip entry representation.
 *   1.0.1: Added directory traversal prevention (Zip Slip) and normalized consecutive/relative slashes.
 * -->
 */
public record VesselDeterministicZipEntry(String path, byte[] data) {

    public VesselDeterministicZipEntry {
        Objects.requireNonNull(path, "path cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        path = normalizePath(path);
        data = Arrays.copyOf(data, data.length);
    }

    /**
     * Normalizes archive path to use forward slashes, strips leading slashes, and prevents Zip Slip.
     *
     * @param rawPath input path
     * @return normalized path
     */
    public static String normalizePath(String rawPath) {
        Objects.requireNonNull(rawPath, "rawPath cannot be null");
        String normalized = rawPath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        if (normalized.contains("/../") || normalized.startsWith("../") || normalized.endsWith("/..") || normalized.equals("..")) {
            throw new IllegalArgumentException("Directory traversal path forbidden: " + rawPath);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Normalized path cannot be empty: " + rawPath);
        }
        return normalized;
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VesselDeterministicZipEntry other)) return false;
        return path.equals(other.path) && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
