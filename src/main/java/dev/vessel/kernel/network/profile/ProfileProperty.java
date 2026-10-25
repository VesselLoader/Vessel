package dev.vessel.kernel.network.profile;

import java.util.Objects;

/**
 * Immutable property associated with a federated player GameProfile (e.g. skin textures).
 *
 * @param name      the property key name (e.g. "textures")
 * @param value     the property base64 or string value
 * @param signature cryptographic signature string (nullable)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ProfileProperty(
        String name,
        String value,
        String signature
) {

    /**
     * Compact constructor validating non-null requirements.
     */
    public ProfileProperty {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Convenience constructor for properties without signature.
     *
     * @param name  property name
     * @param value property value
     */
    public ProfileProperty(String name, String value) {
        this(name, value, null);
    }
}
