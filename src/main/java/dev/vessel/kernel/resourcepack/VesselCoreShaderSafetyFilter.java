package dev.vessel.kernel.resourcepack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resource pack safety sanitizer filtering out vanilla core shaders.
 * <p>
 * Strips shaders from {@code assets/minecraft/shaders/core/**} when generating universal client packs.
 * Vanilla core shaders override the OpenGL pipeline and cause immediate client crashes or severe rendering
 * artifacts on popular client optimization mods including Iris Shaders, Sodium, Oculus, Canvas, and OptiFine.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 * <!-- Changelog:
 *   1.0.0: Initial implementation of vanilla core shader stripping.
 *   1.0.1: Hardened path normalization against evasion (double-slashes, relative dots) and directory-level matches.
 * -->
 */
public final class VesselCoreShaderSafetyFilter {

    /** Standard relative path prefix for vanilla core shaders. */
    public static final String CORE_SHADERS_PREFIX = "assets/minecraft/shaders/core/";

    /** Directory path for vanilla core shaders folder without trailing slash. */
    public static final String CORE_SHADERS_DIR = "assets/minecraft/shaders/core";

    /**
     * Checks whether a resource path targets an unsafe vanilla core shader.
     *
     * @param resourcePath path to inspect
     * @return true if the path is inside {@code assets/minecraft/shaders/core/}
     */
    public static boolean isUnsafeCoreShader(String resourcePath) {
        if (resourcePath == null) {
            return false;
        }
        String normalized = normalizePath(resourcePath).toLowerCase(Locale.ROOT);
        return normalized.startsWith(CORE_SHADERS_PREFIX) || normalized.equals(CORE_SHADERS_DIR);
    }

    /**
     * Checks whether a resource path is safe from core shader conflicts.
     *
     * @param resourcePath path to inspect
     * @return true if safe to distribute
     */
    public static boolean isSafe(String resourcePath) {
        return !isUnsafeCoreShader(resourcePath);
    }

    /**
     * Filters entries and returns a detailed {@link VesselCoreShaderFilterResult}.
     *
     * @param entries map of resource path to file bytes
     * @return filter result containing safe entries and list of stripped shaders
     */
    public VesselCoreShaderFilterResult filter(Map<String, byte[]> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        Map<String, byte[]> safeEntries = new LinkedHashMap<>();
        List<String> removedShaders = new ArrayList<>();

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String path = entry.getKey();
            if (path == null) {
                continue;
            }
            if (isUnsafeCoreShader(path)) {
                removedShaders.add(path);
            } else {
                safeEntries.put(path, entry.getValue() != null ? entry.getValue() : new byte[0]);
            }
        }

        return new VesselCoreShaderFilterResult(safeEntries, removedShaders);
    }

    /**
     * Filters entry collection and returns only safe {@link VesselDeterministicZipEntry} instances.
     *
     * @param entries collection of zip entries
     * @return list containing only safe entries
     */
    public List<VesselDeterministicZipEntry> filterEntries(Collection<VesselDeterministicZipEntry> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        List<VesselDeterministicZipEntry> safeList = new ArrayList<>();
        for (VesselDeterministicZipEntry entry : entries) {
            if (isSafe(entry.path())) {
                safeList.add(entry);
            }
        }
        return safeList;
    }

    /**
     * Normalizes path separators, removes consecutive slashes, and strips leading slashes.
     *
     * @param rawPath input path
     * @return normalized forward-slash path
     */
    public static String normalizePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }
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
        while (normalized.contains("/./")) {
            normalized = normalized.replace("/./", "/");
        }
        return normalized;
    }
}
