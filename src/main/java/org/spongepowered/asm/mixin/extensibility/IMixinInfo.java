package org.spongepowered.asm.mixin.extensibility;

/**
 * Headless server-safe stub for SpongePowered IMixinInfo.
 * Allows Quilt and Fabric Mixin plugins to inspect mixin metadata.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface IMixinInfo {

    /**
     * Returns mixin name.
     *
     * @return mixin name
     */
    String getName();

    /**
     * Returns target class name.
     *
     * @return class name
     */
    String getClassName();

    /**
     * Returns internal class reference name.
     *
     * @return class reference
     */
    String getClassRef();

    /**
     * Returns raw bytecode of the mixin.
     *
     * @return byte array
     */
    byte[] getClassBytes();

    /**
     * Checks if mixin is detached.
     *
     * @return true if detached
     */
    boolean isDetached();

    /**
     * Returns mixin application priority.
     *
     * @return integer priority
     */
    int getPriority();

    /**
     * Returns configuration plugin.
     *
     * @return mixin config plugin
     */
    IMixinConfigPlugin getPlugin();
}
