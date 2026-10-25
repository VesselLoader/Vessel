package dev.vessel.kernel.display.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Presenter and simulation engine for the Blood Cauldron (Iron's Spells and Spellbooks mod).
 * Bridges modded blood rendering logic to Vanilla Java and Bedrock clients using
 * Virtual Fluid BlockDisplay entities and particle burst synchronization.
 * Extinguishes ERR-81 (blood cauldron desync) and AST-FLT (floating fluid artifacts).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class IronSpellsBloodCauldronPresenter {

    public static final String BLOOD_BLOCK_STATE = "minecraft:redstone_block";
    public static final String PARTICLE_CRIMSON_SPORE = "minecraft:crimson_spore";
    public static final String PARTICLE_BUBBLE_POP = "minecraft:bubble_pop";
    public static final String PARTICLE_SOUL = "minecraft:soul";

    public static final float HEAT_TRANSFER_RATE = 0.05f;
    public static final float COOLING_RATE = 0.02f;

    private final int cauldronId;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final VirtualFluidDisplaySurfaceManager surfaceManager;

    private float heatLevel; // 0.0 to 1.5
    private boolean heatSourceActive;
    private long totalTicksRun;

    /**
     * Constructs an IronSpellsBloodCauldronPresenter at a world block position.
     *
     * @param cauldronId unique cauldron ID
     * @param worldX world block coordinate X
     * @param worldY world block coordinate Y
     * @param worldZ world block coordinate Z
     */
    public IronSpellsBloodCauldronPresenter(int cauldronId, double worldX, double worldY, double worldZ) {
        this.cauldronId = cauldronId;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.surfaceManager = new VirtualFluidDisplaySurfaceManager(BLOOD_BLOCK_STATE, 0.1875f, 0.75f);
        this.surfaceManager.setFootprint(0.125f, 0.125f, 0.75f, 0.75f);
        this.heatLevel = 0.0f;
        this.heatSourceActive = false;
    }

    /**
     * Sets whether a heating block (e.g. fire, campfire, lava) is beneath the cauldron.
     */
    public synchronized void setHeatSourceActive(boolean active) {
        this.heatSourceActive = active;
    }

    /**
     * Fills or empties blood fluid fraction in the cauldron.
     *
     * @param fillFraction fluid level [0.0, 1.0]
     */
    public synchronized void setBloodLevel(float fillFraction) {
        surfaceManager.setTargetLevel(fillFraction);
    }

    /**
     * Advances cauldron thermodynamics and emits required particle packets for the tick.
     *
     * @param deltaSeconds delta frame time
     * @return list of particle descriptors to broadcast to nearby clients
     */
    public synchronized List<CauldronParticleDescriptor> tick(float deltaSeconds) {
        totalTicksRun++;

        // Heat accumulation or dissipation
        if (heatSourceActive) {
            heatLevel = Math.min(1.5f, heatLevel + HEAT_TRANSFER_RATE);
        } else {
            heatLevel = Math.max(0.0f, heatLevel - COOLING_RATE);
        }

        CauldronTemperatureState state = evaluateTemperatureState();

        // Adjust wave motion based on temperature
        if (state == CauldronTemperatureState.BOILING || state == CauldronTemperatureState.OVERHEATED) {
            surfaceManager.setWaveParameters(FluidSurfaceWaveParameters.BOILING_TURBULENCE);
        } else {
            surfaceManager.setWaveParameters(FluidSurfaceWaveParameters.GENTLE_RIPPLE);
        }

        surfaceManager.update(deltaSeconds);

        // Generate particle effects
        if (surfaceManager.getCurrentLevel() <= 0.05f) {
            return Collections.emptyList();
        }

        List<CauldronParticleDescriptor> particles = new ArrayList<>();
        double surfaceY = worldY + surfaceManager.getCurrentLevel() * 0.75;

        if (state == CauldronTemperatureState.SIMMERING) {
            if (totalTicksRun % 4 == 0) {
                particles.add(new CauldronParticleDescriptor(
                        PARTICLE_CRIMSON_SPORE,
                        worldX + 0.3 + (Math.random() * 0.4),
                        surfaceY + 0.05,
                        worldZ + 0.3 + (Math.random() * 0.4),
                        0.0, 0.04, 0.0, 2
                ));
            }
        } else if (state == CauldronTemperatureState.BOILING) {
            particles.add(new CauldronParticleDescriptor(
                    PARTICLE_CRIMSON_SPORE,
                    worldX + 0.2 + (Math.random() * 0.6),
                    surfaceY + 0.05,
                    worldZ + 0.2 + (Math.random() * 0.6),
                    (Math.random() - 0.5) * 0.05, 0.08, (Math.random() - 0.5) * 0.05, 4
            ));
            particles.add(new CauldronParticleDescriptor(
                    PARTICLE_BUBBLE_POP,
                    worldX + 0.25 + (Math.random() * 0.5),
                    surfaceY + 0.02,
                    worldZ + 0.25 + (Math.random() * 0.5),
                    0.0, 0.02, 0.0, 1
            ));
        } else if (state == CauldronTemperatureState.OVERHEATED) {
            particles.add(new CauldronParticleDescriptor(
                    PARTICLE_CRIMSON_SPORE,
                    worldX + 0.2 + (Math.random() * 0.6),
                    surfaceY + 0.1,
                    worldZ + 0.2 + (Math.random() * 0.6),
                    (Math.random() - 0.5) * 0.1, 0.15, (Math.random() - 0.5) * 0.1, 8
            ));
            particles.add(new CauldronParticleDescriptor(
                    PARTICLE_SOUL,
                    worldX + 0.5,
                    surfaceY + 0.2,
                    worldZ + 0.5,
                    0.0, 0.1, 0.0, 1
            ));
        }

        return particles;
    }

    /**
     * Determines the current thermal tier of the cauldron.
     */
    public synchronized CauldronTemperatureState evaluateTemperatureState() {
        if (heatLevel >= 1.2f) {
            return CauldronTemperatureState.OVERHEATED;
        } else if (heatLevel >= 0.85f) {
            return CauldronTemperatureState.BOILING;
        } else if (heatLevel >= 0.50f) {
            return CauldronTemperatureState.SIMMERING;
        } else if (heatLevel >= 0.20f) {
            return CauldronTemperatureState.WARM;
        }
        return CauldronTemperatureState.COLD;
    }

    /**
     * Captures an immutable snapshot of cauldron telemetry.
     */
    public synchronized BloodCauldronSnapshot createSnapshot() {
        CauldronTemperatureState state = evaluateTemperatureState();
        return new BloodCauldronSnapshot(
                cauldronId,
                surfaceManager.getCurrentLevel(),
                state,
                state.getFluidColorHex(),
                state == CauldronTemperatureState.BOILING || state == CauldronTemperatureState.OVERHEATED,
                heatSourceActive
        );
    }

    public int getCauldronId() {
        return cauldronId;
    }

    public VirtualFluidDisplaySurfaceManager getSurfaceManager() {
        return surfaceManager;
    }

    public synchronized float getHeatLevel() {
        return heatLevel;
    }

    public synchronized boolean isHeatSourceActive() {
        return heatSourceActive;
    }
}
