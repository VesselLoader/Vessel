package dev.vessel.kernel.identity;

import java.io.InputStream;
import java.util.Objects;

/**
 * Coordination dispatcher for module identity extraction across all supported loader ecosystems.
 * <p>
 * Strictly delegates format-specific parsing to specialized single-responsibility parsers
 * in compliance with Rule 17.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-4)
 */
public class ModuleIdentityParser {

    private final FabricModJsonParser fabricParser;
    private final BukkitPluginYamlParser bukkitParser;
    private final ForgeModsTomlParser forgeParser;

    public ModuleIdentityParser(
            FabricModJsonParser fabricParser,
            BukkitPluginYamlParser bukkitParser,
            ForgeModsTomlParser forgeParser
    ) {
        this.fabricParser = Objects.requireNonNull(fabricParser, "fabricParser cannot be null");
        this.bukkitParser = Objects.requireNonNull(bukkitParser, "bukkitParser cannot be null");
        this.forgeParser = Objects.requireNonNull(forgeParser, "forgeParser cannot be null");
    }

    /**
     * Default constructor instantiating the individual specialized parsers.
     */
    public ModuleIdentityParser() {
        this(new FabricModJsonParser(), new BukkitPluginYamlParser(), new ForgeModsTomlParser());
    }

    /**
     * Dispatches parsing based on descriptor file path and input stream.
     *
     * @param descriptorPath the relative path of the metadata file within the JAR
     * @param stream the input stream containing the file bytes
     * @return the resolved canonical module identifier
     * @throws InvalidMetadataException if the file is unknown or malformed
     */
    public VesselModuleIdentifier parse(String descriptorPath, InputStream stream) {
        Objects.requireNonNull(descriptorPath, "descriptorPath cannot be null");
        Objects.requireNonNull(stream, "stream cannot be null");

        String normalized = descriptorPath.trim().replace('\\', '/').toLowerCase();
        if (normalized.endsWith("fabric.mod.json")) {
            return fabricParser.parse(stream, false);
        } else if (normalized.endsWith("quilt.mod.json")) {
            return fabricParser.parse(stream, true);
        } else if (normalized.endsWith("paper-plugin.yml")) {
            return bukkitParser.parse(stream, true);
        } else if (normalized.endsWith("plugin.yml")) {
            return bukkitParser.parse(stream, false);
        } else if (normalized.endsWith("neoforge.mods.toml")) {
            return forgeParser.parse(stream, true);
        } else if (normalized.endsWith("mods.toml")) {
            return forgeParser.parse(stream, false);
        }

        throw new InvalidMetadataException("Unrecognized module metadata descriptor: " + descriptorPath);
    }
}
