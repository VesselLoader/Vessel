package net.minecraft.world.item;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Clean-room Minecraft creative inventory tab representation
 * ({@code net.minecraft.world.item.CreativeModeTab}) storing tab metadata and display item keys.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CreativeModeTab {

    private final String id;
    private final String title;
    private final List<String> displayItems = new CopyOnWriteArrayList<>();

    /**
     * Constructs a new {@code CreativeModeTab} with the specified namespaced identifier and title.
     *
     * @param id    non-null tab identifier (e.g., {@code "minecraft:building_blocks"})
     * @param title non-null localized or translation-key title
     */
    public CreativeModeTab(String id, String title) {
        this.id = Objects.requireNonNull(id, "id must not be null").trim();
        this.title = Objects.requireNonNull(title, "title must not be null").trim();
    }

    /**
     * Returns the namespaced identifier of this creative mode tab.
     *
     * @return non-null tab identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the display title or translation key of this creative mode tab.
     *
     * @return non-null tab title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Appends an item identifier to this creative mode tab's display list.
     *
     * @param itemIdentifier non-null namespaced item identifier
     */
    public void accept(String itemIdentifier) {
        Objects.requireNonNull(itemIdentifier, "itemIdentifier must not be null");
        displayItems.add(itemIdentifier.trim());
    }

    /**
     * Returns an unmodifiable view of all item identifiers displayed in this creative mode tab.
     *
     * @return unmodifiable list of item identifiers
     */
    public List<String> getDisplayItems() {
        return Collections.unmodifiableList(displayItems);
    }
}
