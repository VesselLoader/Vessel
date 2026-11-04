package net.minecraftforge.event;

/**
 * Universal cross-era shim for Forge RegistryEvent$Register.
 * Fired when registries are ready to accept entries.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @param <T> registered entry type
 * @version 1.0.0
 */
public class RegistryEvent$Register<T> extends RegistryEvent<T> {

    public RegistryEvent$Register() {
        super();
    }
}
