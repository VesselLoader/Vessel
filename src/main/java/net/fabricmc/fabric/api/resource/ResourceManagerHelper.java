package net.fabricmc.fabric.api.resource;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Fabric Resource Loader API helper for registering and triggering
 * {@link IdentifiableResourceReloadListener} instances per {@link ResourceType}
 * ({@code net.fabricmc.fabric.api.resource.ResourceManagerHelper}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ResourceManagerHelper {

    private static final Map<ResourceType, ResourceManagerHelper> HELPERS = new ConcurrentHashMap<>();

    private final ResourceType resourceType;
    private final Map<String, IdentifiableResourceReloadListener> listeners = new ConcurrentHashMap<>();

    /**
     * Constructs a {@code ResourceManagerHelper} for the specified {@link ResourceType}.
     *
     * @param resourceType non-null resource type
     */
    public ResourceManagerHelper(ResourceType resourceType) {
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType must not be null");
    }

    /**
     * Returns the singleton {@code ResourceManagerHelper} for the specified {@link ResourceType}.
     *
     * @param type non-null resource type
     * @return helper instance for {@code type}
     */
    public static ResourceManagerHelper get(ResourceType type) {
        Objects.requireNonNull(type, "type must not be null");
        ResourceManagerHelper existing = HELPERS.get(type);
        if (existing != null) {
            return existing;
        }
        ResourceManagerHelper created = new ResourceManagerHelper(type);
        ResourceManagerHelper raced = HELPERS.putIfAbsent(type, created);
        return raced != null ? raced : created;
    }

    /**
     * Returns the {@link ResourceType} managed by this helper.
     *
     * @return resource type
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Registers an {@link IdentifiableResourceReloadListener} under its {@link IdentifiableResourceReloadListener#getFabricId()}.
     *
     * @param listener non-null reload listener
     */
    public void registerReloadListener(IdentifiableResourceReloadListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        String id = Objects.requireNonNull(listener.getFabricId(), "fabricId must not be null");
        listeners.put(id, listener);
    }

    /**
     * Returns whether a reload listener with the given Fabric ID is registered.
     *
     * @param fabricId listener identifier
     * @return {@code true} if registered
     */
    public boolean isRegistered(String fabricId) {
        return fabricId != null && listeners.containsKey(fabricId);
    }

    /**
     * Triggers {@link IdentifiableResourceReloadListener#onResourceReload()} on all registered
     * listeners for this {@link ResourceType}.
     *
     * @return number of reload listeners invoked
     */
    public int triggerReloadAll() {
        int count = 0;
        for (IdentifiableResourceReloadListener listener : listeners.values()) {
            listener.onResourceReload();
            count++;
        }
        return count;
    }

    /**
     * Clears all registered reload listeners for this {@link ResourceType}.
     */
    public void clearListeners() {
        listeners.clear();
    }
}
