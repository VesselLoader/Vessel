package dev.vessel.kernel.modelengine.driver;

import java.util.Objects;

/**
 * Immutable record of an item drop produced upon driver mob death.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param itemId item namespaced identifier (e.g. "mymod:dragon_scale")
 * @param count  quantity of items dropped
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DroppedItemEntry(String itemId, int count) {

    /**
     * Constructs a DroppedItemEntry with validation.
     */
    public DroppedItemEntry {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive: " + count);
        }
    }
}
