package net.minecraftforge.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * Universal multi-era event fired on MinecraftForge.EVENT_BUS when capabilities are attached to objects.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class AttachCapabilitiesEvent<T> extends Event {

    private final T object;

    public AttachCapabilitiesEvent(Class<T> type, T object) {
        this.object = object;
    }

    public AttachCapabilitiesEvent(T object) {
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    public void addCapability(Object key, Object capabilityProvider) {
    }
}
