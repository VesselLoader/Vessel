package dev.vessel.kernel.test;

import dev.vessel.kernel.resourcepack.VesselCoreShaderFilterResult;
import dev.vessel.kernel.resourcepack.VesselCoreShaderSafetyFilter;
import dev.vessel.kernel.resourcepack.VesselDeterministicZipEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link VesselCoreShaderSafetyFilter}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VesselCoreShaderSafetyFilterTest {

    @Test
    @DisplayName("isUnsafeCoreShader accurately detects vanilla core shaders across case and path separators")
    void testCoreShaderDetection() {
        // Vanilla core shaders (must be filtered out)
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/core/rendertype_solid.json")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/core/rendertype_solid.vsh")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/core/rendertype_solid.fsh")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/core/position_color.json")).isTrue();

        // Windows path separator normalization
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets\\minecraft\\shaders\\core\\rendertype_cutout.vsh")).isTrue();

        // Leading slash normalization
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("/assets/minecraft/shaders/core/rendertype_solid.json")).isTrue();

        // Uppercase / mixed case normalization
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("Assets/Minecraft/Shaders/Core/rendertype_solid.json")).isTrue();

        // Safe shaders: post-processing, blit, include, or custom mod namespaces
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/program/blit.json")).isFalse();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/post/transparency.json")).isFalse();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/include/light.glsl")).isFalse();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/techmod/shaders/core/magic_beam.json")).isFalse();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/textures/block/stone.png")).isFalse();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("pack.mcmeta")).isFalse();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader(null)).isFalse();
    }

    @Test
    @DisplayName("filter strips vanilla core shaders while preserving all safe pack assets")
    void testFilterPreservesSafeAssets() {
        VesselCoreShaderSafetyFilter filter = new VesselCoreShaderSafetyFilter();

        Map<String, byte[]> rawEntries = new LinkedHashMap<>();
        rawEntries.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15}}".getBytes(StandardCharsets.UTF_8));
        rawEntries.put("assets/minecraft/textures/block/dirt.png", new byte[]{1, 2, 3});
        rawEntries.put("assets/minecraft/shaders/core/rendertype_solid.json", new byte[]{4, 5});
        rawEntries.put("assets/minecraft/shaders/core/rendertype_solid.vsh", new byte[]{6, 7});
        rawEntries.put("assets/minecraft/shaders/program/blur.json", new byte[]{8, 9});
        rawEntries.put("assets/techmod/textures/item/wrench.png", new byte[]{10, 11});

        VesselCoreShaderFilterResult result = filter.filter(rawEntries);

        assertThat(result.hasFilteredShaders()).isTrue();
        assertThat(result.filteredCount()).isEqualTo(2);
        assertThat(result.removedShaderPaths()).containsExactly(
                "assets/minecraft/shaders/core/rendertype_solid.json",
                "assets/minecraft/shaders/core/rendertype_solid.vsh"
        );

        Map<String, byte[]> safe = result.safeEntries();
        assertThat(safe).hasSize(4);
        assertThat(safe).containsKey("pack.mcmeta");
        assertThat(safe).containsKey("assets/minecraft/textures/block/dirt.png");
        assertThat(safe).containsKey("assets/minecraft/shaders/program/blur.json");
        assertThat(safe).containsKey("assets/techmod/textures/item/wrench.png");
        assertThat(safe).doesNotContainKey("assets/minecraft/shaders/core/rendertype_solid.json");
        assertThat(safe).doesNotContainKey("assets/minecraft/shaders/core/rendertype_solid.vsh");
    }

    @Test
    @DisplayName("filterEntries correctly purges unsafe entries from entry list")
    void testFilterEntryCollection() {
        VesselCoreShaderSafetyFilter filter = new VesselCoreShaderSafetyFilter();

        List<VesselDeterministicZipEntry> entries = Arrays.asList(
                new VesselDeterministicZipEntry("assets/minecraft/textures/block/stone.png", new byte[]{1}),
                new VesselDeterministicZipEntry("assets/minecraft/shaders/core/rendertype_translucent.json", new byte[]{2}),
                new VesselDeterministicZipEntry("assets/minecraft/models/item/diamond.json", new byte[]{3})
        );

        List<VesselDeterministicZipEntry> safeEntries = filter.filterEntries(entries);

        assertThat(safeEntries).hasSize(2);
        assertThat(safeEntries.stream().map(VesselDeterministicZipEntry::path).toList())
                .containsExactly(
                        "assets/minecraft/textures/block/stone.png",
                        "assets/minecraft/models/item/diamond.json"
                );
    }

    @Test
    @DisplayName("isUnsafeCoreShader detects directory and evasion attempts like double slashes or relative dots")
    void testEvasionAndDirectoryDetection() {
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft//shaders/core/rendertype_solid.json")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("./assets/minecraft/shaders/core/rendertype_solid.json")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/./shaders/core/rendertype_solid.json")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/core")).isTrue();
        assertThat(VesselCoreShaderSafetyFilter.isUnsafeCoreShader("assets/minecraft/shaders/core/")).isTrue();
    }
}
