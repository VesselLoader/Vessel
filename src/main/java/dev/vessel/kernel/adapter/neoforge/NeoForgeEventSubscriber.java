package dev.vessel.kernel.adapter.neoforge;

/**
 * Functional callback interface for handling NeoForge events.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface NeoForgeEventSubscriber {

    /**
     * Consumes an incoming event notification.
     *
     * @param event the event payload object
     * @throws Exception if an error occurs in the subscriber logic
     */
    void onEvent(Object event) throws Exception;
}
