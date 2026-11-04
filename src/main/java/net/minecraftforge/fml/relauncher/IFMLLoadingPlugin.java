package net.minecraftforge.fml.relauncher;

import java.util.Map;

/**
 * Universal cross-era shim for Forge 1.12.2 IFMLLoadingPlugin interface.
 * Enables legacy Forge CoreMods to initialize on modern VESSEL microkernel without fatal bytecode halt.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface IFMLLoadingPlugin {

    /**
     * Returns an array of ASM class transformer names.
     *
     * @return transformer class names
     */
    String[] getASMTransformerClass();

    /**
     * Returns custom ModContainer class name.
     *
     * @return container class name
     */
    String getModContainerClass();

    /**
     * Returns setup class name.
     *
     * @return setup class name
     */
    String getSetupClass();

    /**
     * Injects runtime coremod environment data.
     *
     * @param data environment properties map
     */
    void injectData(Map<String, Object> data);

    /**
     * Returns access transformer class name.
     *
     * @return access transformer name
     */
    String getAccessTransformerClass();
}
