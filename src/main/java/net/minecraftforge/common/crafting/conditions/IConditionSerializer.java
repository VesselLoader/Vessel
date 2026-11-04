package net.minecraftforge.common.crafting.conditions;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * Serializer contract for JSON-backed Forge crafting and recipe condition types.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <T> condition implementation type
 */
public interface IConditionSerializer<T extends ICondition> {

    void write(JsonObject json, T value);

    T read(JsonObject json);

    ResourceLocation getID();
}
