package dev.vessel.kernel.ecosystem;

/**
 * Unified runtime coordinator uniting all cross-ecosystem event bridges into a single sovereign
 * execution plane.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class UnifiedEcosystemServerRuntimeCoordinator {

    private final CrossEcosystemBlockEventBridge blockBridge = new CrossEcosystemBlockEventBridge();
    private final CrossEcosystemPlayerLifecycleBridge playerBridge = new CrossEcosystemPlayerLifecycleBridge();
    private final CrossEcosystemServerTickBridge tickBridge = new CrossEcosystemServerTickBridge();
    private final CrossEcosystemCombatEventBridge combatBridge = new CrossEcosystemCombatEventBridge();

    public UnifiedEcosystemServerRuntimeCoordinator() {
    }

    public CrossEcosystemBlockEventBridge blockBridge() {
        return blockBridge;
    }

    public CrossEcosystemPlayerLifecycleBridge playerBridge() {
        return playerBridge;
    }

    public CrossEcosystemServerTickBridge tickBridge() {
        return tickBridge;
    }

    public CrossEcosystemCombatEventBridge combatBridge() {
        return combatBridge;
    }
}
