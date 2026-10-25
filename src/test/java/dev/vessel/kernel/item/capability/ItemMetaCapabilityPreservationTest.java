package dev.vessel.kernel.item.capability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ItemMetaCapabilityPreservationBridge} and {@link CraftItemStackTagPreserver}
 * under ERR-55, CBT-0001..0400, and CBT-0401..0800.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
class ItemMetaCapabilityPreservationTest {

    @Test
    void testPreserveForgeCapabilitiesAcrossBukkitMutation() {
        ItemMetaCapabilityPreservationBridge bridge = new ItemMetaCapabilityPreservationBridge();
        String itemId = "mekanism:atomic_disassembler";

        byte[] energyPayload = "ENERGY_DATA_500000_FE".getBytes(StandardCharsets.UTF_8);
        byte[] curiosPayload = "CURIOS_INVENTORY_SLOT_0".getBytes(StandardCharsets.UTF_8);

        Map<String, CapabilityTagAttachment> caps = new LinkedHashMap<>();
        caps.put("forge:energy", new CapabilityTagAttachment("forge:energy", energyPayload, 1));
        caps.put("curios:slot", new CapabilityTagAttachment("curios:slot", curiosPayload, 1));

        PreservedCapabilitySnapshot snapshot = bridge.capture(itemId, caps);
        assertTrue(snapshot.hasAttachments());
        assertEquals(2, snapshot.attachments().size());

        // Bukkit plugin setItemMeta() produces a map that only contains display changes
        Map<String, Object> bukkitMetaOutput = new LinkedHashMap<>();
        bukkitMetaOutput.put("display", Map.of("Name", "Custom Disassembler", "Lore", Collections.singletonList("Legendary Tool")));

        // Bridge restores capabilities
        Map<String, Object> merged = bridge.restoreCapabilities(bukkitMetaOutput, snapshot);
        assertNotNull(merged);
        assertTrue(merged.containsKey("display"));
        assertTrue(merged.containsKey(ItemMetaCapabilityPreservationBridge.FORGE_CAPS_TAG_KEY));

        @SuppressWarnings("unchecked")
        Map<String, Object> restoredCaps = (Map<String, Object>) merged.get(ItemMetaCapabilityPreservationBridge.FORGE_CAPS_TAG_KEY);
        assertArrayEquals(energyPayload, (byte[]) restoredCaps.get("forge:energy"));
        assertArrayEquals(curiosPayload, (byte[]) restoredCaps.get("curios:slot"));
    }

    @Test
    void testCraftItemStackTagPreserverDeepModCompound() {
        CraftItemStackTagPreserver preserver = new CraftItemStackTagPreserver();

        // Mod machine item with deep custom NBT
        Map<String, Object> originalTags = new LinkedHashMap<>();
        originalTags.put("mekData", Map.of("energyStored", 2500000L, "gasStored", "oxygen:1000"));
        originalTags.put("mana", 50000);
        originalTags.put("ae2_grid", "STORAGE_CELL_64K_DATA");
        originalTags.put("Damage", 0);

        // Bukkit plugin modifies display lore and enchantments
        Map<String, Object> bukkitMutatedTags = new LinkedHashMap<>();
        bukkitMutatedTags.put("display", Map.of("Lore", Collections.singletonList("Godly Efficiency")));
        bukkitMutatedTags.put("Enchantments", Collections.singletonList(Map.of("id", "minecraft:unbreaking", "lvl", 3)));
        bukkitMutatedTags.put("Damage", 10);

        Map<String, Object> preserved = preserver.preserveModTags(originalTags, bukkitMutatedTags);

        // Verify Bukkit updates are present
        assertTrue(preserved.containsKey("display"));
        assertTrue(preserved.containsKey("Enchantments"));
        assertEquals(10, preserved.get("Damage"));

        // Verify all deep mod machine tags are 100% preserved
        assertTrue(preserved.containsKey("mekData"));
        assertEquals(Map.of("energyStored", 2500000L, "gasStored", "oxygen:1000"), preserved.get("mekData"));
        assertEquals(50000, preserved.get("mana"));
        assertEquals("STORAGE_CELL_64K_DATA", preserved.get("ae2_grid"));
    }

    @Test
    void testIsBukkitManagedKey() {
        CraftItemStackTagPreserver preserver = new CraftItemStackTagPreserver();
        assertTrue(preserver.isBukkitManagedKey("display"));
        assertTrue(preserver.isBukkitManagedKey("Enchantments"));
        assertTrue(preserver.isBukkitManagedKey("CustomModelData"));
        assertFalse(preserver.isBukkitManagedKey("mekData"));
        assertFalse(preserver.isBukkitManagedKey("botania_mana"));
        assertFalse(preserver.isBukkitManagedKey("AE2_DATA"));
    }
}
