package net.fabricmc.fabric.api.resource;

/**
 * Canonical Fabric Resource Loader API identifiable reload listener contract
 * ({@code net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IdentifiableResourceReloadListener {

    /**
     * Returns the unique namespaced Fabric identifier of this resource reload listener.
     *
     * @return non-null namespaced identifier
     */
    String getFabricId();

    /**
     * Invoked when resources for the associated {@link ResourceType} are reloaded.
     */
    void onResourceReload();
}
