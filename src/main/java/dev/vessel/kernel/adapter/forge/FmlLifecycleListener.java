package dev.vessel.kernel.adapter.forge;

/**
 * Functional contract for subscribers listening to FML mod lifecycle transitions.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface FmlLifecycleListener {

    /**
     * Handles an incoming FML lifecycle event.
     *
     * @param event the lifecycle event notification
     * @throws Exception if an error occurs during listener execution
     */
    void onLifecycleEvent(FmlLifecycleEvent event) throws Exception;
}
