package net.minecraftforge.common.crafting.conditions;

import net.minecraft.resources.ResourceLocation;

/**
 * Root interface representing Forge recipe and data-pack conditional predicates.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ICondition {

    ResourceLocation getID();

    boolean test(ICondition$IContext context);
}
