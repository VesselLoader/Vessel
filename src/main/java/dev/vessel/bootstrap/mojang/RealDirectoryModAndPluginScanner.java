package dev.vessel.bootstrap.mojang;

import dev.vessel.adapter.bukkit.BukkitPluginBootstrapAdapter;
import dev.vessel.adapter.bukkit.PaperPluginBootstrapAdapter;
import dev.vessel.adapter.neoforge.NeoForgeMetadataAdapter;
import dev.vessel.kernel.identity.QuiltModJsonParser;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans physical {@code .jar} files inside {@code mods/} and {@code plugins/} (and their
 * ecosystem-scoped subdirectories), inspecting internal metadata descriptors
 * ({@code fabric.mod.json}, {@code quilt.mod.json}, {@code META-INF/neoforge.mods.toml},
 * {@code META-INF/mods.toml}, {@code paper-plugin.yml}, {@code plugin.yml}) or falling back to
 * lenient filename inference so virtually all real-world mods and plugins load in a single pass.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class RealDirectoryModAndPluginScanner {

    private static final Pattern JSON_ID_PATTERN =
            Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_VERSION_PATTERN =
            Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");

    private final QuiltModJsonParser quiltParser = new QuiltModJsonParser();
    private final NeoForgeMetadataAdapter neoForgeAdapter = new NeoForgeMetadataAdapter();
    private final PaperPluginBootstrapAdapter paperAdapter = new PaperPluginBootstrapAdapter();
    private final BukkitPluginBootstrapAdapter bukkitAdapter = new BukkitPluginBootstrapAdapter();

    /**
     * Scans {@code serverRoot/mods} and {@code serverRoot/plugins} recursively for {@code .jar}
     * archives and resolves each archive into its canonical {@link VesselModuleIdentifier}.
     *
     * @param serverRoot the server root directory
     * @return an ordered map from physical {@code .jar} path to {@link VesselModuleIdentifier}
     */
    public Map<Path, VesselModuleIdentifier> scanAllModAndPluginJars(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Map<Path, VesselModuleIdentifier> discovered = new LinkedHashMap<>();
        scanDirectoryTree(serverRoot.resolve("mods"), SourceLoaderKind.FABRIC, discovered);
        scanDirectoryTree(serverRoot.resolve("plugins"), SourceLoaderKind.PAPER, discovered);
        return Map.copyOf(discovered);
    }

    /**
     * Inspects a single {@code .jar} archive and extracts or infers its canonical
     * {@link VesselModuleIdentifier}.
     *
     * @param jarPath       the physical JAR file path
     * @param defaultLoader the fallback loader kind if the JAR contains no standard descriptor
     * @return the resolved {@link VesselModuleIdentifier}
     */
    public VesselModuleIdentifier inspectJarModuleIdentity(
            Path jarPath,
            SourceLoaderKind defaultLoader) {
        Objects.requireNonNull(jarPath, "jarPath must not be null");
        Objects.requireNonNull(defaultLoader, "defaultLoader must not be null");

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String quiltJson = readEntryText(jar, "quilt.mod.json");
            if (quiltJson != null) {
                return quiltParser.parseQuiltModJson(quiltJson);
            }

            String fabricJson = readEntryText(jar, "fabric.mod.json");
            if (fabricJson != null) {
                return parseFabricModJson(fabricJson);
            }

            String neoToml = readEntryText(jar, "META-INF/neoforge.mods.toml");
            if (neoToml != null) {
                return neoForgeAdapter.parseNeoForgeToml(neoToml);
            }

            String forgeToml = readEntryText(jar, "META-INF/mods.toml");
            if (forgeToml != null) {
                VesselModuleIdentifier parsed = neoForgeAdapter.parseNeoForgeToml(forgeToml);
                return VesselModuleIdentifier.of(
                        SourceLoaderKind.FORGE, parsed.namespace(), parsed.version());
            }

            String paperYaml = readEntryText(jar, "paper-plugin.yml");
            if (paperYaml != null) {
                return paperAdapter.parsePaperPluginYaml(paperYaml);
            }

            String bukkitYaml = readEntryText(jar, "plugin.yml");
            if (bukkitYaml != null) {
                return bukkitAdapter.loadLegacyBukkitPlugin(bukkitYaml);
            }
        } catch (Exception ignored) {
            // Fall through to lenient filename inference
        }

        return inferFromFileName(jarPath, defaultLoader);
    }

    private void scanDirectoryTree(
            Path rootDir,
            SourceLoaderKind fallbackLoader,
            Map<Path, VesselModuleIdentifier> outMap) {
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(rootDir)) {
            List<Path> jars = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                    .sorted()
                    .toList();
            for (Path jarPath : jars) {
                SourceLoaderKind hint = inferLoaderFromParentFolder(jarPath, fallbackLoader);
                outMap.put(jarPath, inspectJarModuleIdentity(jarPath, hint));
            }
        } catch (IOException ignored) {
            // Ignore unreadable directories
        }
    }

    private static SourceLoaderKind inferLoaderFromParentFolder(
            Path jarPath,
            SourceLoaderKind fallback) {
        String normalizedPath = jarPath.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalizedPath.contains("/mods/neoforge/")) {
            return SourceLoaderKind.NEOFORGE;
        }
        if (normalizedPath.contains("/mods/forge/")) {
            return SourceLoaderKind.FORGE;
        }
        if (normalizedPath.contains("/mods/quilt/")) {
            return SourceLoaderKind.QUILT;
        }
        if (normalizedPath.contains("/mods/fabric/")) {
            return SourceLoaderKind.FABRIC;
        }
        if (normalizedPath.contains("/plugins/folia/")) {
            return SourceLoaderKind.FOLIA;
        }
        if (normalizedPath.contains("/plugins/paper/")) {
            return SourceLoaderKind.PAPER;
        }
        if (normalizedPath.contains("/plugins/spigot/")) {
            return SourceLoaderKind.SPIGOT;
        }
        if (normalizedPath.contains("/plugins/bukkit/")) {
            return SourceLoaderKind.BUKKIT;
        }
        return fallback;
    }

    private static VesselModuleIdentifier parseFabricModJson(String json) {
        Matcher idMatcher = JSON_ID_PATTERN.matcher(json);
        String id = idMatcher.find() ? sanitizeNamespace(idMatcher.group(1)) : "fabric_mod";
        Matcher verMatcher = JSON_VERSION_PATTERN.matcher(json);
        String version = verMatcher.find() ? verMatcher.group(1).trim() : "1.0.0";
        if (version.isEmpty() || "${version}".equals(version)) {
            version = "1.0.0";
        }
        return VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, id, version);
    }

    private static VesselModuleIdentifier inferFromFileName(
            Path jarPath,
            SourceLoaderKind loaderKind) {
        String fileName = jarPath.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        int dashIdx = fileName.indexOf('-');
        String rawName = dashIdx > 0 ? fileName.substring(0, dashIdx) : fileName;
        String rawVer = dashIdx > 0 ? fileName.substring(dashIdx + 1) : "1.0.0";
        String namespace = sanitizeNamespace(rawName);
        String version = rawVer.trim().isEmpty() ? "1.0.0" : rawVer.trim();
        return VesselModuleIdentifier.of(loaderKind, namespace, version);
    }

    private static String sanitizeNamespace(String raw) {
        String cleaned = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return cleaned.isEmpty() ? "unknown_module" : cleaned;
    }

    private static String readEntryText(JarFile jar, String entryPath) throws IOException {
        JarEntry entry = jar.getJarEntry(entryPath);
        if (entry == null) {
            return null;
        }
        try (InputStream in = jar.getInputStream(entry)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
