package net.minecraft.resources;

import java.util.Locale;
import java.util.Objects;

/**
 * Canonical Minecraft resource identifier ({@code net.minecraft.resources.ResourceLocation})
 * representing a namespaced path ({@code namespace:path}).
 *
 * <p>Supports both legacy 1.20.x constructors ({@code new ResourceLocation(namespace, path)}) and
 * modern 1.21+ factory methods ({@link #fromNamespaceAndPath(String, String)},
 * {@link #withDefaultNamespace(String)}, {@link #parse(String)}, {@link #tryParse(String)}).</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ResourceLocation implements Comparable<ResourceLocation> {

    /**
     * Default namespace applied when no explicit namespace is specified.
     */
    public static final String DEFAULT_NAMESPACE = "minecraft";

    private final String namespace;
    private final String path;

    /**
     * Constructs a {@code ResourceLocation} from an explicit namespace and path.
     *
     * @param namespace non-null, non-blank namespace
     * @param path      non-null, non-blank path
     */
    public ResourceLocation(String namespace, String path) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(path, "path must not be null");
        String normNamespace = namespace.trim().toLowerCase(Locale.ROOT);
        String normPath = path.trim().toLowerCase(Locale.ROOT);
        if (normNamespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (normPath.isEmpty()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        this.namespace = normNamespace;
        this.path = normPath;
    }

    /**
     * Constructs a {@code ResourceLocation} by splitting {@code location} on {@code ':'},
     * defaulting to {@link #DEFAULT_NAMESPACE} if no colon is present.
     *
     * @param location resource location string
     */
    public ResourceLocation(String location) {
        this(extractNamespace(location, ':'), extractPath(location, ':'));
    }

    /**
     * Creates a {@code ResourceLocation} by splitting {@code location} on the given separator
     * character.
     *
     * @param location  resource location string
     * @param separator delimiter character between namespace and path
     * @return parsed {@code ResourceLocation}
     */
    public static ResourceLocation of(String location, char separator) {
        return new ResourceLocation(
                extractNamespace(location, separator),
                extractPath(location, separator));
    }

    /**
     * Parses a {@code ResourceLocation} from its string representation ({@code namespace:path} or
     * {@code path}).
     *
     * @param location resource location string
     * @return parsed {@code ResourceLocation}
     */
    public static ResourceLocation parse(String location) {
        return new ResourceLocation(location);
    }

    /**
     * Creates a {@code ResourceLocation} from an explicit namespace and path (Minecraft 1.21+
     * factory).
     *
     * @param namespace resource namespace
     * @param path      resource path
     * @return new {@code ResourceLocation}
     */
    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    /**
     * Creates a {@code ResourceLocation} in the {@link #DEFAULT_NAMESPACE} namespace.
     *
     * @param path resource path
     * @return new {@code ResourceLocation}
     */
    public static ResourceLocation withDefaultNamespace(String path) {
        return new ResourceLocation(DEFAULT_NAMESPACE, path);
    }

    /**
     * Attempts to parse {@code location} into a {@code ResourceLocation}, returning {@code null}
     * if the input is {@code null}, blank, or malformed.
     *
     * @param location resource location string
     * @return parsed {@code ResourceLocation}, or {@code null} if invalid
     */
    public static ResourceLocation tryParse(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        try {
            return new ResourceLocation(location);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Returns the namespace component of this resource location.
     *
     * @return namespace string
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the path component of this resource location.
     *
     * @return path string
     */
    public String getPath() {
        return path;
    }

    @Override
    public int compareTo(ResourceLocation other) {
        int pathComparison = this.path.compareTo(other.path);
        if (pathComparison != 0) {
            return pathComparison;
        }
        return this.namespace.compareTo(other.namespace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceLocation other)) {
            return false;
        }
        return namespace.equals(other.namespace) && path.equals(other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, path);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    private static String extractNamespace(String location, char separator) {
        Objects.requireNonNull(location, "location must not be null");
        String trimmed = location.trim();
        int idx = trimmed.indexOf(separator);
        if (idx >= 0) {
            return trimmed.substring(0, idx);
        }
        return DEFAULT_NAMESPACE;
    }

    private static String extractPath(String location, char separator) {
        Objects.requireNonNull(location, "location must not be null");
        String trimmed = location.trim();
        int idx = trimmed.indexOf(separator);
        if (idx >= 0) {
            return trimmed.substring(idx + 1);
        }
        return trimmed;
    }
}
