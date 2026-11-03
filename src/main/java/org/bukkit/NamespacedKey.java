package org.bukkit;

import java.util.Locale;
import java.util.Objects;
import org.bukkit.plugin.Plugin;

/**
 * Represents a String-based key which consists of two components - a namespace and a key.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NamespacedKey {

    /**
     * The namespace representing all inbuilt Minecraft keys.
     */
    public static final String MINECRAFT = "minecraft";

    private final String namespace;
    private final String key;

    /**
     * Creates a key in a specific namespace.
     *
     * @param namespace the namespace (non-null, non-blank)
     * @param key       the key (non-null, non-blank)
     */
    public NamespacedKey(String namespace, String key) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(key, "key must not be null");
        String normalizedNamespace = namespace.trim().toLowerCase(Locale.ROOT);
        String normalizedKey = key.trim().toLowerCase(Locale.ROOT);
        if (normalizedNamespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.namespace = normalizedNamespace;
        this.key = normalizedKey;
    }

    /**
     * Creates a key in the plugin's namespace.
     *
     * @param plugin the plugin to use for the namespace
     * @param key    the key to create
     */
    public NamespacedKey(Plugin plugin, String key) {
        this(Objects.requireNonNull(plugin, "plugin must not be null").getName(), key);
    }

    /**
     * Gets a key in the {@link #MINECRAFT} namespace.
     *
     * @param key the key to use
     * @return new key in the Minecraft namespace
     */
    public static NamespacedKey minecraft(String key) {
        return new NamespacedKey(MINECRAFT, key);
    }

    /**
     * Parses a {@code NamespacedKey} from a string input ({@code "namespace:key"} or {@code "key"}).
     *
     * @param string the string to convert
     * @return the parsed key, or {@code null} if invalid or blank
     */
    public static NamespacedKey fromString(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        String trimmed = string.trim();
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx >= 0) {
            String ns = trimmed.substring(0, colonIdx).trim();
            String k = trimmed.substring(colonIdx + 1).trim();
            if (ns.isEmpty() || k.isEmpty()) {
                return null;
            }
            return new NamespacedKey(ns, k);
        }
        return new NamespacedKey(MINECRAFT, trimmed);
    }

    /**
     * Returns the namespace component of this key.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the key component of this key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return namespace + ":" + key;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NamespacedKey other)) {
            return false;
        }
        return namespace.equals(other.namespace) && key.equals(other.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, key);
    }
}
