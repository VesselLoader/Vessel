package net.kyori.adventure.key;

import java.util.Locale;
import java.util.Objects;

/**
 * Canonical Kyori Adventure namespaced identifier ({@code net.kyori.adventure.key.Key}).
 *
 * <p>Used across Paper, Velocity, Sponge, and modern Adventure-enabled plugins to address
 * sounds, fonts, channels, and resources.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Key {

    /**
     * Default Minecraft namespace ({@code "minecraft"}).
     */
    public static final String MINECRAFT_NAMESPACE = "minecraft";

    private final String namespace;
    private final String value;

    /**
     * Constructs a normalized Adventure {@code Key}.
     *
     * @param namespace non-null, non-blank namespace
     * @param value     non-null, non-blank key value
     */
    public Key(String namespace, String value) {
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(value, "value must not be null");
        String trimmedNamespace = namespace.trim().toLowerCase(Locale.ROOT);
        String trimmedValue = value.trim().toLowerCase(Locale.ROOT);
        if (trimmedNamespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (trimmedValue.isEmpty()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        this.namespace = trimmedNamespace;
        this.value = trimmedValue;
    }

    /**
     * Creates a {@code Key} from an explicit namespace and value.
     *
     * @param namespace key namespace
     * @param value     key value
     * @return constructed {@code Key}
     */
    public static Key key(String namespace, String value) {
        return new Key(namespace, value);
    }

    /**
     * Creates a {@code Key} by parsing a {@code "namespace:value"} string, defaulting to
     * {@link #MINECRAFT_NAMESPACE} if no colon separator is present.
     *
     * @param combined combined key string
     * @return constructed {@code Key}
     */
    public static Key key(String combined) {
        Objects.requireNonNull(combined, "combined must not be null");
        int colonIndex = combined.indexOf(':');
        if (colonIndex < 0) {
            return new Key(MINECRAFT_NAMESPACE, combined);
        }
        String ns = combined.substring(0, colonIndex);
        String val = combined.substring(colonIndex + 1);
        return new Key(ns.isEmpty() ? MINECRAFT_NAMESPACE : ns, val);
    }

    /**
     * Returns the namespace portion of this key.
     *
     * @return lowercase namespace string
     */
    public String namespace() {
        return namespace;
    }

    /**
     * Returns the value portion of this key.
     *
     * @return lowercase value string
     */
    public String value() {
        return value;
    }

    /**
     * Returns the canonical {@code "namespace:value"} string representation.
     *
     * @return formatted key string
     */
    public String asString() {
        return namespace + ":" + value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Key other)) {
            return false;
        }
        return namespace.equals(other.namespace) && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, value);
    }

    @Override
    public String toString() {
        return asString();
    }
}
