package dev.vessel.kernel.vstm.builder;

import dev.vessel.kernel.vstm.engine.VstmDialectBridgeRouter;
import dev.vessel.kernel.vstm.engine.VstmResolutionFallbackChain;
import dev.vessel.kernel.vstm.engine.VstmTransmutationFastLookup;
import java.util.Objects;

/**
 * Encapsulates the active transmutation context for a specific mod JAR or ecosystem domain.
 *
 * <p>Coordinates fast-path $O(1)$ off-heap lookups with dialect bridge routing
 * and synthetic fallback chains, maintaining thread-safe immutable context per module (Catalog ref: FOL-0040).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmTransmutationContext {

    private final String moduleId;
    private final int sourceDialect;
    private final int targetDialect;
    private final VstmTransmutationFastLookup fastLookup;
    private final VstmResolutionFallbackChain fallbackChain;
    private final VstmDialectBridgeRouter bridgeRouter;

    /**
     * Constructs an active module transmutation context.
     *
     * @param moduleId      the unique identifier of the module (e.g. "fabric:create:0.5.1")
     * @param sourceDialect the input mapping dialect
     * @param targetDialect the output mapping dialect
     * @param fastLookup    the fast lookup engine
     * @param fallbackChain the fallback resolution chain
     * @param bridgeRouter  the cross-dialect bridge router
     */
    public VstmTransmutationContext(
            String moduleId,
            int sourceDialect,
            int targetDialect,
            VstmTransmutationFastLookup fastLookup,
            VstmResolutionFallbackChain fallbackChain,
            VstmDialectBridgeRouter bridgeRouter
    ) {
        this.moduleId = Objects.requireNonNull(moduleId, "moduleId must not be null");
        this.sourceDialect = sourceDialect;
        this.targetDialect = targetDialect;
        this.fastLookup = Objects.requireNonNull(fastLookup, "fastLookup must not be null");
        this.fallbackChain = Objects.requireNonNull(fallbackChain, "fallbackChain must not be null");
        this.bridgeRouter = Objects.requireNonNull(bridgeRouter, "bridgeRouter must not be null");
    }

    /**
     * Remaps a class internal name from source dialect to target dialect.
     *
     * @param internalName class internal name (e.g., "net/minecraft/src/C_1234_")
     * @return remapped class name
     */
    public String remapClass(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return internalName;
        }
        return bridgeRouter.routeClass(internalName, sourceDialect, targetDialect, fastLookup);
    }

    /**
     * Remaps a method name using fast lookup and fallback resolution.
     *
     * @param owner      the owner class internal name
     * @param name       the method name
     * @param descriptor the method descriptor
     * @return remapped method name
     */
    public String remapMethod(String owner, String name, String descriptor) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return bridgeRouter.routeMethod(owner, name, descriptor, sourceDialect, targetDialect, fastLookup, fallbackChain);
    }

    /**
     * Remaps a field name using fast lookup and fallback resolution.
     *
     * @param owner      the owner class internal name
     * @param name       the field name
     * @param descriptor the field descriptor
     * @return remapped field name
     */
    public String remapField(String owner, String name, String descriptor) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return bridgeRouter.routeField(owner, name, descriptor, sourceDialect, targetDialect, fastLookup, fallbackChain);
    }

    /**
     * Remaps a method descriptor if a remapped descriptor is recorded.
     *
     * @param owner      the owner class internal name
     * @param name       the method name
     * @param descriptor original method descriptor
     * @return remapped descriptor, or original if unmapped
     */
    public String remapMethodDescriptor(String owner, String name, String descriptor) {
        String mapped = fastLookup.lookupMethodDescriptor(owner, name, descriptor, targetDialect);
        return mapped != null ? mapped : descriptor;
    }

    public String getModuleId() {
        return moduleId;
    }

    public int getSourceDialect() {
        return sourceDialect;
    }

    public int getTargetDialect() {
        return targetDialect;
    }

    public VstmTransmutationFastLookup getFastLookup() {
        return fastLookup;
    }

    public VstmResolutionFallbackChain getFallbackChain() {
        return fallbackChain;
    }

    public VstmDialectBridgeRouter getBridgeRouter() {
        return bridgeRouter;
    }
}
