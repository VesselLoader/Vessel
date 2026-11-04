package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Accesses and configures Minecraft block state definitions ({@code StateDefinition}) and
 * retrieves default {@code BlockState} instances across all Minecraft eras.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBlockStateDefinitionSupport {

    public VesselBlockStateDefinitionSupport() {
    }

    /**
     * Resolves the default {@code BlockState} of the given block instance.
     *
     * @param blockInstance non-null block object
     * @return default block state object, or {@code null} if retrieval failed
     */
    public Object resolveDefaultBlockState(Object blockInstance) {
        Objects.requireNonNull(blockInstance, "blockInstance must not be null");

        // 1. Modern 1.16+ / 26.3: defaultBlockState()
        try {
            Method m = blockInstance.getClass().getMethod("defaultBlockState");
            return m.invoke(blockInstance);
        } catch (Exception ignored) {
        }

        // 2. Legacy 1.8 – 1.15: getDefaultState()
        try {
            Method m = blockInstance.getClass().getMethod("getDefaultState");
            return m.invoke(blockInstance);
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * Resolves the {@code StateDefinition} holding all valid properties for this block.
     *
     * @param blockInstance non-null block object
     * @return state definition object, or {@code null} if retrieval failed
     */
    public Object resolveStateDefinition(Object blockInstance) {
        Objects.requireNonNull(blockInstance, "blockInstance must not be null");

        // 1. Modern 1.16+ / 26.3: getStateDefinition()
        try {
            Method m = blockInstance.getClass().getMethod("getStateDefinition");
            return m.invoke(blockInstance);
        } catch (Exception ignored) {
        }

        // 2. Legacy 1.8 – 1.15: getBlockState()
        try {
            Method m = blockInstance.getClass().getMethod("getBlockState");
            return m.invoke(blockInstance);
        } catch (Exception ignored) {
        }

        return null;
    }
}
