package dev.vessel.kernel.gui.security;

/**
 * Top-level test record for slot flush events.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TestSlotFlushRecord(String playerId, int windowId, int slotIndex, String itemId, int count) {}
