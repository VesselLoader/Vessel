package dev.vessel.kernel.compat.era;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforces VESSEL's strict forward-version guard: while mods and plugins targeting older or equal
 * Minecraft versions ({@code modMcVersion <= serverMcVersion}) are automatically upgraded and
 * loaded, any mod or plugin requiring a strictly newer Minecraft version than the running server
 * ({@code modMcVersion > serverMcVersion}) is immediately refused with an explicit remediation
 * message.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ForwardVersionModRejectionGuard {

    private static final Pattern JSON_MC_DEPENDENCY_PATTERN =
            Pattern.compile("\"minecraft\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TOML_MC_BLOCK_PATTERN =
            Pattern.compile(
                    "modId\\s*=\\s*\"minecraft\"[\\s\\S]*?versionRange\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern YAML_API_VERSION_PATTERN =
            Pattern.compile("(?m)^\\s*api-version\\s*:\\s*['\"]?([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)['\"]?");

    private final SemanticMinecraftVersionComparator comparator =
            new SemanticMinecraftVersionComparator();

    /**
     * Extracts the declared Minecraft version requirement from a raw metadata descriptor text
     * ({@code fabric.mod.json}, {@code quilt.mod.json}, {@code mods.toml},
     * {@code neoforge.mods.toml}, {@code plugin.yml}, or {@code paper-plugin.yml}).
     *
     * @param metadataText raw metadata content
     * @return minimum required Minecraft version (e.g., {@code "1.21.4"}), or {@code ""} if none
     */
    public String extractDeclaredMinecraftVersion(String metadataText) {
        if (metadataText == null || metadataText.isBlank()) {
            return "";
        }
        Matcher jsonMatcher = JSON_MC_DEPENDENCY_PATTERN.matcher(metadataText);
        if (jsonMatcher.find()) {
            return comparator.extractMinimumRequiredMinecraftVersion(jsonMatcher.group(1));
        }
        Matcher tomlMatcher = TOML_MC_BLOCK_PATTERN.matcher(metadataText);
        if (tomlMatcher.find()) {
            return comparator.extractMinimumRequiredMinecraftVersion(tomlMatcher.group(1));
        }
        Matcher yamlMatcher = YAML_API_VERSION_PATTERN.matcher(metadataText);
        if (yamlMatcher.find()) {
            return comparator.extractMinimumRequiredMinecraftVersion(yamlMatcher.group(1));
        }
        return "";
    }

    /**
     * Inspects a physical mod or plugin {@code .jar} archive and extracts its minimum required
     * Minecraft version if declared.
     *
     * @param jarPath path to the {@code .jar} archive
     * @return minimum required Minecraft version string, or {@code ""} if unconstrained
     */
    public String extractRequiredMinecraftVersionFromJar(Path jarPath) {
        Objects.requireNonNull(jarPath, "jarPath must not be null");
        if (!Files.isRegularFile(jarPath)) {
            return "";
        }
        String[] candidateDescriptors = new String[]{
            "fabric.mod.json",
            "quilt.mod.json",
            "META-INF/neoforge.mods.toml",
            "META-INF/mods.toml",
            "paper-plugin.yml",
            "plugin.yml"
        };
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            for (String descriptorPath : candidateDescriptors) {
                JarEntry entry = jarFile.getJarEntry(descriptorPath);
                if (entry != null) {
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        String extracted = extractDeclaredMinecraftVersion(text);
                        if (!extracted.isEmpty()) {
                            return extracted;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Unreadable JAR handled by caller
        }
        return "";
    }

    /**
     * Verifies that the active server Minecraft version is at least as new as the version required
     * by the module. If the server is running a lower Minecraft version than the module requires,
     * immediately throws {@link ForwardVersionModRejectedException}.
     *
     * @param moduleId              canonical module identifier or plugin name
     * @param requiredMcConstraint  declared Minecraft version constraint of the mod/plugin
     * @param activeServerMcVersion Minecraft version currently running on the server
     * @throws ForwardVersionModRejectedException if {@code requiredMcConstraint > activeServerMcVersion}
     */
    public void enforceServerVersionNotLowerThanMod(
            String moduleId,
            String requiredMcConstraint,
            String activeServerMcVersion) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(activeServerMcVersion, "activeServerMcVersion must not be null");
        String normalizedModMc =
                comparator.extractMinimumRequiredMinecraftVersion(requiredMcConstraint);
        if (normalizedModMc.isEmpty()) {
            return;
        }
        if (comparator.isServerVersionLowerThanMod(normalizedModMc, activeServerMcVersion)) {
            throw new ForwardVersionModRejectedException(
                    moduleId,
                    normalizedModMc,
                    activeServerMcVersion);
        }
    }

    /**
     * Validates a physical mod or plugin {@code .jar} archive against the active server Minecraft
     * version, throwing {@link ForwardVersionModRejectedException} if the JAR requires a newer
     * Minecraft release than the running server.
     *
     * @param jarPath               physical {@code .jar} archive path
     * @param moduleId              module identifier or name
     * @param activeServerMcVersion Minecraft version currently running on the server
     * @throws ForwardVersionModRejectedException if the JAR targets a newer Minecraft version
     */
    public void validateJarAgainstServerVersion(
            Path jarPath,
            String moduleId,
            String activeServerMcVersion) {
        Objects.requireNonNull(jarPath, "jarPath must not be null");
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        Objects.requireNonNull(activeServerMcVersion, "activeServerMcVersion must not be null");
        String requiredMc = extractRequiredMinecraftVersionFromJar(jarPath);
        if (!requiredMc.isEmpty()) {
            enforceServerVersionNotLowerThanMod(moduleId, requiredMc, activeServerMcVersion);
        }
    }
}
