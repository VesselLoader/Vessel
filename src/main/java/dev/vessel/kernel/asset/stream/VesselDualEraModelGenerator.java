package dev.vessel.kernel.asset.stream;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dual-era item model generator providing seamless visual presentation across both legacy
 * Minecraft versions (1.20.1 - 1.20.4 via {@code CustomModelData} overrides) and modern versions
 * (1.20.5+ / 26.3 via {@code item_model} DataComponents and {@code minecraft:range_dispatch}).
 * <p>
 * Ensures that a single compiled resource pack is simultaneously compatible with older 1.20.x clients
 * and cutting-edge 1.21+ / 26.x clients without requiring separate packs or protocol translators.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselDualEraModelGenerator {

    private final LegacyCustomModelDataCompiler legacyCompiler = new LegacyCustomModelDataCompiler();
    private final ModernItemModelDefinitionCompiler modernCompiler = new ModernItemModelDefinitionCompiler();

    /**
     * Compiles definitions for both eras simultaneously, generating dual-compatible asset paths.
     *
     * @param definitions list of item model definitions
     * @return unified map of relative asset paths to generated JSON contents
     */
    public Map<String, String> generateDualEraModels(List<ItemModelSourceDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions cannot be null");
        if (definitions.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> unified = new LinkedHashMap<>();

        // 1. Generate legacy 1.20.1-1.20.4 CustomModelData overrides (assets/<ns>/models/item/*.json)
        Map<String, String> legacyAssets = legacyCompiler.compileLegacyPack(definitions);
        unified.putAll(legacyAssets);

        // 2. Generate modern 1.20.5+ DataComponent item models (assets/<ns>/items/*.json)
        Map<String, String> modernAssets = modernCompiler.compileModernPack(definitions);
        unified.putAll(modernAssets);

        return Collections.unmodifiableMap(unified);
    }

    /**
     * Compiles definitions targeting a specific active Minecraft version.
     *
     * @param definitions list of item model definitions
     * @param mcVersion active Minecraft version string (e.g. {@code "1.20.1"}, {@code "1.21.4"}, {@code "26.3"})
     * @return map of relative asset paths to generated JSON contents
     */
    public Map<String, String> generateForVersion(List<ItemModelSourceDefinition> definitions, String mcVersion) {
        Objects.requireNonNull(definitions, "definitions cannot be null");
        Objects.requireNonNull(mcVersion, "mcVersion cannot be null");

        if (isModernVersion(mcVersion)) {
            // Modern or dual generation: supply both so hybrid proxies and multi-version clients both work
            return generateDualEraModels(definitions);
        } else {
            return legacyCompiler.compileLegacyPack(definitions);
        }
    }

    /**
     * Determines whether the given Minecraft version uses modern 1.20.5+ item model definitions.
     *
     * @param version version string
     * @return true if 1.20.5 or newer
     */
    public static boolean isModernVersion(String version) {
        if (version == null || version.isBlank()) {
            return true;
        }

        String cleaned = version.replaceAll("[^0-9.]", "").trim();
        String[] parts = cleaned.split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                if (major > 1) {
                    return true; // e.g. 26.3
                }
                if (major == 1) {
                    if (minor > 20) {
                        return true; // 1.21+
                    }
                    if (minor == 20 && parts.length >= 3) {
                        int patch = Integer.parseInt(parts[2]);
                        return patch >= 5; // 1.20.5+
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return true;
    }
}
