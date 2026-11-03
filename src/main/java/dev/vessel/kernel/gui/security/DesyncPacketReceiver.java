package dev.vessel.kernel.gui.security;

/**
 * Clean-room callback receiver for outbound packet synchronization events
 * generated during immediate inventory desync flushes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface DesyncPacketReceiver {

    /**
     * Invoked when a specific slot update packet must be sent to the client.
     *
     * @param playerId the target player id
     * @param windowId the container window id
     * @param slotIndex the slot index in the window
     * @param itemId the canonical item id to set
     * @param count the item count
     */
    void onSlotFlush(String playerId, int windowId, int slotIndex, String itemId, int count);

    /**
     * Invoked when the carried cursor item update packet must be sent to the client.
     *
     * @param playerId the target player id
     * @param windowId the container window id
     * @param carriedItemId the canonical item id on the cursor
     * @param count the item count
     */
    void onCarriedFlush(String playerId, int windowId, String carriedItemId, int count);
}
