package net.fabricmc.fabric.api.event.lifecycle.v1;

/**
 * Canonical Fabric Lifecycle Events v1 server lifecycle &amp; tick callback contract
 * ({@code net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface ServerLifecycleCallback {

    /**
     * Invoked when a server lifecycle transition or tick boundary occurs.
     *
     * @param serverInstance active server instance handle
     */
    void onServerLifecycle(Object serverInstance);
}
