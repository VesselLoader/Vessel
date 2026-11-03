package dev.vessel.kernel.gui.security;

import java.util.Objects;

/**
 * Immutable interaction security policy defining allowed and prohibited operations
 * for a specific container slot classification.
 *
 * @param allowPickup           whether player can pick up items from this slot
 * @param allowPlace            whether player can place items into this slot
 * @param allowHotbarSwap       whether hotbar keys (1..9) can swap with this slot
 * @param allowOffhandSwap      whether offhand key (F) can swap with this slot
 * @param allowQuickMoveShift   whether shift-click quick move can interact with this slot
 * @param allowDropThrow        whether drop keys (Q / Ctrl+Q) can throw items from this slot
 * @param allowPickupAllDouble  whether double-click collect all can collect from this slot
 * @param allowDragSplit        whether mouse drag splitting can distribute items across this slot
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VirtualSlotSecurityPolicy(
        boolean allowPickup,
        boolean allowPlace,
        boolean allowHotbarSwap,
        boolean allowOffhandSwap,
        boolean allowQuickMoveShift,
        boolean allowDropThrow,
        boolean allowPickupAllDouble,
        boolean allowDragSplit
) {

    public static final VirtualSlotSecurityPolicy BUTTON_POLICY = new VirtualSlotSecurityPolicy(
            false, false, false, false, false, false, false, false
    );

    public static final VirtualSlotSecurityPolicy PROGRESS_BAR_POLICY = new VirtualSlotSecurityPolicy(
            false, false, false, false, false, false, false, false
    );

    public static final VirtualSlotSecurityPolicy DECORATIVE_FRAME_POLICY = new VirtualSlotSecurityPolicy(
            false, false, false, false, false, false, false, false
    );

    public static final VirtualSlotSecurityPolicy MOD_INPUT_POLICY = new VirtualSlotSecurityPolicy(
            true, true, true, true, true, true, true, true
    );

    public static final VirtualSlotSecurityPolicy MOD_OUTPUT_POLICY = new VirtualSlotSecurityPolicy(
            true, false, false, false, true, true, true, false
    );

    public static final VirtualSlotSecurityPolicy MOD_STORAGE_POLICY = new VirtualSlotSecurityPolicy(
            true, true, true, true, true, true, true, true
    );

    public static final VirtualSlotSecurityPolicy REAL_PLAYER_BACKPACK_POLICY = new VirtualSlotSecurityPolicy(
            true, true, true, true, true, true, true, true
    );

    /**
     * Resolves the standard security policy for the specified slot classification.
     *
     * @param classification the slot classification
     * @return corresponding immutable security policy
     */
    public static VirtualSlotSecurityPolicy forClassification(VirtualSlotClassification classification) {
        Objects.requireNonNull(classification, "classification cannot be null");
        return switch (classification) {
            case BUTTON -> BUTTON_POLICY;
            case PROGRESS_BAR -> PROGRESS_BAR_POLICY;
            case DECORATIVE_FRAME -> DECORATIVE_FRAME_POLICY;
            case FUNCTIONAL_MOD_INPUT -> MOD_INPUT_POLICY;
            case FUNCTIONAL_MOD_OUTPUT -> MOD_OUTPUT_POLICY;
            case FUNCTIONAL_MOD_STORAGE -> MOD_STORAGE_POLICY;
            case REAL_PLAYER_BACKPACK -> REAL_PLAYER_BACKPACK_POLICY;
        };
    }
}
