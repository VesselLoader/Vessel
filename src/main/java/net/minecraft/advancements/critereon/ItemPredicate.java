package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Universal multi-era compatibility shim providing the legacy {@code net.minecraft.advancements.critereon.ItemPredicate}
 * API surface expected by Minecraft 1.20.1 Forge and Fabric mods (such as Mekanism).
 *
 * <p>In modern Minecraft (1.20.5+ / 1.21+ / 26.3), Mojang transitioned predicate structures to
 * {@code net.minecraft.advancements.predicates.ItemPredicate} using Codecs rather than static deserializer registries.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ItemPredicate {

    /**
     * Singleton instance matching any item stack, matching the Minecraft 1.20.1 API contract.
     */
    public static final ItemPredicate ANY = new ItemPredicate();

    private static final ConcurrentMap<ResourceLocation, Function<JsonObject, ? extends ItemPredicate>> CUSTOM_DESERIALIZERS =
            new ConcurrentHashMap<>();

    /**
     * Default constructor for item predicate matching.
     */
    public ItemPredicate() {
    }

    /**
     * Registers a custom item predicate deserializer under the provided resource location.
     *
     * @param id           identifier for the custom predicate type
     * @param deserializer JSON deserialization function
     */
    public static void register(
            ResourceLocation id,
            Function<JsonObject, ? extends ItemPredicate> deserializer) {
        if (id != null && deserializer != null) {
            CUSTOM_DESERIALIZERS.put(id, deserializer);
        }
    }

    /**
     * Matches the given item stack against this predicate.
     *
     * @param itemStack item stack to test
     * @return {@code true} if matching
     */
    public boolean matches(ItemStack itemStack) {
        return true;
    }

    /**
     * SRG mapped method alias for {@link #matches(ItemStack)}.
     *
     * @param itemStack item stack to test
     * @return {@code true} if matching
     */
    public boolean m_45049_(ItemStack itemStack) {
        return matches(itemStack);
    }

    /**
     * Serializes this predicate into a JSON element.
     *
     * @return JSON representation of this predicate
     */
    public JsonElement serializeToJson() {
        return new JsonObject();
    }

    /**
     * SRG mapped method alias for {@link #serializeToJson()}.
     *
     * @return JSON representation
     */
    public JsonElement m_45048_() {
        return serializeToJson();
    }

    /**
     * Deserializes an item predicate from JSON.
     *
     * @param json element to parse
     * @return parsed predicate or {@link #ANY}
     */
    public static ItemPredicate fromJson(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ANY;
        }
        return new ItemPredicate();
    }

    /**
     * SRG mapped method alias for {@link #fromJson(JsonElement)}.
     *
     * @param json element to parse
     * @return parsed predicate
     */
    public static ItemPredicate m_45051_(JsonElement json) {
        return fromJson(json);
    }
}
