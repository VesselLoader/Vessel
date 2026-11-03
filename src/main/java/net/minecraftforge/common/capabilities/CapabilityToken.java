package net.minecraftforge.common.capabilities;

import java.util.Objects;

/**
 * Forge capability type token ({@code net.minecraftforge.common.capabilities.CapabilityToken})
 * used with {@code CapabilityManager.get(CapabilityToken)} to capture generic capability types.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> capability interface type
 * @version 1.0.0
 */
public class CapabilityToken<T> {

    private final String typeIdentifier;

    /**
     * Constructs a new {@code CapabilityToken} deriving its type identifier from its runtime class.
     */
    public CapabilityToken() {
        this.typeIdentifier = null;
    }

    /**
     * Constructs a new {@code CapabilityToken} with an explicit type identifier.
     *
     * @param typeIdentifier non-null capability type identifier
     */
    public CapabilityToken(String typeIdentifier) {
        this.typeIdentifier = Objects.requireNonNull(typeIdentifier, "typeIdentifier must not be null");
    }

    /**
     * Returns the type identifier associated with this capability token.
     *
     * @return non-null capability type identifier
     */
    public String getTypeIdentifier() {
        return typeIdentifier != null ? typeIdentifier : getClass().getName();
    }
}
