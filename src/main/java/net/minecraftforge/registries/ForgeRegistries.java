package net.minecraftforge.registries;

import net.minecraft.resources.ResourceLocation;

/**
 * Static holders for standard Forge registries ({@code net.minecraftforge.registries.ForgeRegistries}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ForgeRegistries {

    public static final IForgeRegistry<Object> BLOCKS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:block"));
    public static final IForgeRegistry<Object> ITEMS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:item"));
    public static final IForgeRegistry<Object> FLUIDS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:fluid"));
    public static final IForgeRegistry<Object> MOB_EFFECTS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:mob_effect"));
    public static final IForgeRegistry<Object> POTIONS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:potion"));
    public static final IForgeRegistry<Object> BIOMES = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:worldgen/biome"));
    public static final IForgeRegistry<Object> SOUND_EVENTS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:sound_event"));
    public static final IForgeRegistry<Object> ENTITY_TYPES = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:entity_type"));
    public static final IForgeRegistry<Object> BLOCK_ENTITY_TYPES = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:block_entity_type"));
    public static final IForgeRegistry<Object> PARTICLE_TYPES = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:particle_type"));
    public static final IForgeRegistry<Object> MENU_TYPES = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:menu"));
    public static final IForgeRegistry<Object> RECIPE_TYPES = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:recipe_type"));
    public static final IForgeRegistry<Object> RECIPE_SERIALIZERS = new SimpleForgeRegistry<>(new ResourceLocation("minecraft:recipe_serializer"));

    private ForgeRegistries() {
    }
}
