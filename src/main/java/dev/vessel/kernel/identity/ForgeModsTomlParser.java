package dev.vessel.kernel.identity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized parser for Forge and NeoForge module descriptors (mods.toml, neoforge.mods.toml).
 * <p>
 * Extracts {@code modId} and {@code version} within the {@code [[mods]]} table block in
 * accordance with Clean-Room standards without external TOML library dependencies.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-4)
 */
public class ForgeModsTomlParser {

    private static final Pattern MOD_ID_PATTERN = Pattern.compile("^\\s*modId\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.MULTILINE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\s*version\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.MULTILINE);

    /**
     * Parses metadata from an input stream.
     *
     * @param stream input stream containing TOML descriptor
     * @param isNeoForge whether this descriptor originates from neoforge.mods.toml
     * @return the extracted canonical module identifier
     * @throws InvalidMetadataException if modId or version is missing or malformed
     */
    public VesselModuleIdentifier parse(InputStream stream, boolean isNeoForge) {
        Objects.requireNonNull(stream, "stream cannot be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return parseContent(sb.toString(), isNeoForge);
        } catch (IOException e) {
            throw new InvalidMetadataException("Failed to read Forge/NeoForge metadata stream", e);
        }
    }

    /**
     * Parses metadata from TOML content string.
     *
     * @param content raw TOML string
     * @param isNeoForge whether this descriptor originates from neoforge.mods.toml
     * @return the extracted canonical module identifier
     * @throws InvalidMetadataException if modId or version is missing
     */
    public VesselModuleIdentifier parseContent(String content, boolean isNeoForge) {
        Objects.requireNonNull(content, "content cannot be null");

        String modId = null;
        String version = null;

        Matcher modIdMatcher = MOD_ID_PATTERN.matcher(content);
        if (modIdMatcher.find()) {
            modId = modIdMatcher.group(1).trim();
        }

        Matcher versionMatcher = VERSION_PATTERN.matcher(content);
        if (versionMatcher.find()) {
            version = versionMatcher.group(1).trim();
        }

        if (modId == null || modId.isBlank()) {
            throw new InvalidMetadataException("Missing or empty 'modId' in " + (isNeoForge ? "neoforge.mods.toml" : "mods.toml"));
        }
        if (version == null || version.isBlank()) {
            throw new InvalidMetadataException("Missing or empty 'version' for mod '" + modId + "'");
        }

        SourceLoaderKind loader = isNeoForge ? SourceLoaderKind.NEOFORGE : SourceLoaderKind.FORGE;
        return VesselModuleIdentifier.of(loader, modId, version);
    }
}
