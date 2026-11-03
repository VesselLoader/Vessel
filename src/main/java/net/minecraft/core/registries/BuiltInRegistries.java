package net.minecraft.core.registries;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Built-in vanilla Minecraft registries ({@code net.minecraft.core.registries.BuiltInRegistries})
 * pre-populated with baseline block, item, entity type, sound event, and creative tab registries.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BuiltInRegistries {

    /**
     * Built-in item registry ({@code minecraft:item}).
     */
    public static final Registry<String> ITEM = new Registry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "item")));

    /**
     * Built-in block registry ({@code minecraft:block}).
     */
    public static final Registry<String> BLOCK = new Registry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "block")));

    /**
     * Built-in entity type registry ({@code minecraft:entity_type}).
     */
    public static final Registry<String> ENTITY_TYPE = new Registry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "entity_type")));

    /**
     * Built-in sound event registry ({@code minecraft:sound_event}).
     */
    public static final Registry<String> SOUND_EVENT = new Registry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "sound_event")));

    /**
     * Built-in creative mode tab registry ({@code minecraft:creative_mode_tab}).
     */
    public static final Registry<CreativeModeTab> CREATIVE_MODE_TAB = new Registry<>(
            ResourceKey.createRegistryKey(new ResourceLocation("minecraft", "creative_mode_tab")));

    static {
        Registry.register(ITEM, new ResourceLocation("minecraft", "air"), "minecraft:air");
        Registry.register(ITEM, new ResourceLocation("minecraft", "stone"), "minecraft:stone");
        Registry.register(ITEM, new ResourceLocation("minecraft", "diamond"), "minecraft:diamond");
        Registry.register(BLOCK, new ResourceLocation("minecraft", "air"), "minecraft:air");
        Registry.register(BLOCK, new ResourceLocation("minecraft", "stone"), "minecraft:stone");
        Registry.register(
                ENTITY_TYPE, new ResourceLocation("minecraft", "player"), "minecraft:player");
    }

    private BuiltInRegistries() {
    }
}
