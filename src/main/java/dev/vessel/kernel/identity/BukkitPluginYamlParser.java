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
 * Specialized parser for Bukkit ecosystem plugin descriptors (plugin.yml, paper-plugin.yml).
 * <p>
 * Handles property extraction according to Clean-Room standards without external YAML parser dependencies.
 * Automatically inspects Folia support flags to assign the most specific {@link SourceLoaderKind}.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class BukkitPluginYamlParser {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\s*name\\s*:\\s*[\"']?([^\"'#\r\n]+)[\"']?", Pattern.MULTILINE);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\s*version\\s*:\\s*[\"']?([^\"'#\r\n]+)[\"']?", Pattern.MULTILINE);
    private static final Pattern FOLIA_SUPPORTED_PATTERN = Pattern.compile("^\\s*folia-supported\\s*:\\s*[\"']?(true)[\"']?", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    /**
     * Parses metadata from an input stream.
     *
     * @param stream input stream containing YAML descriptor
     * @param isPaperPlugin whether this descriptor is a paper-plugin.yml
     * @return the extracted canonical module identifier
     * @throws InvalidMetadataException if name or version is missing or malformed
     */
    public VesselModuleIdentifier parse(InputStream stream, boolean isPaperPlugin) {
        Objects.requireNonNull(stream, "stream cannot be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return parseContent(sb.toString(), isPaperPlugin);
        } catch (IOException e) {
            throw new InvalidMetadataException("Failed to read Bukkit plugin metadata stream", e);
        }
    }

    /**
     * Parses metadata from YAML content string.
     *
     * @param content raw YAML string
     * @param isPaperPlugin whether this descriptor is a paper-plugin.yml
     * @return the extracted canonical module identifier
     * @throws InvalidMetadataException if name or version is missing
     */
    public VesselModuleIdentifier parseContent(String content, boolean isPaperPlugin) {
        Objects.requireNonNull(content, "content cannot be null");

        String name = null;
        String version = null;
        boolean foliaSupported = false;

        Matcher nameMatcher = NAME_PATTERN.matcher(content);
        if (nameMatcher.find()) {
            name = nameMatcher.group(1).trim();
        }

        Matcher versionMatcher = VERSION_PATTERN.matcher(content);
        if (versionMatcher.find()) {
            version = versionMatcher.group(1).trim();
        }

        Matcher foliaMatcher = FOLIA_SUPPORTED_PATTERN.matcher(content);
        if (foliaMatcher.find()) {
            foliaSupported = true;
        }

        if (name == null || name.isBlank()) {
            throw new InvalidMetadataException("Missing or empty 'name' in Bukkit plugin descriptor");
        }
        if (version == null || version.isBlank()) {
            throw new InvalidMetadataException("Missing or empty 'version' in plugin descriptor for '" + name + "'");
        }

        SourceLoaderKind loader;
        if (foliaSupported) {
            loader = SourceLoaderKind.FOLIA;
        } else if (isPaperPlugin) {
            loader = SourceLoaderKind.PAPER;
        } else {
            loader = SourceLoaderKind.BUKKIT;
        }

        return VesselModuleIdentifier.of(loader, name, version);
    }
}
