package org.bukkit.persistence;

import java.util.Objects;

/**
 * Defines a typed data descriptor for reading and writing values inside a
 * {@link PersistentDataContainer} ({@code org.bukkit.persistence.PersistentDataType}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param <P> primitive storage representation type
 * @param <C> complex runtime object type
 * @version 1.0.0
 */
public final class PersistentDataType<P, C> {

    public static final PersistentDataType<Byte, Byte> BYTE =
            new PersistentDataType<>(Byte.class, Byte.class);
    public static final PersistentDataType<Short, Short> SHORT =
            new PersistentDataType<>(Short.class, Short.class);
    public static final PersistentDataType<Integer, Integer> INTEGER =
            new PersistentDataType<>(Integer.class, Integer.class);
    public static final PersistentDataType<Long, Long> LONG =
            new PersistentDataType<>(Long.class, Long.class);
    public static final PersistentDataType<Float, Float> FLOAT =
            new PersistentDataType<>(Float.class, Float.class);
    public static final PersistentDataType<Double, Double> DOUBLE =
            new PersistentDataType<>(Double.class, Double.class);
    public static final PersistentDataType<String, String> STRING =
            new PersistentDataType<>(String.class, String.class);
    public static final PersistentDataType<byte[], byte[]> BYTE_ARRAY =
            new PersistentDataType<>(byte[].class, byte[].class);

    private final Class<P> primitiveType;
    private final Class<C> complexType;

    /**
     * Constructs a new {@code PersistentDataType} descriptor.
     *
     * @param primitiveType primitive backing class token
     * @param complexType   complex object class token
     */
    public PersistentDataType(Class<P> primitiveType, Class<C> complexType) {
        this.primitiveType = Objects.requireNonNull(primitiveType, "primitiveType must not be null");
        this.complexType = Objects.requireNonNull(complexType, "complexType must not be null");
    }

    public Class<P> getPrimitiveType() {
        return primitiveType;
    }

    public Class<C> getComplexType() {
        return complexType;
    }
}
