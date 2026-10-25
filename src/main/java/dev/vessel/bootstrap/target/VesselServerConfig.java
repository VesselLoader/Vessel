package dev.vessel.bootstrap.target;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the root {@code vessel.yml} configuration file containing all 34 annotated
 * enterprise configuration sections for the VESSEL microkernel.
 *
 * @version 1.0.0
 */
public final class VesselServerConfig {

    private static final List<String> CANONICAL_34_SECTIONS = List.of(
            "branding",
            "actor-scheduler",
            "world-and-chunks",
            "redstone-and-physics",
            "chunk-loader-governor",
            "mob-spawning-and-despawn",
            "optimizations",
            "network-and-security",
            "loader-runtime",
            "dual-sided-mods",
            "mod-specific-optimizations",
            "client-presentation",
            "logging",
            "hybrid-compatibility",
            "proxy-integration",
            "entity-density-limits",
            "voice-chat",
            "maintenance",
            "anti-exploit",
            "microblock-compression",
            "client-inventory-tweaks",
            "cross-chunk-governor",
            "industrial-and-recipe-accelerators",
            "world-event-batching",
            "memory-and-npc-guards",
            "hybrid-overload-and-dfu",
            "fluid-and-falling-block-governor",
            "tree-canopy-decay-scheduler",
            "natural-spawner-and-creatures",
            "instanced-loot-and-world-map",
            "nested-backpack-and-curios",
            "botania-and-corporea",
            "valkyrien-physics-governor",
            "lua-computer-governor"
    );

    private final Map<String, String> flatProperties = new ConcurrentHashMap<>();

    /**
     * Creates a new server configuration manager populated with in-memory defaults.
     */
    public VesselServerConfig() {
        populateBuiltinDefaults();
    }

    /**
     * Loads {@code vessel.yml} from the specified server root directory, or generates
     * the default 34-section annotated {@code vessel.yml} if it does not yet exist.
     *
     * @param serverRoot the root directory of the server
     */
    public void loadOrGenerateDefaults(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        populateBuiltinDefaults();
        try {
            Files.createDirectories(serverRoot);
            Path vesselYamlPath = serverRoot.resolve("vessel.yml");
            if (!Files.exists(vesselYamlPath)) {
                Files.writeString(vesselYamlPath, generateDefaultAnnotatedVesselYaml(), StandardCharsets.UTF_8);
            }
            List<String> lines = Files.readAllLines(vesselYamlPath, StandardCharsets.UTF_8);
            parseSimpleHierarchyYaml(lines);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load or generate vessel.yml in " + serverRoot, ex);
        }
    }

    /**
     * Retrieves a string configuration value by dot-separated key path.
     *
     * @param keyPath      the dot-separated key path (e.g., {@code "server-identity.brand"})
     * @param defaultValue fallback value if the key is absent
     * @return resolved string configuration value
     */
    public String getString(String keyPath, String defaultValue) {
        Objects.requireNonNull(keyPath, "keyPath must not be null");
        return flatProperties.getOrDefault(keyPath, defaultValue);
    }

    /**
     * Retrieves an integer configuration value by dot-separated key path.
     *
     * @param keyPath      the dot-separated key path
     * @param defaultValue fallback integer value if absent or unparseable
     * @return resolved integer configuration value
     */
    public int getInt(String keyPath, int defaultValue) {
        Objects.requireNonNull(keyPath, "keyPath must not be null");
        String raw = flatProperties.get(keyPath);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Retrieves a boolean configuration value by dot-separated key path.
     *
     * @param keyPath      the dot-separated key path
     * @param defaultValue fallback boolean value if absent
     * @return resolved boolean configuration value
     */
    public boolean getBoolean(String keyPath, boolean defaultValue) {
        Objects.requireNonNull(keyPath, "keyPath must not be null");
        String raw = flatProperties.get(keyPath);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * Generates the complete default annotated {@code vessel.yml} containing all 34
     * canonical configuration sections.
     *
     * @return annotated YAML document string
     */
    public String generateDefaultAnnotatedVesselYaml() {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("# ============================================================================\n");
        sb.append("# VESSEL Sovereign Microkernel Enterprise Configuration (34 Canonical Sections)\n");
        sb.append("# ============================================================================\n\n");
        int sectionNumber = 1;
        for (String section : CANONICAL_34_SECTIONS) {
            sb.append("# Section ").append(sectionNumber).append(": ").append(section).append('\n');
            sb.append(section).append(":\n");
            sb.append("  enabled: true\n");
            if ("branding".equals(section)) {
                sb.append("  server-brand-name: \"VESSEL\"\n");
                sb.append("  f3-debug-brand: \"VESSEL\"\n");
            } else if ("actor-scheduler".equals(section)) {
                sb.append("  use-virtual-threads: true\n");
            } else if ("proxy-integration".equals(section)) {
                sb.append("  protocol: \"VELOCITY_MODERN\"\n");
                sb.append("  velocity-forwarding-secret: \"\"\n");
            } else if ("lua-computer-governor".equals(section)) {
                sb.append("  max-cpu-slice-micros-per-tick: 250\n");
            } else {
                sb.append("  profile: \"enterprise\"\n");
            }
            sb.append('\n');
            sectionNumber++;
        }
        return sb.toString();
    }

    private void populateBuiltinDefaults() {
        for (String section : CANONICAL_34_SECTIONS) {
            flatProperties.put(section + ".enabled", "true");
            flatProperties.put(section + ".profile", "enterprise");
        }
        flatProperties.put("branding.server-brand-name", "VESSEL");
        flatProperties.put("branding.f3-debug-brand", "VESSEL");
        flatProperties.put("actor-scheduler.use-virtual-threads", "true");
        flatProperties.put("proxy-integration.protocol", "VELOCITY_MODERN");
        flatProperties.put("proxy-integration.velocity-forwarding-secret", "");
        flatProperties.put("lua-computer-governor.max-cpu-slice-micros-per-tick", "250");
        flatProperties.put("server-identity.brand", "VESSEL");
        flatProperties.put("startup-build-stage.first-run-warning-seconds", "45");
        flatProperties.put("regional-strand-scheduler.enabled", "true");
    }

    private void parseSimpleHierarchyYaml(List<String> lines) {
        String currentSection = null;
        for (String rawLine : lines) {
            if (rawLine == null) {
                continue;
            }
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            boolean indented = rawLine.startsWith(" ") || rawLine.startsWith("\t");
            int colonIdx = trimmed.indexOf(':');
            if (colonIdx <= 0) {
                continue;
            }
            String key = trimmed.substring(0, colonIdx).trim();
            String valuePart = trimmed.substring(colonIdx + 1).trim();
            if (!indented && valuePart.isEmpty()) {
                currentSection = key;
            } else {
                String cleanValue = stripQuotes(valuePart);
                String fullKey = (indented && currentSection != null) ? currentSection + "." + key : key;
                flatProperties.put(fullKey, cleanValue);
            }
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
