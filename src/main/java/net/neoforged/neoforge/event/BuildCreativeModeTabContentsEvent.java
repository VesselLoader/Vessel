package net.neoforged.neoforge.event;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.CreativeModeTab;

/**
 * NeoForge mod-bus creative tab population event
 * ({@code net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent}) allowing mods to append
 * items to a {@link CreativeModeTab}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BuildCreativeModeTabContentsEvent {

    private final CreativeModeTab tab;
    private final String tabKey;

    /**
     * Constructs a new {@code BuildCreativeModeTabContentsEvent} for the target {@link CreativeModeTab}.
     *
     * @param tab    non-null target creative mode tab
     * @param tabKey non-null canonical creative mode tab registry key
     */
    public BuildCreativeModeTabContentsEvent(CreativeModeTab tab, String tabKey) {
        this.tab = Objects.requireNonNull(tab, "tab must not be null");
        this.tabKey = Objects.requireNonNull(tabKey, "tabKey must not be null").trim();
    }

    /**
     * Returns the {@link CreativeModeTab} being populated.
     *
     * @return non-null creative mode tab
     */
    public CreativeModeTab getTab() {
        return tab;
    }

    /**
     * Returns the registry key of the {@link CreativeModeTab} being populated.
     *
     * @return non-null creative mode tab key
     */
    public String getTabKey() {
        return tabKey;
    }

    /**
     * Appends an item identifier to the target {@link CreativeModeTab}.
     *
     * @param itemIdentifier non-null item identifier
     */
    public void accept(String itemIdentifier) {
        tab.accept(itemIdentifier);
    }

    /**
     * Returns an unmodifiable view of the item identifiers currently added to the target tab.
     *
     * @return unmodifiable list of item identifiers
     */
    public List<String> getEntries() {
        return tab.getDisplayItems();
    }
}
