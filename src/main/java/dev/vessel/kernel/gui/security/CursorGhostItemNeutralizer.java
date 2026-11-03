package dev.vessel.kernel.gui.security;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Neutralizes ghost items held on player cursor when container windows close,
 * clients disconnect, or game clients crash unexpectedly.
 * Prevents virtual GUI buttons and UI frames from being dropped into the world as real items.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CursorGhostItemNeutralizer {

    public static final String DEFAULT_VIRTUAL_ITEM_TAG = "VESSEL_VIRTUAL_ITEM";
    public static final String VIRTUAL_ITEM_PREFIX = "vessel:virtual_";

    private final String virtualItemTag;
    private final SlotImmediateDesyncFlusher desyncFlusher;
    private final AtomicLong neutralizedItemCounter = new AtomicLong(0);

    /**
     * Constructs a CursorGhostItemNeutralizer with default configuration.
     */
    public CursorGhostItemNeutralizer() {
        this(DEFAULT_VIRTUAL_ITEM_TAG, new SlotImmediateDesyncFlusher());
    }

    /**
     * Constructs a CursorGhostItemNeutralizer with explicit tag and flusher.
     *
     * @param virtualItemTag the metadata tag used to mark virtual items
     * @param desyncFlusher the desync flusher instance
     */
    public CursorGhostItemNeutralizer(String virtualItemTag, SlotImmediateDesyncFlusher desyncFlusher) {
        this.virtualItemTag = Objects.requireNonNull(virtualItemTag, "virtualItemTag cannot be null");
        this.desyncFlusher = Objects.requireNonNull(desyncFlusher, "desyncFlusher cannot be null");
    }

    public String getVirtualItemTag() {
        return virtualItemTag;
    }

    public long getNeutralizedItemCount() {
        return neutralizedItemCounter.get();
    }

    /**
     * Determines whether an item represents an untouchable virtual UI element.
     *
     * @param itemId canonical item identifier
     * @param hasVirtualMarker whether item holds the virtual component tag
     * @return true if item is virtual
     */
    public boolean isVirtualItem(String itemId, boolean hasVirtualMarker) {
        if (hasVirtualMarker) {
            return true;
        }
        if (itemId == null || itemId.isEmpty() || ContainerClickContext.ITEM_EMPTY.equals(itemId)) {
            return false;
        }
        return itemId.startsWith(VIRTUAL_ITEM_PREFIX);
    }

    /**
     * Evaluates and neutralizes the carried cursor stack upon container window closure.
     *
     * @param playerId interacting player id
     * @param carriedItemId canonical item id on cursor
     * @param hasVirtualMarker whether item holds the virtual marker tag
     * @param count stack quantity
     * @return neutralization outcome
     */
    public NeutralizationResult neutralizeCarriedOnClose(
            String playerId,
            String carriedItemId,
            boolean hasVirtualMarker,
            int count
    ) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(carriedItemId, "carriedItemId cannot be null");

        if (isVirtualItem(carriedItemId, hasVirtualMarker)) {
            neutralizedItemCounter.incrementAndGet();
            desyncFlusher.flushCarried(playerId, 0, ContainerClickContext.ITEM_EMPTY, 0);
            return NeutralizationResult.purged(carriedItemId);
        }

        return NeutralizationResult.preserved(carriedItemId, count);
    }
}
