package dev.vessel.kernel.display.model;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for virtual BlockDisplay entities rendering dynamic 60 FPS fluid surfaces.
 * Implements smooth height lerping, multi-frequency wave oscillations, and block state adaptations.
 * Extinguishes ERR-80 (virtual fluid clipping) and AST-GEO (geometry deformation).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VirtualFluidDisplaySurfaceManager {

    private static final AtomicInteger FLUID_ENTITY_ID_GEN = new AtomicInteger(5_000_000);

    public static final String DEFAULT_WATER_BLOCK_STATE = "minecraft:water[level=0]";
    public static final float DEFAULT_CONTAINER_BASE_Y = 0.0625f; // 1/16 block bottom
    public static final float DEFAULT_CONTAINER_MAX_HEIGHT = 0.875f; // 14/16 block top
    public static final float LERP_INTERPOLATION_SPEED = 0.08f;

    private final int virtualEntityId;
    private final float basePositionY;
    private final float maxHeight;
    private String currentBlockStateId;
    private FluidSurfaceWaveParameters waveParams;

    private float currentLevel; // 0.0 to 1.0
    private float targetLevel;  // 0.0 to 1.0
    private float wavePhase;
    private float containerInsetX = 0.0625f;
    private float containerInsetZ = 0.0625f;
    private float surfaceSizeX = 0.875f;
    private float surfaceSizeZ = 0.875f;

    /**
     * Constructs a VirtualFluidDisplaySurfaceManager with default water block state.
     */
    public VirtualFluidDisplaySurfaceManager() {
        this(DEFAULT_WATER_BLOCK_STATE, DEFAULT_CONTAINER_BASE_Y, DEFAULT_CONTAINER_MAX_HEIGHT);
    }

    /**
     * Constructs a VirtualFluidDisplaySurfaceManager with custom container boundaries.
     *
     * @param blockStateId block state identifier for fluid visual
     * @param basePositionY lowest fluid elevation relative to block origin
     * @param maxHeight maximum fill height span
     */
    public VirtualFluidDisplaySurfaceManager(String blockStateId, float basePositionY, float maxHeight) {
        this.virtualEntityId = FLUID_ENTITY_ID_GEN.incrementAndGet();
        this.currentBlockStateId = Objects.requireNonNull(blockStateId, "blockStateId cannot be null");
        this.basePositionY = basePositionY;
        this.maxHeight = maxHeight;
        this.waveParams = FluidSurfaceWaveParameters.GENTLE_RIPPLE;
        this.currentLevel = 0.0f;
        this.targetLevel = 0.0f;
    }

    /**
     * Sets target fluid fill fraction.
     *
     * @param targetFraction value between 0.0 (empty) and 1.0 (full)
     */
    public synchronized void setTargetLevel(float targetFraction) {
        this.targetLevel = Math.clamp(targetFraction, 0.0f, 1.0f);
    }

    /**
     * Instantly sets current and target fluid fill fraction without lerping.
     */
    public synchronized void snapLevel(float fraction) {
        float clamped = Math.clamp(fraction, 0.0f, 1.0f);
        this.currentLevel = clamped;
        this.targetLevel = clamped;
    }

    /**
     * Updates fluid wave oscillation parameters.
     */
    public synchronized void setWaveParameters(FluidSurfaceWaveParameters params) {
        this.waveParams = Objects.requireNonNull(params, "params cannot be null");
    }

    /**
     * Changes the fluid block state (e.g. from water to blood or lava).
     */
    public synchronized void setBlockStateId(String blockStateId) {
        this.currentBlockStateId = Objects.requireNonNull(blockStateId, "blockStateId cannot be null");
    }

    /**
     * Configures the horizontal footprint of the fluid within its container.
     */
    public synchronized void setFootprint(float insetX, float insetZ, float sizeX, float sizeZ) {
        this.containerInsetX = insetX;
        this.containerInsetZ = insetZ;
        this.surfaceSizeX = Math.max(0.01f, sizeX);
        this.surfaceSizeZ = Math.max(0.01f, sizeZ);
    }

    /**
     * Advances the fluid surface simulation by one delta tick/frame and computes
     * the updated display entity frame transform.
     *
     * @param deltaSeconds delta time in fractional seconds (e.g. 0.0166f for 60 FPS)
     * @return current display entity transformation frame
     */
    public synchronized VirtualFluidDisplayFrame update(float deltaSeconds) {
        // Smoothly interpolate current level towards target level
        float deltaLevel = targetLevel - currentLevel;
        if (Math.abs(deltaLevel) > 0.0001f) {
            currentLevel += deltaLevel * LERP_INTERPOLATION_SPEED;
        } else {
            currentLevel = targetLevel;
        }

        // Advance wave phase
        wavePhase += waveParams.frequency() * (deltaSeconds * 60.0f);
        if (wavePhase > Math.PI * 2.0) {
            wavePhase -= (float) (Math.PI * 2.0);
        }

        // Active fluid surface calculations
        float waveOffset = (float) (Math.sin(wavePhase) * waveParams.amplitude());
        float pitchAngle = (float) (Math.sin(wavePhase * 0.7f) * waveParams.pitchRollWobble());
        float rollAngle = (float) (Math.cos(wavePhase * 0.8f) * waveParams.pitchRollWobble());

        // Calculate absolute surface height
        float fluidThickness = Math.max(0.01f, currentLevel * maxHeight);
        float surfaceY = basePositionY + fluidThickness + (currentLevel > 0.05f ? waveOffset : 0.0f);

        return new VirtualFluidDisplayFrame(
                virtualEntityId,
                containerInsetX,
                surfaceY,
                containerInsetZ,
                rollAngle,
                pitchAngle,
                surfaceSizeX,
                fluidThickness,
                surfaceSizeZ,
                currentBlockStateId
        );
    }

    public int getVirtualEntityId() {
        return virtualEntityId;
    }

    public synchronized float getCurrentLevel() {
        return currentLevel;
    }

    public synchronized float getTargetLevel() {
        return targetLevel;
    }

    public synchronized String getCurrentBlockStateId() {
        return currentBlockStateId;
    }

    public synchronized FluidSurfaceWaveParameters getWaveParams() {
        return waveParams;
    }
}
