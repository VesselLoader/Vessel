package dev.vessel.bootstrap.target;

import dev.vessel.kernel.resourcepack.VesselResourcePackAssetPipeline;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Compiles the Universal Client Presentation Resource Pack ({@code vessel-pack.zip}) by aggregating
 * client assets (textures, block/item/entity models, blockstates, sounds, lang) across all discovered
 * mod and plugin JARs in {@code mods/} and {@code plugins/}.
 * <p>
 * Routes all aggregated assets through {@link VesselResourcePackAssetPipeline} for:
 * <ul>
 *   <li>POSIX lowercase normalization to prevent case-sensitivity crashes on Linux and Android</li>
 *   <li>Shader safety filtering to prevent OptiFine/Iris/Sodium OpenGL crashes</li>
 *   <li>Atlas seam protection and Power-of-Two verification for RenderDragon mobile GPUs</li>
 *   <li>Audio streaming flags in {@code sounds.json} for large OGG tracks (>500KB or >5s)</li>
 *   <li>Bedrock metadata synthesis ({@code item_texture.json}, {@code terrain_texture.json}, {@code texts/*.lang}, clean manifest)</li>
 *   <li>Deterministic ZIP output with fixed epoch timestamps (1980-01-01 00:00:00 UTC) and alphabetical sorting</li>
 * </ul>
 * Computes the 40-character SHA-1 checksum on-the-fly in a single streaming pass using {@link DigestOutputStream},
 * completely eliminating {@code Files.readAllBytes} heap allocations.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.3
 * @author VESSEL Engine Team
 */
public final class VesselUniversalResourcePackCompiler {

    private static final byte[] MINIMAL_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    private final VesselPackFormatResolver packFormatResolver = new VesselPackFormatResolver();
    private final VesselResourcePackAssetPipeline assetPipeline = new VesselResourcePackAssetPipeline();

    public VesselUniversalResourcePackCompiler() {
    }

    /**
     * Compiles all assets found across {@code mods/} and {@code plugins/} into {@code packZip} and
     * returns the deterministic SHA-1 hex hash of the resulting resource pack archive, targeting 1.21.4 / 26.3.
     *
     * @param serverRoot the root server directory
     * @param packZip the target path for {@code vessel-pack.zip}
     * @return 40-character lowercase hexadecimal SHA-1 string
     * @throws IOException on I/O or archive writing errors
     */
    public String compileResourcePack(Path serverRoot, Path packZip) throws IOException {
        return compileResourcePack(serverRoot, packZip, "1.21.4");
    }

    /**
     * Compiles all assets found across {@code mods/} and {@code plugins/} into {@code packZip} with
     * the canonical {@code pack_format} computed for the running Minecraft version.
     *
     * @param serverRoot the root server directory
     * @param packZip the target path for {@code vessel-pack.zip}
     * @param mcVersion active Minecraft version (e.g. {@code "1.12.2"}, {@code "1.20.1"}, {@code "26.3"})
     * @return 40-character lowercase hexadecimal SHA-1 string
     * @throws IOException on I/O or archive writing errors
     */
    public String compileResourcePack(Path serverRoot, Path packZip, String mcVersion) throws IOException {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(packZip, "packZip must not be null");

        Path parentDir = packZip.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        int packFormat = packFormatResolver.resolvePackFormat(mcVersion);
        Map<String, byte[]> rawAssets = new LinkedHashMap<>();

        // 1. pack.mcmeta with dynamic pack_format for the detected Minecraft version
        String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": "
                + packFormat
                + ",\n    \"description\": \"VESSEL Sovereign Universal Mod Presentation Pack\"\n  }\n}\n";
        rawAssets.put("pack.mcmeta", mcmeta.getBytes(StandardCharsets.UTF_8));

        // 2. assets/vessel/textures/item/vessel_core.png
        rawAssets.put("assets/vessel/textures/item/vessel_core.png", MINIMAL_PNG);

        // 3. Scan serverRoot/mods and serverRoot/plugins for JAR assets/
        aggregateAssetsFromDirectory(serverRoot.resolve("mods"), rawAssets);
        aggregateAssetsFromDirectory(serverRoot.resolve("plugins"), rawAssets);

        // 4. Stream and pack deterministically while calculating SHA-1 on-the-fly
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            try (DigestOutputStream dos = new DigestOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(packZip)), sha1Digest)) {
                assetPipeline.sanitizeAndPackToStream(rawAssets, Collections.emptyList(), mcVersion, dos);
                dos.flush();
            }
            return HexFormat.of().formatHex(sha1Digest.digest());
        } catch (Exception e) {
            throw new IOException("Failed to compile deterministic resource pack: " + e.getMessage(), e);
        }
    }

    private static void aggregateAssetsFromDirectory(Path dir, Map<String, byte[]> rawAssets) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        File[] files = dir.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                extractJarAssetsIntoMap(file, rawAssets);
            }
        }
    }

    private static void extractJarAssetsIntoMap(File jarFile, Map<String, byte[]> rawAssets) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith("assets/")) {
                    continue;
                }
                if (name.endsWith(".class") || name.endsWith(".DS_Store")) {
                    continue;
                }
                if (rawAssets.containsKey(name.toLowerCase())) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] data = in.readAllBytes();
                    rawAssets.put(name, data);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
