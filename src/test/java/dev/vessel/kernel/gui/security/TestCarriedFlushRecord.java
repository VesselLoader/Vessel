package dev.vessel.kernel.gui.security;

/**
 * Top-level test record for carried flush events.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TestCarriedFlushRecord(String playerId, int windowId, String carriedItemId, int count) {}
