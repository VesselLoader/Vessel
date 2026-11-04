package dev.vessel.kernel.resourcepack;

import java.util.Locale;
import java.util.Objects;

/**
 * Normalizer enforcing lowercase canonical paths and POSIX separators across all archive entries.
 * <p>
 * Operating systems such as Linux, Android, and iOS treat filesystem paths case-sensitively,
 * whereas Windows treats them case-insensitively. This normalizer forces all asset paths
 * to lowercase ({@code toLowerCase(Locale.ROOT)}) and replaces backslashes with forward slashes,
 * preventing duplicate entries and missing asset crashes on mobile Bedrock and Linux Java clients.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselAssetPathNormalizer {

    /**
     * Normalizes a resource path to lowercase POSIX format.
     *
     * @param rawPath raw relative path string
     * @return normalized lowercase relative path
     * @throws IllegalArgumentException if path is null, blank, or attempts directory traversal
     */
    public String normalize(String rawPath) {
        Objects.requireNonNull(rawPath, "rawPath cannot be null");
        String trimmed = rawPath.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Asset path cannot be empty");
        }

        String posix = trimmed.replace('\\', '/');
        while (posix.startsWith("/")) {
            posix = posix.substring(1);
        }

        if (posix.contains("/../") || posix.startsWith("../") || posix.endsWith("/..") || posix.equals("..")) {
            throw new IllegalArgumentException("Directory traversal path forbidden: " + rawPath);
        }

        return posix.toLowerCase(Locale.ROOT);
    }
}
