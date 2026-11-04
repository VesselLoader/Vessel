package net.minecraftforge.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal cross-era shim for Forge RegistryEvent.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @param <T> registered entry type
 * @version 1.0.0
 */
public class RegistryEvent<T> extends Event {

    public RegistryEvent() {
    }
}
