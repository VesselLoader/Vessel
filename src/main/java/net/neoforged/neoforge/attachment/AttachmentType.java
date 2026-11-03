package net.neoforged.neoforge.attachment;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * NeoForge data attachment type descriptor ({@code net.neoforged.neoforge.attachment.AttachmentType})
 * that defines the canonical key and default value factory for attached state on entities, chunks,
 * block entities, and item stacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> attached data value type
 */
public final class AttachmentType<T> {

    private final String key;
    private final Supplier<T> defaultValueSupplier;

    /**
     * Constructs a new {@code AttachmentType} with an explicit key and default value supplier.
     *
     * @param key                  canonical attachment identifier
     * @param defaultValueSupplier factory supplying initial attachment instances
     */
    public AttachmentType(String key, Supplier<T> defaultValueSupplier) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.defaultValueSupplier = Objects.requireNonNull(
                defaultValueSupplier, "defaultValueSupplier must not be null");
    }

    /**
     * Creates an {@code AttachmentType} with a synthetic key derived from the supplier identity.
     *
     * @param defaultValueSupplier factory supplying initial attachment instances
     * @param <T>                  attached data value type
     * @return new {@code AttachmentType} instance
     */
    public static <T> AttachmentType<T> of(Supplier<T> defaultValueSupplier) {
        Objects.requireNonNull(defaultValueSupplier, "defaultValueSupplier must not be null");
        return new AttachmentType<>(
                "neoforge:attachment_" + System.identityHashCode(defaultValueSupplier),
                defaultValueSupplier);
    }

    /**
     * Creates an {@code AttachmentType} with the specified key and default value supplier.
     *
     * @param key                  canonical attachment identifier
     * @param defaultValueSupplier factory supplying initial attachment instances
     * @param <T>                  attached data value type
     * @return new {@code AttachmentType} instance
     */
    public static <T> AttachmentType<T> of(String key, Supplier<T> defaultValueSupplier) {
        return new AttachmentType<>(key, defaultValueSupplier);
    }

    /**
     * Returns the canonical key identifying this attachment type.
     *
     * @return attachment key
     */
    public String getKey() {
        return key;
    }

    /**
     * Creates a new default attachment value using the configured supplier.
     *
     * @return newly created default attachment value
     */
    public T createDefaultValue() {
        return defaultValueSupplier.get();
    }
}
