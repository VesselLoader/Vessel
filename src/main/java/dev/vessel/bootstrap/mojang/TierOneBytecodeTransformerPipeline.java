package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.mixin.weaver.RuntimeMixinTargetRegistry;
import java.util.Objects;

/**
 * Unified execution pipeline chaining ASM bytecode transformers for the Tier-1 Game ClassLoader.
 * Eliminates redundant class-retrieval boilerplate while strictly isolating each transformation stage.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TierOneBytecodeTransformerPipeline {

    private final VesselServerBrandTransformer brandTransformer = new VesselServerBrandTransformer();
    private final VesselServerLifecycleTransformer lifecycleTransformer = new VesselServerLifecycleTransformer();
    private final VesselServerTickTransformer serverTickTransformer = new VesselServerTickTransformer();
    private final VesselCommandsTransformer commandsTransformer = new VesselCommandsTransformer();
    private final VesselCommandSourceStackTransformer commandSourceStackTransformer = new VesselCommandSourceStackTransformer();
    private final VesselResourceKeyTransformer resourceKeyTransformer = new VesselResourceKeyTransformer();
    private final VesselDyeColorTransformer dyeColorTransformer = new VesselDyeColorTransformer();
    private final VesselTextColorTransformer textColorTransformer = new VesselTextColorTransformer();
    private final VesselMapColorTransformer mapColorTransformer = new VesselMapColorTransformer();
    private final VesselStyleTransformer styleTransformer = new VesselStyleTransformer();
    private final VesselComponentTransformer componentTransformer = new VesselComponentTransformer();
    private final VesselMutableComponentTransformer mutableComponentTransformer = new VesselMutableComponentTransformer();
    private final VesselIngredientTransformer ingredientTransformer = new VesselIngredientTransformer();
    private final VesselEntityOptimizationTransformer entityOptimizationTransformer = new VesselEntityOptimizationTransformer();
    private final VesselExplosionTransformer explosionTransformer = new VesselExplosionTransformer();

    public TierOneBytecodeTransformerPipeline() {
    }

    /**
     * Determines whether the given class is targeted by any active transformer in this pipeline.
     *
     * @param className     fully qualified binary class name
     * @param mixinRegistry optional runtime mixin target registry
     * @return {@code true} if at least one transformer targets this class
     */
    public boolean isTransformedClass(String className, RuntimeMixinTargetRegistry mixinRegistry) {
        if (className == null) {
            return false;
        }
        return VesselServerBrandTransformer.isServerClass(className)
                || VesselCommandsTransformer.isCommandsClass(className)
                || VesselCommandSourceStackTransformer.isCommandSourceStackClass(className)
                || VesselResourceKeyTransformer.isResourceKeyClass(className)
                || VesselDyeColorTransformer.isDyeColorClass(className)
                || VesselTextColorTransformer.isTextColorClass(className)
                || VesselMapColorTransformer.isMapColorClass(className)
                || VesselStyleTransformer.isStyleClass(className)
                || VesselComponentTransformer.isComponentClass(className)
                || VesselMutableComponentTransformer.isMutableComponentClass(className)
                || VesselIngredientTransformer.isIngredientClass(className)
                || VesselEntityOptimizationTransformer.isEntityClass(className)
                || VesselExplosionTransformer.isExplosionClass(className)
                || (mixinRegistry != null && mixinRegistry.isRegisteredTarget(className));
    }

    /**
     * Executes the ordered transformation chain on the input bytecode for the specified class.
     *
     * @param className     fully qualified binary class name
     * @param originalBytes raw class bytecode
     * @param mixinRegistry optional runtime mixin target registry
     * @return transformed bytecode
     */
    public byte[] transformClassBytecode(
            String className,
            byte[] originalBytes,
            RuntimeMixinTargetRegistry mixinRegistry) {
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(originalBytes, "originalBytes must not be null");

        byte[] current = originalBytes;

        if (VesselServerBrandTransformer.isServerClass(className)) {
            current = brandTransformer.transformServerBrand(current);
            current = lifecycleTransformer.transformServerLifecycle(current);
            current = serverTickTransformer.transformServerTick(current);
        }

        if (VesselCommandsTransformer.isCommandsClass(className)) {
            current = commandsTransformer.transformCommands(current);
        }

        if (VesselCommandSourceStackTransformer.isCommandSourceStackClass(className)) {
            current = commandSourceStackTransformer.transformCommandSourceStack(current);
        }

        if (VesselResourceKeyTransformer.isResourceKeyClass(className)) {
            current = resourceKeyTransformer.transformResourceKey(current);
        }

        if (VesselDyeColorTransformer.isDyeColorClass(className)) {
            current = dyeColorTransformer.transformDyeColor(current);
        }

        if (VesselTextColorTransformer.isTextColorClass(className)) {
            current = textColorTransformer.transformTextColor(current);
        }

        if (VesselMapColorTransformer.isMapColorClass(className)) {
            current = mapColorTransformer.transformMapColor(current);
        }

        if (VesselStyleTransformer.isStyleClass(className)) {
            current = styleTransformer.transformStyle(current);
        }

        if (VesselComponentTransformer.isComponentClass(className)) {
            current = componentTransformer.transformComponent(current);
        }

        if (VesselMutableComponentTransformer.isMutableComponentClass(className)) {
            current = mutableComponentTransformer.transformMutableComponent(current);
        }

        if (VesselIngredientTransformer.isIngredientClass(className)) {
            current = ingredientTransformer.transformIngredient(current);
        }

        if (VesselEntityOptimizationTransformer.isEntityClass(className)) {
            current = entityOptimizationTransformer.transformEntity(current);
        }

        if (VesselExplosionTransformer.isExplosionClass(className)) {
            current = explosionTransformer.transformExplosion(current);
        }

        if (mixinRegistry != null && mixinRegistry.isRegisteredTarget(className)) {
            current = mixinRegistry.applyRegisteredWidenersIfTarget(current);
        }

        return current;
    }

    public VesselServerBrandTransformer getBrandTransformer() {
        return brandTransformer;
    }

    public VesselServerLifecycleTransformer getLifecycleTransformer() {
        return lifecycleTransformer;
    }

    public VesselServerTickTransformer getServerTickTransformer() {
        return serverTickTransformer;
    }

    public VesselCommandsTransformer getCommandsTransformer() {
        return commandsTransformer;
    }

    public VesselCommandSourceStackTransformer getCommandSourceStackTransformer() {
        return commandSourceStackTransformer;
    }

    public VesselEntityOptimizationTransformer getEntityOptimizationTransformer() {
        return entityOptimizationTransformer;
    }

    public VesselExplosionTransformer getExplosionTransformer() {
        return explosionTransformer;
    }
}
