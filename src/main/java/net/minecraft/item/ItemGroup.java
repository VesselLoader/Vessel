package net.minecraft.item;

/**
 * Universal cross-era shim for Forge 1.16.5 net.minecraft.item.ItemGroup.
 * In Minecraft 1.19+ MojMap, this was refactored into net.minecraft.world.item.CreativeModeTab.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class ItemGroup {

    private final String label;
    private final int id;

    public ItemGroup(String label) {
        this(0, label);
    }

    public ItemGroup(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getRecipeFolderName() {
        return this.label;
    }

    public int getId() {
        return this.id;
    }
}
