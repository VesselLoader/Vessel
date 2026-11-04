package dev.vessel.kernel.test;

import dev.vessel.kernel.resourcepack.PackTargetPlatform;
import dev.vessel.kernel.resourcepack.VesselCoreShaderFilterResult;
import dev.vessel.kernel.resourcepack.VesselCoreShaderSafetyFilter;
import dev.vessel.kernel.resourcepack.VesselDeterministicZipPacker;
import dev.vessel.kernel.resourcepack.VesselSanitizedTextureResult;
import dev.vessel.kernel.resourcepack.VesselSoundStreamingMetadataGenerator;
import dev.vessel.kernel.resourcepack.VesselTextureAtlasSanitizer;
import dev.vessel.kernel.resourcepack.VirtualResourcePack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End integration test suite for the Deterministic ZIP Packaging & Asset Sanitization Pipeline.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VesselDeterministicAssetPipelineIntegrationTest {

    private byte[] createTestPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    private byte[] createDummyOgg(int sizeBytes) {
        byte[] data = new byte[sizeBytes];
        data[0] = 'O'; data[1] = 'g'; data[2] = 'g'; data[3] = 'S';
        return data;
    }

    @Test
    @DisplayName("End-to-End Asset Sanitization and Deterministic Packaging Pipeline")
    void testFullPipelineIntegration() throws IOException {
        // 1. Prepare raw mod asset files
        Map<String, byte[]> rawAssets = new LinkedHashMap<>();
        rawAssets.put("pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"Test\"}}".getBytes(StandardCharsets.UTF_8));
        rawAssets.put("assets/minecraft/shaders/core/rendertype_solid.json", "{}".getBytes(StandardCharsets.UTF_8)); // Should be stripped
        rawAssets.put("assets/minecraft/shaders/core/rendertype_solid.vsh", "// shader".getBytes(StandardCharsets.UTF_8)); // Should be stripped
        rawAssets.put("assets/techmod/textures/item/wrench.png", createTestPng(16, 16));
        rawAssets.put("assets/techmod/sounds/ambient/machinery.ogg", createDummyOgg(600 * 1024)); // > 500KB -> must stream
        rawAssets.put("assets/techmod/sounds/item/click.ogg", createDummyOgg(50 * 1024)); // 50KB -> no stream
        rawAssets.put("assets/techmod/sounds.json", """
        {
          "techmod:machinery_run": {
            "sounds": ["techmod:ambient/machinery"]
          },
          "techmod:click": {
            "sounds": ["techmod:item/click"]
          }
        }
        """.getBytes(StandardCharsets.UTF_8));

        // 2. Step 1: Core Shader Safety Filter
        VesselCoreShaderSafetyFilter shaderFilter = new VesselCoreShaderSafetyFilter();
        VesselCoreShaderFilterResult filterResult = shaderFilter.filter(rawAssets);
        assertThat(filterResult.hasFilteredShaders()).isTrue();
        assertThat(filterResult.filteredCount()).isEqualTo(2);

        Map<String, byte[]> sanitizedAssets = new LinkedHashMap<>(filterResult.safeEntries());
        assertThat(sanitizedAssets).doesNotContainKey("assets/minecraft/shaders/core/rendertype_solid.json");
        assertThat(sanitizedAssets).doesNotContainKey("assets/minecraft/shaders/core/rendertype_solid.vsh");

        // 3. Step 2: Texture Atlas Sanitizer
        VesselTextureAtlasSanitizer textureSanitizer = new VesselTextureAtlasSanitizer();
        byte[] rawWrench = sanitizedAssets.get("assets/techmod/textures/item/wrench.png");
        VesselSanitizedTextureResult sanitizedTexture = textureSanitizer.sanitize(rawWrench);
        assertThat(sanitizedTexture.originallyPowerOfTwo()).isTrue();
        assertThat(sanitizedTexture.originalWidth()).isEqualTo(16);
        assertThat(sanitizedTexture.paddedWidth()).isEqualTo(18); // 16 + 2*1px padding
        sanitizedAssets.put("assets/techmod/textures/item/wrench.png", sanitizedTexture.sanitizedPngData());

        // 4. Step 3: Sound Streaming Metadata Generator
        VesselSoundStreamingMetadataGenerator soundGen = new VesselSoundStreamingMetadataGenerator();
        String initialSoundsJson = new String(sanitizedAssets.get("assets/techmod/sounds.json"), StandardCharsets.UTF_8);
        String updatedSoundsJson = soundGen.updateSoundsJson(initialSoundsJson, sanitizedAssets);
        assertThat(updatedSoundsJson).contains("\"stream\": true");
        sanitizedAssets.put("assets/techmod/sounds.json", updatedSoundsJson.getBytes(StandardCharsets.UTF_8));

        // 5. Step 4: Deterministic Packaging
        VesselDeterministicZipPacker packer = new VesselDeterministicZipPacker();
        VirtualResourcePack pack1 = packer.packVirtual("techmod-pack", "TechMod Assets", PackTargetPlatform.JAVA_EDITION, sanitizedAssets);

        // Run packaging again from shuffled map to ensure 100% deterministic SHA-1
        Map<String, byte[]> shuffled = new LinkedHashMap<>();
        shuffled.put("assets/techmod/sounds.json", sanitizedAssets.get("assets/techmod/sounds.json"));
        shuffled.put("pack.mcmeta", sanitizedAssets.get("pack.mcmeta"));
        shuffled.put("assets/techmod/textures/item/wrench.png", sanitizedAssets.get("assets/techmod/textures/item/wrench.png"));
        shuffled.put("assets/techmod/sounds/ambient/machinery.ogg", sanitizedAssets.get("assets/techmod/sounds/ambient/machinery.ogg"));
        shuffled.put("assets/techmod/sounds/item/click.ogg", sanitizedAssets.get("assets/techmod/sounds/item/click.ogg"));

        VirtualResourcePack pack2 = packer.packVirtual("techmod-pack", "TechMod Assets", PackTargetPlatform.JAVA_EDITION, shuffled);

        assertThat(pack1.sha1Hex()).isEqualTo(pack2.sha1Hex());
        assertThat(pack1.archiveData()).isEqualTo(pack2.archiveData());
    }
}
