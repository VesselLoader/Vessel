package dev.vessel.kernel.adapter.forge;

/**
 * Gatekeeper stage controlling lifecycle access windows for Inter-Mod Communications (IMC).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum InterModCommsLifecycleStage {

    /**
     * Uninitialized stage before IMC becomes operational.
     */
    IDLE,

    /**
     * Window during InterModEnqueueEvent when sending messages is permitted.
     */
    ENQUEUE_OPEN,

    /**
     * Window during InterModProcessEvent when receiving/consuming messages is permitted.
     */
    PROCESS_OPEN,

    /**
     * Terminal stage where IMC router is closed for the runtime session.
     */
    CLOSED
}
