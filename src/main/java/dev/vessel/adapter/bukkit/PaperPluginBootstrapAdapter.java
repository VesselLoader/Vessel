package dev.vessel.adapter.bukkit;

import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses modern Paper {@code paper-plugin.yml} descriptors into {@code PAPER}-scoped
 * {@link VesselModuleIdentifier} records and extracts declared Maven library coordinates.
 *
 * @version 1.0.0
 */
public final class PaperPluginBootstrapAdapter {

    /**
     * Parses a {@code paper-plugin.yml} descriptor into a canonical {@link VesselModuleIdentifier}.
     *
     * @param yamlContent the raw {@code paper-plugin.yml} content
     * @return the canonical {@code PAPER} module identifier
     */
    public VesselModuleIdentifier parsePaperPluginYaml(String yamlContent) {
        Objects.requireNonNull(yamlContent, "yamlContent must not be null");
        String name = "unknown_paper_plugin";
        String version = "1.0.0";
        boolean foliaSupported = false;

        String[] lines = yamlContent.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = stripQuotes(line.substring(colon + 1).trim());
            if ("name".equalsIgnoreCase(key) && !value.isEmpty()) {
                name = sanitizeNamespace(value);
            } else if ("version".equalsIgnoreCase(key) && !value.isEmpty()) {
                version = value;
            } else if ("folia-supported".equalsIgnoreCase(key)) {
                foliaSupported = Boolean.parseBoolean(value);
            }
        }
        SourceLoaderKind loaderKind = foliaSupported
                ? SourceLoaderKind.FOLIA
                : SourceLoaderKind.PAPER;
        return VesselModuleIdentifier.of(loaderKind, name, version);
    }

    /**
     * Extracts Maven library coordinate strings (e.g., {@code group:artifact:version}) from a
     * {@code paper-plugin.yml} or {@code plugin.yml} {@code libraries:} list.
     *
     * @param yamlContent the raw YAML descriptor content
     * @return the immutable list of Maven library coordinates
     */
    public List<String> extractPaperLibraryCoordinates(String yamlContent) {
        Objects.requireNonNull(yamlContent, "yamlContent must not be null");
        List<String> coordinates = new ArrayList<>();
        boolean inLibrariesSection = false;

        String[] lines = yamlContent.split("\\R");
        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("libraries:")) {
                inLibrariesSection = true;
                continue;
            }
            if (inLibrariesSection) {
                if (trimmed.startsWith("-")) {
                    String coord = stripQuotes(trimmed.substring(1).trim());
                    if (!coord.isEmpty()) {
                        coordinates.add(coord);
                    }
                } else if (!rawLine.startsWith(" ") && !rawLine.startsWith("\t")) {
                    inLibrariesSection = false;
                }
            }
        }
        return List.copyOf(coordinates);
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static String sanitizeNamespace(String raw) {
        String sanitized = raw.trim().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
        return sanitized.isEmpty() ? "unknown_paper_plugin" : sanitized;
    }
}
