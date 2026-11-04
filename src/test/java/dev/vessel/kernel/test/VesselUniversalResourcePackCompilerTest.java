package dev.vessel.kernel.test;

import dev.vessel.bootstrap.target.VesselUniversalResourcePackCompiler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end unit verification suite for {@link VesselUniversalResourcePackCompiler}.
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public class VesselUniversalResourcePackCompilerTest {

    private static byte[] createPng(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, w, h);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    private static byte[] createLargeOgg() {
        byte[] ogg = new byte[600 * 1024]; // 600 KB > 500 KB threshold
        ogg[0] = 'O'; ogg[1] = 'g'; ogg[2] = 'g'; ogg[3] = 'S';
        return ogg;
    }

    private static void createMockModJar(Path jarPath) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarPath))) {
            // 1. Uppercase texture path (must be lowercased by pipeline)
            jos.putNextEntry(new JarEntry("assets/SampleMod/textures/item/SuperWrench.png"));
            jos.write(createPng(16, 16));
            jos.closeEntry();

            // 2. Block texture
            jos.putNextEntry(new JarEntry("assets/samplemod/textures/block/ReactorBlock.png"));
            jos.write(createPng(32, 32));
            jos.closeEntry();

            // 3. Dangerous Core Shader (must be stripped)
            jos.putNextEntry(new JarEntry("assets/minecraft/shaders/core/rendertype_solid.json"));
            jos.write("{\"blend\":{}}".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // 4. Large audio file (> 500KB)
            jos.putNextEntry(new JarEntry("assets/samplemod/sounds/ambient/hum.ogg"));
            jos.write(createLargeOgg());
            jos.closeEntry();

            // 5. sounds.json referencing the audio file
            jos.putNextEntry(new JarEntry("assets/samplemod/sounds.json"));
            jos.write("""
            {
              "samplemod:hum": {
                "sounds": ["samplemod:ambient/hum"]
              }
            }
            """.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // 6. Language file
            jos.putNextEntry(new JarEntry("assets/samplemod/lang/en_us.json"));
            jos.write("""
            {
              "item.samplemod.superwrench": "Super Wrench",
              "block.samplemod.reactorblock": "Fusion Reactor"
            }
            """.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }

    @Test
    @DisplayName("VesselUniversalResourcePackCompiler compiles deterministic sanitized pack with Bedrock metadata")
    void testUniversalCompilerPipeline(@TempDir Path serverRoot) throws IOException {
        Path modsDir = serverRoot.resolve("mods");
        Files.createDirectories(modsDir);
        Path modJar = modsDir.resolve("sample-mod.jar");
        createMockModJar(modJar);

        Path packZip1 = serverRoot.resolve("cache/ecosystem/vessel-pack1.zip");
        Path packZip2 = serverRoot.resolve("cache/ecosystem/vessel-pack2.zip");

        VesselUniversalResourcePackCompiler compiler = new VesselUniversalResourcePackCompiler();

        // Pass 1: Compile targeting 1.21.4
        String sha1_1 = compiler.compileResourcePack(serverRoot, packZip1, "1.21.4");
        assertThat(sha1_1).hasSize(40);
        assertThat(Files.isRegularFile(packZip1)).isTrue();

        // Pass 2: Re-compile targeting 1.21.4 into another file
        String sha1_2 = compiler.compileResourcePack(serverRoot, packZip2, "1.21.4");
        assertThat(sha1_2).isEqualTo(sha1_1);
        assertThat(Files.readAllBytes(packZip1)).isEqualTo(Files.readAllBytes(packZip2));

        // Inspect ZIP structure
        List<String> entryNames = new ArrayList<>();
        try (ZipFile zip = new ZipFile(packZip1.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            String prevName = null;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                entryNames.add(name);

                // Verify strictly alphabetical order
                if (prevName != null) {
                    assertThat(prevName.compareTo(name))
                            .as("Entries must be sorted alphabetically: '%s' vs '%s'", prevName, name)
                            .isLessThanOrEqualTo(0);
                }
                prevName = name;

                // Verify deterministic timestamp (DOS epoch 1980-01-01 00:00:00 UTC)
                assertThat(entry.getLastModifiedTime().toInstant().getEpochSecond()).isEqualTo(315532800L);
            }

            // Verify shader safety: core shader stripped
            assertThat(entryNames).noneMatch(n -> n.contains("shaders/core"));

            // Verify lowercased paths
            assertThat(entryNames).contains("assets/samplemod/textures/item/superwrench.png");
            assertThat(entryNames).contains("assets/samplemod/textures/block/reactorblock.png");

            // Verify pack.mcmeta exists
            assertThat(entryNames).contains("pack.mcmeta");
            ZipEntry mcmetaEntry = zip.getEntry("pack.mcmeta");
            String mcmeta = new String(zip.getInputStream(mcmetaEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(mcmeta).contains("\"pack_format\": 46"); // 1.21.4 format

            // Verify audio streaming flag in sounds.json
            assertThat(entryNames).contains("assets/samplemod/sounds.json");
            ZipEntry soundsEntry = zip.getEntry("assets/samplemod/sounds.json");
            String soundsJson = new String(zip.getInputStream(soundsEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(soundsJson).contains("\"stream\": true");

            // Verify Bedrock metadata synthesized
            assertThat(entryNames).contains("textures/item_texture.json");
            assertThat(entryNames).contains("textures/terrain_texture.json");
            assertThat(entryNames).contains("texts/en_US.lang");
            assertThat(entryNames).contains("manifest.json");

            // Verify Bedrock manifest stripped of dependencies
            ZipEntry manifestEntry = zip.getEntry("manifest.json");
            String manifestJson = new String(zip.getInputStream(manifestEntry).readAllBytes(), StandardCharsets.UTF_8);
            assertThat(manifestJson).doesNotContain("\"dependencies\"");
            assertThat(manifestJson).contains("\"type\": \"resources\"");
        }
    }
}
