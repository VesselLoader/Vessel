package net.fabricmc.fabric.api.resource;

/**
 * Canonical Fabric Resource Loader API resource pack domain type
 * ({@code net.fabricmc.fabric.api.resource.ResourceType}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum ResourceType {
    /**
     * Client-side assets ({@code assets/}).
     */
    CLIENT_RESOURCES,

    /**
     * Server-side data packs ({@code data/}).
     */
    SERVER_DATA
}
