package dev.vessel.bootstrap.wave10;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.api.registry.VesselBuiltInRegistryEntrySnapshot;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 10 verifying Minecraft {@link ResourceKey}, {@link Holder},
 * {@link Registry}, {@link BuiltInRegistries}, and VUSA
 * {@link VesselBuiltInRegistryEntrySnapshot} interoperability.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveTenMinecraftBuiltInRegistriesAndHolderTest {

    @Test
    void testResourceKeyHolderBuiltInRegistriesAndVusaSnapshot() {
        assertTrue(
                BuiltInRegistries.ITEM.containsKey(
                        ResourceLocation.withDefaultNamespace("diamond")));
        assertTrue(
                BuiltInRegistries.BLOCK.containsKey(
                        ResourceLocation.withDefaultNamespace("stone")));
        assertTrue(
                BuiltInRegistries.ENTITY_TYPE.containsKey(
                        ResourceLocation.withDefaultNamespace("player")));

        ResourceLocation quantumBladeId = ResourceLocation.parse("vessel:quantum_blade");
        String registeredItem = Registry.register(
                BuiltInRegistries.ITEM,
                quantumBladeId,
                "vessel:quantum_blade_item");
        assertEquals("vessel:quantum_blade_item", registeredItem);
        assertEquals("vessel:quantum_blade_item", BuiltInRegistries.ITEM.get(quantumBladeId));
        assertEquals(
                quantumBladeId,
                BuiltInRegistries.ITEM.getKey("vessel:quantum_blade_item"));

        Holder<String> bladeHolder = BuiltInRegistries.ITEM.getHolder(quantumBladeId).orElse(null);
        assertNotNull(bladeHolder);
        assertTrue(bladeHolder.isBound());
        assertTrue(bladeHolder.is(quantumBladeId));
        assertEquals("vessel:quantum_blade_item", bladeHolder.value());

        TagKey<String> swordsTag = TagKey.create(
                ResourceLocation.withDefaultNamespace("item"),
                ResourceLocation.withDefaultNamespace("swords"));
        bladeHolder.bindTag(swordsTag);
        assertTrue(bladeHolder.is(swordsTag));
        assertEquals(1, bladeHolder.tags().size());

        ResourceKey<?> itemRegistryKey = BuiltInRegistries.ITEM.key();
        assertNotNull(itemRegistryKey);
        assertEquals(ResourceLocation.withDefaultNamespace("item"), itemRegistryKey.location());

        VesselBuiltInRegistryEntrySnapshot snapshot = new VesselBuiltInRegistryEntrySnapshot(
                itemRegistryKey.location().toString(),
                bladeHolder.unwrapKey().toString(),
                1024,
                Set.of(swordsTag.location().toString()));
        assertEquals("minecraft:item", snapshot.registryKey());
        assertEquals("vessel:quantum_blade", snapshot.entryKey());
        assertEquals(1024, snapshot.rawNumericalId());
        assertTrue(snapshot.hasTag("minecraft:swords"));
    }
}
