package dev.vessel.kernel.gui.security;

/**
 * Result decision returned by the Virtual Slot Theft Guard.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum SecurityDecision {

    /**
     * Interaction is fully permitted and should be forwarded to mod machine or inventory logic.
     */
    ALLOW,

    /**
     * Interaction is strictly prohibited. The operation must be cancelled immediately
     * and an urgent slot desync flush dispatched to the client.
     */
    REJECT_AND_FLUSH,

    /**
     * Interaction is recognized as an action button trigger. The physical item mutation
     * is cancelled, but the registered UI button callback is executed.
     */
    TRIGGER_ACTION_ONLY;

    public boolean isAllowed() {
        return this == ALLOW;
    }

    public boolean isRejected() {
        return this == REJECT_AND_FLUSH;
    }

    public boolean isTriggerOnly() {
        return this == TRIGGER_ACTION_ONLY;
    }
}
