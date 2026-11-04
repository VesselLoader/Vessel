package dev.vessel.kernel.optimization.supermod;

import dev.vessel.kernel.gpu.GpuComputeDispatcher;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance cached accessor extracting explosion epicenter coordinates and blast radius
 * from Vanilla {@code Explosion} or {@code ServerExplosion} instances with zero ongoing allocation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselExplosionFieldAccessor {

    private static final Map<Class<?>, MethodHandle> X_GETTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> Y_GETTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> Z_GETTERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MethodHandle> RADIUS_GETTERS = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private VesselExplosionFieldAccessor() {
    }

    /**
     * Extracts coordinates and power from the candidate explosion object and dispatches
     * parallel raycasting to the sovereign GPU/SIMD compute dispatcher.
     *
     * @param explosion Minecraft explosion object
     */
    public static void dispatchAcceleratedExplosion(Object explosion) {
        if (explosion == null) {
            return;
        }
        Class<?> clazz = explosion.getClass();
        try {
            double x = getDoubleCoordinate(clazz, explosion, X_GETTERS, "x");
            double y = getDoubleCoordinate(clazz, explosion, Y_GETTERS, "y");
            double z = getDoubleCoordinate(clazz, explosion, Z_GETTERS, "z");
            float radius = getFloatRadius(clazz, explosion, RADIUS_GETTERS, "radius");

            if (radius > 0.0f) {
                GpuComputeDispatcher.getActiveDispatcher().dispatchExplosionRaycasts(x, y, z, radius, 1357);
            }
        } catch (Throwable ignored) {
            // Gracefully ignore in case obfuscation mapping differs or access is restricted
        }
    }

    private static double getDoubleCoordinate(
            Class<?> clazz,
            Object instance,
            Map<Class<?>, MethodHandle> cache,
            String primaryFieldName) throws Throwable {
        MethodHandle handle = cache.get(clazz);
        if (handle == null) {
            handle = resolveGetter(clazz, double.class, primaryFieldName, "x", "f_46042_", "field_1394");
            if (handle != null) {
                cache.put(clazz, handle);
            }
        }
        if (handle != null) {
            return (double) handle.invoke(instance);
        }
        return 0.0;
    }

    private static float getFloatRadius(
            Class<?> clazz,
            Object instance,
            Map<Class<?>, MethodHandle> cache,
            String primaryFieldName) throws Throwable {
        MethodHandle handle = cache.get(clazz);
        if (handle == null) {
            handle = resolveGetter(clazz, float.class, primaryFieldName, "radius", "power", "f_46045_", "field_1397");
            if (handle != null) {
                cache.put(clazz, handle);
            }
        }
        if (handle != null) {
            return (float) handle.invoke(instance);
        }
        return 0.0f;
    }

    private static MethodHandle resolveGetter(
            Class<?> clazz,
            Class<?> returnType,
            String... candidateFieldNames) {
        for (String candidate : candidateFieldNames) {
            try {
                Field f = clazz.getDeclaredField(candidate);
                f.setAccessible(true);
                return LOOKUP.unreflectGetter(f);
            } catch (Throwable ignored) {
            }
        }
        // Also inspect parent superclasses if needed
        Class<?> current = clazz.getSuperclass();
        while (current != null && current != Object.class) {
            for (String candidate : candidateFieldNames) {
                try {
                    Field f = current.getDeclaredField(candidate);
                    f.setAccessible(true);
                    return LOOKUP.unreflectGetter(f);
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
