package org.spongepowered.asm.mixin.extensibility;

import java.util.List;
import java.util.Set;

/**
 * Headless server-safe stub for SpongePowered IMixinConfigPlugin.
 * Allows Quilt and Fabric Mixin plugins (such as LambdaBetterGrass) to load configuration safely.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface IMixinConfigPlugin {

    /**
     * Called when the mixin configuration is loaded.
     *
     * @param mixinPackage package prefix
     */
    void onLoad(String mixinPackage);

    /**
     * Returns refmap configuration file path.
     *
     * @return refmap path
     */
    String getRefMapperConfig();

    /**
     * Determines whether to apply mixin to target class.
     *
     * @param targetClassName target class name
     * @param mixinClassName  mixin class name
     * @return true to apply
     */
    boolean shouldApplyMixin(String targetClassName, String mixinClassName);

    /**
     * Called when targets are accepted.
     *
     * @param myTargets    plugin target classes
     * @param otherTargets other target classes
     */
    void acceptTargets(Set<String> myTargets, Set<String> otherTargets);

    /**
     * Returns extra mixin classes to apply.
     *
     * @return list of mixin class names
     */
    List<String> getMixins();

    /**
     * Hook called before mixin application.
     */
    void preApply(String targetClassName, Object targetClass, String mixinClassName, Object mixinInfo);

    /**
     * Hook called after mixin application.
     */
    void postApply(String targetClassName, Object targetClass, String mixinClassName, Object mixinInfo);
}
