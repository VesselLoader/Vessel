package dev.vessel.bootstrap.target;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compiles the Universal Client Presentation Resource Pack ({@code vessel-pack.zip}) by aggregating
 * client assets (textures, block/item/entity models, blockstates, sounds, lang) across all discovered
 * mod and plugin JARs in {@code mods/} and {@code plugins/}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
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

    public VesselUniversalResourcePackCompiler() {
    }

    /**
     * Compiles all assets found across {@code mods/} and {@code plugins/} into {@code packZip} and
     * returns the SHA-1 hex hash of the resulting resource pack archive, targeting modern 1.21.4 / 26.3.
     *
     * @param serverRoot the root server directory
     * @param packZip    the target path for {@code vessel-pack.zip}
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
     * @param packZip    the target path for {@code vessel-pack.zip}
     * @param mcVersion  active Minecraft version (e.g. {@code "1.12.2"}, {@code "1.20.1"}, {@code "26.3"})
     * @return 40-character lowercase hexadecimal SHA-1 string
     * @throws IOException on I/O or archive writing errors
     */
    public String compileResourcePack(Path serverRoot, Path packZip, String mcVersion) throws IOException {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(packZip, "packZip must not be null");

        Files.createDirectories(packZip.getParent());
        Set<String> packagedEntries = new HashSet<>();
        int packFormat = packFormatResolver.resolvePackFormat(mcVersion);

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(packZip)))) {
            // 1. pack.mcmeta with dynamic pack_format for the detected Minecraft version
            ZipEntry mcmetaEntry = new ZipEntry("pack.mcmeta");
            zos.putNextEntry(mcmetaEntry);
            String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": "
                    + packFormat
                    + ",\n    \"description\": \"VESSEL Sovereign Universal Mod Presentation Pack\"\n  }\n}\n";
            zos.write(mcmeta.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            packagedEntries.add("pack.mcmeta");

            // 2. assets/vessel/textures/item/vessel_core.png
            ZipEntry defaultCore = new ZipEntry("assets/vessel/textures/item/vessel_core.png");
            zos.putNextEntry(defaultCore);
            zos.write(MINIMAL_PNG);
            zos.closeEntry();
            packagedEntries.add("assets/vessel/textures/item/vessel_core.png");

            // 3. Scan serverRoot/mods and serverRoot/plugins for assets/
            aggregateAssetsFromDirectory(serverRoot.resolve("mods"), zos, packagedEntries);
            aggregateAssetsFromDirectory(serverRoot.resolve("plugins"), zos, packagedEntries);
        }

        byte[] packBytes = Files.readAllBytes(packZip);
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(sha1Digest.digest(packBytes));
        } catch (Exception e) {
            throw new IOException("Failed to calculate SHA-1 of compiled resource pack", e);
        }
    }

    private static void aggregateAssetsFromDirectory(
            Path dir,
            ZipOutputStream zos,
            Set<String> packagedEntries) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        File[] files = dir.toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                extractJarAssetsIntoPack(file, zos, packagedEntries);
            }
        }
    }

    private static void extractJarAssetsIntoPack(
            File jarFile,
            ZipOutputStream zos,
            Set<String> packagedEntries) {
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
                if (!packagedEntries.add(name)) {
                    continue;
                }
                ZipEntry newEntry = new ZipEntry(name);
                zos.putNextEntry(newEntry);
                try (InputStream in = jar.getInputStream(entry)) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
        } catch (Exception ignored) {
        }
    }
}
