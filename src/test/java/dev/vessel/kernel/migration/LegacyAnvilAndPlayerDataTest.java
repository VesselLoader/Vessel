package dev.vessel.kernel.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite verifying legacy Anvil 2D biome array expansion and playerdata tag preservation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LegacyAnvilAndPlayerDataTest {

    @Test
    @DisplayName("LegacyAnvilBiomeArrayExpander correctly expands 256-byte legacy biomes to 3D paletted sections")
    public void testAnvilBiomeExpansionSuccess() {
        LegacyAnvilBiomeArrayExpander expander = new LegacyAnvilBiomeArrayExpander();
        byte[] legacyBiomes = new byte[LegacyAnvilBiomeArrayExpander.LEGACY_2D_BIOME_COUNT];

        // Fill lower half with plains (1) and upper half with desert (2)
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                legacyBiomes[z * 16 + x] = (byte) (z < 8 ? 1 : 2);
            }
        }

        List<PalettedBiomeContainer> sections = expander.expandLegacyByteArray(legacyBiomes);
        assertThat(sections).hasSize(LegacyAnvilBiomeArrayExpander.DEFAULT_SECTION_COUNT);

        for (PalettedBiomeContainer section : sections) {
            assertThat(section.palette()).contains("minecraft:plains", "minecraft:desert");

            // Verify representative voxel queries
            // z = 0, x = 0 -> voxelZ = 0, voxelX = 0 -> plains
            assertThat(section.getBiome(0, 0, 0)).isEqualTo("minecraft:plains");
            assertThat(section.getBiome(0, 3, 0)).isEqualTo("minecraft:plains");

            // z = 12, x = 0 -> voxelZ = 3, voxelX = 0 -> desert
            assertThat(section.getBiome(0, 0, 3)).isEqualTo("minecraft:desert");
            assertThat(section.getBiome(0, 2, 3)).isEqualTo("minecraft:desert");
        }
    }

    @Test
    @DisplayName("LegacyAnvilBiomeArrayExpander validates input array size and non-null constraints")
    public void testAnvilBiomeExpansionValidation() {
        LegacyAnvilBiomeArrayExpander expander = new LegacyAnvilBiomeArrayExpander();

        assertThatThrownBy(() -> expander.expandLegacyByteArray(null))
                .isInstanceOf(BiomeArrayExpansionException.class)
                .hasMessageContaining("cannot be null");

        byte[] invalidLength = new byte[128];
        assertThatThrownBy(() -> expander.expandLegacyByteArray(invalidLength))
                .isInstanceOf(BiomeArrayExpansionException.class)
                .hasMessageContaining("Invalid legacy biome array length");
    }

    @Test
    @DisplayName("PlayerDataTagPreserver sanitizes UUID and preserves modded inventory NBT and XP")
    public void testPlayerDataPreservationAndMerge() {
        PlayerDataTagPreserver preserver = new PlayerDataTagPreserver();

        String rawUpperUuid = "12345678-ABCD-EF01-2345-6789ABCDEF01";
        Map<String, String> standard = Map.of("Health", "20.0f", "Air", "300");
        Map<String, byte[]> modTags = new HashMap<>();
        modTags.put("ForgeCaps", new byte[]{0x01, 0x02, 0x03});
        modTags.put("Curios", new byte[]{0x0A, 0x0B});
        modTags.put("VanillaTagIgnoredByModShield", new byte[]{0x55});

        PlayerInventorySnapshot snapshot1 = preserver.createSnapshot(
                rawUpperUuid,
                30,
                0.5f,
                1500,
                42,
                standard,
                modTags
        );

        // Verify UUID was normalized to lowercase
        assertThat(snapshot1.playerUuid()).isEqualTo("12345678-abcd-ef01-2345-6789abcdef01");
        // Verify only protected tags were retained in preservedModTags
        assertThat(snapshot1.preservedModTags()).containsKey("ForgeCaps");
        assertThat(snapshot1.preservedModTags()).containsKey("Curios");
        assertThat(snapshot1.preservedModTags()).doesNotContainKey("VanillaTagIgnoredByModShield");

        // Simulate subsequent session with lower level (e.g. death or glitch), merge must preserve highest XP
        PlayerInventorySnapshot snapshot2 = preserver.createSnapshot(
                snapshot1.playerUuid(),
                15,
                0.2f,
                800,
                42,
                Map.of("Health", "10.0f"),
                Map.of("cardinal_components", new byte[]{0x77})
        );

        PlayerInventorySnapshot merged = preserver.mergeSnapshots(snapshot1, snapshot2);

        // Experience monotonic preservation
        assertThat(merged.xpLevel()).isEqualTo(30);
        assertThat(merged.xpTotal()).isEqualTo(1500);

        // Mod capabilities union
        assertThat(merged.preservedModTags()).containsKey("ForgeCaps");
        assertThat(merged.preservedModTags()).containsKey("Curios");
        assertThat(merged.preservedModTags()).containsKey("cardinal_components");
    }

    @Test
    @DisplayName("PlayerDataTagPreserver rejects merging snapshots of disparate player UUIDs")
    public void testDisparateUuidMergeRejection() {
        PlayerDataTagPreserver preserver = new PlayerDataTagPreserver();
        PlayerInventorySnapshot p1 = preserver.createSnapshot(
                UUID.randomUUID().toString(), 10, 0f, 100, 1, Map.of(), Map.of()
        );
        PlayerInventorySnapshot p2 = preserver.createSnapshot(
                UUID.randomUUID().toString(), 20, 0f, 200, 2, Map.of(), Map.of()
        );

        assertThatThrownBy(() -> preserver.mergeSnapshots(p1, p2))
                .isInstanceOf(PlayerDataPreservationException.class)
                .hasMessageContaining("Cannot merge snapshots of different players");
    }
}
