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
 * Specialized parser for Fabric and Quilt metadata descriptors (fabric.mod.json, quilt.mod.json).
 * <p>
 * Extracts canonical module identifier fields in accordance with Clean-Room standards without
 * runtime external JSON library baggage.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class FabricModJsonParser {

    private static final Pattern FABRIC_ID_PATTERN = Pattern.compile(
            "\"id\"\\s*:\\s*\"([^\"]+)\"", Pattern.MULTILINE
    );
    private static final Pattern FABRIC_VERSION_PATTERN = Pattern.compile(
            "\"version\"\\s*:\\s*\"([^\"]+)\"", Pattern.MULTILINE
    );
    private static final Pattern QUILT_LOADER_BLOCK = Pattern.compile(
            "\"quilt_loader\"\\s*:\\s*\\{([^}]+)\\}", Pattern.DOTALL
    );

    /**
     * Parses metadata from an input stream.
     *
     * @param stream input stream containing JSON metadata
     * @param isQuilt whether the descriptor represents quilt.mod.json
     * @return the extracted canonical module identifier
     * @throws InvalidMetadataException if the metadata is malformed or missing id/version
     */
    public VesselModuleIdentifier parse(InputStream stream, boolean isQuilt) {
        Objects.requireNonNull(stream, "stream cannot be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return parseContent(sb.toString(), isQuilt);
        } catch (IOException e) {
            throw new InvalidMetadataException("Failed to read Fabric/Quilt metadata stream", e);
        }
    }

    /**
     * Parses metadata from a JSON string.
     *
     * @param content raw JSON string content
     * @param isQuilt whether the descriptor represents quilt.mod.json
     * @return the extracted canonical module identifier
     * @throws InvalidMetadataException if the metadata is malformed or missing id/version
     */
    public VesselModuleIdentifier parseContent(String content, boolean isQuilt) {
        Objects.requireNonNull(content, "content cannot be null");

        String id = null;
        String version = null;

        if (isQuilt) {
            Matcher quiltMatcher = QUILT_LOADER_BLOCK.matcher(content);
            if (quiltMatcher.find()) {
                String quiltBlock = quiltMatcher.group(1);
                Matcher idMatcher = FABRIC_ID_PATTERN.matcher(quiltBlock);
                if (idMatcher.find()) {
                    id = idMatcher.group(1);
                }
                Matcher verMatcher = FABRIC_VERSION_PATTERN.matcher(quiltBlock);
                if (verMatcher.find()) {
                    version = verMatcher.group(1);
                }
            }
        }

        if (id == null) {
            Matcher idMatcher = FABRIC_ID_PATTERN.matcher(content);
            if (idMatcher.find()) {
                id = idMatcher.group(1);
            }
        }

        if (version == null) {
            Matcher verMatcher = FABRIC_VERSION_PATTERN.matcher(content);
            if (verMatcher.find()) {
                version = verMatcher.group(1);
            }
        }

        if (id == null || id.isBlank()) {
            throw new InvalidMetadataException("Missing or empty 'id' in " + (isQuilt ? "quilt.mod.json" : "fabric.mod.json"));
        }
        if (version == null || version.isBlank()) {
            throw new InvalidMetadataException("Missing or empty 'version' for module '" + id + "'");
        }

        SourceLoaderKind loader = isQuilt ? SourceLoaderKind.QUILT : SourceLoaderKind.FABRIC;
        return VesselModuleIdentifier.of(loader, id, version);
    }
}
