package zone.rong.mixinbooter;

import java.util.List;

/**
 * Headless server-safe stub for Forge 1.12 MixinBooter IEarlyMixinLoader interface.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface IEarlyMixinLoader {

    /**
     * Returns list of mixin configuration file names to register.
     *
     * @return list of mixin json configuration paths
     */
    List<String> getMixinConfigs();
}
