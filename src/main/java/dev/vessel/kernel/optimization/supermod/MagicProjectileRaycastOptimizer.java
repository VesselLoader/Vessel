package dev.vessel.kernel.optimization.supermod;

import java.util.Objects;

/**
 * Vectorized SIMD-friendly batch trajectory integrator for Botania, Iron's Spells &amp; Spellbooks,
 * and Psi magic projectile bursts.
 *
 * @version 1.0.0
 */
public final class MagicProjectileRaycastOptimizer {

    /**
     * Batch-computes updated 3D projectile coordinates into a flat interleaved {@code [x, y, z, ...]}
     * array for {@code n} magic projectiles.
     *
     * @param originsX   the starting X coordinates
     * @param originsY   the starting Y coordinates
     * @param originsZ   the starting Z coordinates
     * @param velX       the X velocity components
     * @param velY       the Y velocity components
     * @param velZ       the Z velocity components
     * @param deltaTicks the elapsed tick delta multiplier
     * @return a flat {@code float[n * 3]} array of interleaved {@code (x, y, z)} positions
     */
    public float[] batchComputeProjectilePositions(
            float[] originsX,
            float[] originsY,
            float[] originsZ,
            float[] velX,
            float[] velY,
            float[] velZ,
            float deltaTicks) {
        Objects.requireNonNull(originsX, "originsX must not be null");
        Objects.requireNonNull(originsY, "originsY must not be null");
        Objects.requireNonNull(originsZ, "originsZ must not be null");
        Objects.requireNonNull(velX, "velX must not be null");
        Objects.requireNonNull(velY, "velY must not be null");
        Objects.requireNonNull(velZ, "velZ must not be null");
        int count = Math.min(
                Math.min(originsX.length, originsY.length),
                Math.min(originsZ.length, Math.min(velX.length, Math.min(velY.length, velZ.length))));
        float[] result = new float[count * 3];
        for (int i = 0; i < count; i++) {
            int base = i * 3;
            result[base] = Math.fma(velX[i], deltaTicks, originsX[i]);
            result[base + 1] = Math.fma(velY[i], deltaTicks, originsY[i]);
            result[base + 2] = Math.fma(velZ[i], deltaTicks, originsZ[i]);
        }
        return result;
    }
}
