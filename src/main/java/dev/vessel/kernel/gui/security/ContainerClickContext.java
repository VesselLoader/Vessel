package dev.vessel.kernel.gui.security;

import java.util.Objects;

/**
 * Immutable context snapshot representing a container click or interaction packet.
 * Captures all relevant state parameters required for anti-theft security evaluation
 * without introducing dependencies on external loader or internal NMS classes.
 *
 * @param slotIndex             the target slot index in the container
 * @param button                the mouse or keyboard button code
 * @param clickTypeName         the protocol click type name (e.g. "PICKUP", "SWAP", "THROW")
 * @param playerId              the unique identifier of the interacting player
 * @param targetItemId          identifier of the item currently sitting in the slot
 * @param targetItemVirtual     whether the item in the slot is flagged as a virtual UI component
 * @param carriedItemId         identifier of the item currently held on the player's cursor
 * @param carriedItemVirtual    whether the carried item is flagged as a virtual UI component
 * @param containerSize         total number of valid slots in the open container window
 * @param windowClosing         whether the interaction coincides with a window close or disconnect
 * @param outOfBounds           whether the click occurred outside the container bounds (e.g. slot -999)
 * @param rapidBurstDetected    whether automated high-frequency packet flood was detected
 * @param concurrentRaceDetected whether a concurrent modification conflict was detected
 * @param bedrockClient         whether the interacting player is connecting from Bedrock edition
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ContainerClickContext(
        int slotIndex,
        int button,
        String clickTypeName,
        String playerId,
        String targetItemId,
        boolean targetItemVirtual,
        String carriedItemId,
        boolean carriedItemVirtual,
        int containerSize,
        boolean windowClosing,
        boolean outOfBounds,
        boolean rapidBurstDetected,
        boolean concurrentRaceDetected,
        boolean bedrockClient
) {

    public static final String CLICK_TYPE_PICKUP = "PICKUP";
    public static final String CLICK_TYPE_QUICK_MOVE = "QUICK_MOVE";
    public static final String CLICK_TYPE_SWAP = "SWAP";
    public static final String CLICK_TYPE_CLONE = "CLONE";
    public static final String CLICK_TYPE_THROW = "THROW";
    public static final String CLICK_TYPE_QUICK_CRAFT = "QUICK_CRAFT";
    public static final String CLICK_TYPE_PICKUP_ALL = "PICKUP_ALL";

    public static final String ITEM_EMPTY = "minecraft:air";
    public static final int OUT_OF_BOUNDS_SLOT_ID = -999;
    public static final int OFFHAND_BUTTON_CODE = 40;

    public ContainerClickContext {
        Objects.requireNonNull(clickTypeName, "clickTypeName cannot be null");
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(targetItemId, "targetItemId cannot be null");
        Objects.requireNonNull(carriedItemId, "carriedItemId cannot be null");
    }

    /**
     * Creates a standard context for common slot clicks.
     */
    public static ContainerClickContext ofClick(
            int slotIndex,
            int button,
            String clickTypeName,
            String playerId,
            String targetItemId,
            boolean targetItemVirtual,
            String carriedItemId,
            boolean carriedItemVirtual,
            int containerSize
    ) {
        return new ContainerClickContext(
                slotIndex,
                button,
                clickTypeName,
                playerId,
                targetItemId,
                targetItemVirtual,
                carriedItemId,
                carriedItemVirtual,
                containerSize,
                false,
                slotIndex == OUT_OF_BOUNDS_SLOT_ID,
                false,
                false,
                false
        );
    }

    /**
     * Creates a window closing context for cursor neutralization checks.
     */
    public static ContainerClickContext ofWindowClose(
            String playerId,
            String carriedItemId,
            boolean carriedItemVirtual,
            int containerSize
    ) {
        return new ContainerClickContext(
                -1,
                0,
                "WINDOW_CLOSE",
                playerId,
                ITEM_EMPTY,
                false,
                carriedItemId,
                carriedItemVirtual,
                containerSize,
                true,
                false,
                false,
                false,
                false
        );
    }
}
