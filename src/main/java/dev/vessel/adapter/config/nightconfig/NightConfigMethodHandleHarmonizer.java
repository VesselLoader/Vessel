package dev.vessel.adapter.config.nightconfig;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;
import java.util.Optional;

/**
 * MethodHandle dispatcher harmonizing NightConfig API differences between version 3.6.x and 3.8.x.
 *
 * <p>Prevents {@link NoSuchMethodError} when Forge and NeoForge mods parse TOML configurations.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class NightConfigMethodHandleHarmonizer {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /**
     * Constructs a method handle harmonizer.
     */
    public NightConfigMethodHandleHarmonizer() {
    }

    /**
     * Safely finds a method handle across old or new signatures.
     *
     * @param targetClass target class
     * @param methodName  method name
     * @param returnType  return type
     * @param paramTypes  parameter types
     * @return optional method handle
     */
    public Optional<MethodHandle> findHandle(
            Class<?> targetClass,
            String methodName,
            Class<?> returnType,
            Class<?>... paramTypes
    ) {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(methodName, "methodName cannot be null");
        try {
            MethodType type = MethodType.methodType(returnType, paramTypes);
            MethodHandle handle = LOOKUP.findVirtual(targetClass, methodName, type);
            return Optional.of(handle);
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            // Signature mismatch, return empty
            return Optional.empty();
        }
    }
}
