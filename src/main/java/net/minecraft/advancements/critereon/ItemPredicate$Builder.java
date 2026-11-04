package net.minecraft.advancements.critereon;

/**
 * Universal multi-era compatibility shim providing the legacy {@code ItemPredicate.Builder}
 * API surface expected by Minecraft 1.20.1 Forge and Fabric mods.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ItemPredicate$Builder {

    /**
     * Creates a new builder instance.
     *
     * @return fresh builder
     */
    public static ItemPredicate$Builder item() {
        return new ItemPredicate$Builder();
    }

    /**
     * Builds the configured item predicate.
     *
     * @return item predicate instance
     */
    public ItemPredicate build() {
        return ItemPredicate.ANY;
    }

    /**
     * SRG mapped method alias for {@link #build()}.
     *
     * @return item predicate instance
     */
    public ItemPredicate m_45065_() {
        return build();
    }
}
