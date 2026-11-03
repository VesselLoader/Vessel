package dev.vessel.bootstrap.wave8;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 8 verifying Fabric {@link ItemGroupEvents},
 * {@link FabricItemGroupEntries}, {@link BiomeModifications}, {@link BiomeSelectionContext}, and
 * {@link ModificationPhase}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveEightFabricItemGroupAndBiomeTest {

    private static void populateQuantumEntries(FabricItemGroupEntries entries) {
        entries.add("vessel:quantum_ingot");
        entries.addAfter("vessel:quantum_ingot", "vessel:quantum_nugget");
    }

    private static boolean isOverworldBiome(BiomeSelectionContext context) {
        return context.hasTag("minecraft:is_overworld");
    }

    @Test
    void testFabricItemGroupEventsAndBiomeModifications() {
        ItemGroupEvents.clearAll();
        BiomeModifications.clearAll();

        ItemGroupEvents.modifyEntriesEvent("minecraft:ingredients")
                .register(WaveEightFabricItemGroupAndBiomeTest::populateQuantumEntries);

        FabricItemGroupEntries entries = new FabricItemGroupEntries();
        ItemGroupEvents.modifyEntriesEvent("minecraft:ingredients")
                .invoker()
                .modifyEntries(entries);

        assertEquals(
                List.of("vessel:quantum_ingot", "vessel:quantum_nugget"),
                entries.getDisplayStacks());

        BiomeModifications.addFeature(
                WaveEightFabricItemGroupAndBiomeTest::isOverworldBiome,
                "UNDERGROUND_ORES",
                "vessel:ore_quantum");

        BiomeSelectionContext plainsContext = new BiomeSelectionContext(
                "minecraft:plains",
                Set.of("minecraft:is_overworld"));
        assertEquals("minecraft:plains", plainsContext.getBiomeKey());
        assertTrue(plainsContext.hasTag("minecraft:is_overworld"));
        assertFalse(plainsContext.hasTag("minecraft:is_nether"));

        List<String> evaluated = BiomeModifications.evaluateForBiome(plainsContext);
        assertEquals(List.of("UNDERGROUND_ORES:vessel:ore_quantum"), evaluated);
        assertEquals(
                List.of("UNDERGROUND_ORES:vessel:ore_quantum"),
                BiomeModifications.getAppliedFeatures());
        assertEquals(ModificationPhase.ADDITIONS, ModificationPhase.valueOf("ADDITIONS"));

        ItemGroupEvents.clearAll();
        BiomeModifications.clearAll();
    }
}
