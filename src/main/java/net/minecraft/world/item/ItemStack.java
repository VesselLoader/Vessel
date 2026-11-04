package net.minecraft.world.item;

/**
 * Universal multi-era compatibility stub for {@code net.minecraft.world.item.ItemStack}.
 *
 * <p>At runtime, this class is shadowed by the real Minecraft server runtime class loaded
 * child-first via {@code TierOneGameTransformingClassLoader}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ItemStack {

    /**
     * Default empty item stack representation.
     */
    public static final ItemStack EMPTY = new ItemStack();

    /**
     * Default constructor.
     */
    public ItemStack() {
    }

    /**
     * Returns whether this item stack is empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }
}
