package net.fabricmc.fabric.api.itemgroup.v1;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canonical Fabric Item Group API v1 mutable creative tab entries collector
 * ({@code net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class FabricItemGroupEntries {

    private final List<String> entries = new CopyOnWriteArrayList<>();

    /**
     * Constructs an empty {@code FabricItemGroupEntries} collector.
     */
    public FabricItemGroupEntries() {
    }

    /**
     * Appends an item identifier to the end of this creative item group.
     *
     * @param itemIdentifier non-null namespaced item identifier
     */
    public void add(String itemIdentifier) {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier must not be null");
        entries.add(itemIdentifier);
    }

    /**
     * Inserts an item identifier immediately after {@code anchorItem} if present,
     * or appends it to the end if {@code anchorItem} is not found.
     *
     * @param anchorItem     existing anchor item identifier
     * @param itemIdentifier non-null namespaced item identifier to insert
     */
    public void addAfter(String anchorItem, String itemIdentifier) {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier must not be null");
        if (anchorItem == null) {
            entries.add(itemIdentifier);
            return;
        }
        int anchorIndex = entries.indexOf(anchorItem);
        if (anchorIndex >= 0 && anchorIndex + 1 <= entries.size()) {
            entries.add(anchorIndex + 1, itemIdentifier);
        } else {
            entries.add(itemIdentifier);
        }
    }

    /**
     * Returns an unmodifiable snapshot of the ordered item identifiers in this item group.
     *
     * @return unmodifiable list of item identifiers
     */
    public List<String> getDisplayStacks() {
        return Collections.unmodifiableList(entries);
    }
}
