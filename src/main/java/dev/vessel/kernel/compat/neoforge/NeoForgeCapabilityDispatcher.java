package dev.vessel.kernel.compat.neoforge;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dispatches modern NeoForge RegisterCapabilitiesEvent for blocks, entities, and items.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class NeoForgeCapabilityDispatcher {

    private final ConcurrentMap<String, List<Consumer<Object>>> capabilityRegistrars = new ConcurrentHashMap<>();

    public void registerCapabilityHandler(String capabilityCategory, Consumer<Object> handler) {
        Objects.requireNonNull(capabilityCategory, "capabilityCategory cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        capabilityRegistrars.computeIfAbsent(capabilityCategory, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void dispatchRegistration(String capabilityCategory, Object eventInstance) {
        if (capabilityCategory == null || eventInstance == null) {
            return;
        }
        List<Consumer<Object>> handlers = capabilityRegistrars.get(capabilityCategory);
        if (handlers != null) {
            for (Consumer<Object> handler : handlers) {
                try {
                    handler.accept(eventInstance);
                } catch (Exception ignored) {
                    // Prevent rogue mod failure from aborting entire event chain
                }
            }
        }
    }

    public int getRegistrarCount(String capabilityCategory) {
        if (capabilityCategory == null) {
            return 0;
        }
        List<Consumer<Object>> handlers = capabilityRegistrars.get(capabilityCategory);
        return handlers != null ? handlers.size() : 0;
    }
}
