package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.engine.VstmDialectMask;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * High-performance streaming parser for NeoForge / Forge TSRG v2 format mapping files.
 *
 * <p>Ingests deobfuscation definitions (e.g. {@code joined.tsrg}) bridging obfuscated
 * symbols to Searge / NeoForge standard nomenclature (Catalog ref: TRN-0018).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class NeoForgeTsrg2Parser {

    private static final String TSRG2_HEADER_PREFIX = "tsrg2";
    private static final String DIALECT_NAME = "NeoForge-TSRG2";

    /**
     * Parses a TSRG v2 file from disk using default SRG dialect bitmask.
     *
     * @param path path to TSRG v2 file
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if syntax is malformed
     */
    public ParsedDialectResult parse(Path path) throws IOException {
        return parse(path, VstmDialectMask.SRG);
    }

    /**
     * Parses a TSRG v2 file from disk with an explicit dialect bitmask.
     *
     * @param path        path to TSRG v2 file
     * @param dialectMask active dialect validity bitmask (e.g., SRG or NEOFORGE)
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if syntax is malformed
     */
    public ParsedDialectResult parse(Path path, int dialectMask) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a TSRG v2 input stream with an explicit dialect bitmask.
     *
     * @param in          input stream
     * @param dialectMask active dialect bitmask
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if syntax is malformed
     */
    public ParsedDialectResult parse(InputStream in, int dialectMask) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a TSRG v2 character stream using default SRG dialect bitmask.
     *
     * @param reader character stream
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if syntax is malformed
     */
    public ParsedDialectResult parse(Reader reader) throws IOException {
        return parse(reader, VstmDialectMask.SRG);
    }

    /**
     * Parses a TSRG v2 character stream with an explicit dialect bitmask.
     *
     * @param reader      character stream
     * @param dialectMask active dialect bitmask
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if syntax is malformed
     */
    public ParsedDialectResult parse(Reader reader, int dialectMask) throws IOException {
        Objects.requireNonNull(reader, "reader must not be null");

        BufferedReader bufferedReader = (reader instanceof BufferedReader br) ? br : new BufferedReader(reader);
        String headerLine = bufferedReader.readLine();
        if (headerLine == null || !headerLine.trim().startsWith(TSRG2_HEADER_PREFIX)) {
            throw new UpstreamParserException(DIALECT_NAME, 1, headerLine, "Missing or invalid TSRG v2 header prefix");
        }

        List<ParsedClassMapping> classes = new ArrayList<>();
        List<ParsedFieldMapping> fields = new ArrayList<>();
        List<ParsedMethodMapping> methods = new ArrayList<>();

        String currentSourceClass = null;
        String line;
        int lineNumber = 1;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }

            // Top-level class line (0 indentation)
            if (line.charAt(0) != '\t' && line.charAt(0) != ' ') {
                String[] tokens = line.trim().split("\\s+");
                if (tokens.length >= 2) {
                    String sourceClass = tokens[0].trim();
                    String targetClass = tokens[1].trim();

                    currentSourceClass = sourceClass.replace('.', '/');
                    classes.add(new ParsedClassMapping(sourceClass, targetClass, dialectMask));
                }
                continue;
            }

            // Single tab indentation: member definitions
            if (line.startsWith("\t") && !line.startsWith("\t\t")) {
                if (currentSourceClass == null) {
                    throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Member mapping declared outside of class scope");
                }

                String memberContent = line.substring(1).trim();
                if (memberContent.isEmpty() || memberContent.startsWith("#")) {
                    continue;
                }

                String[] tokens = memberContent.split("\\s+");
                if (tokens.length >= 2) {
                    // Check if second token is method descriptor
                    if (tokens[1].startsWith("(")) {
                        // Method: <obfMethod> <desc> <srgMethod> [id]
                        String sourceMethod = tokens[0].trim();
                        String descriptor = tokens[1].trim();
                        String targetMethod = tokens.length > 2 ? tokens[2].trim() : sourceMethod;

                        methods.add(new ParsedMethodMapping(
                                currentSourceClass,
                                sourceMethod,
                                descriptor,
                                targetMethod,
                                descriptor,
                                dialectMask
                        ));
                    } else {
                        // Field: <obfField> <srgField> [id]
                        String sourceField = tokens[0].trim();
                        String targetField = tokens[1].trim();

                        fields.add(new ParsedFieldMapping(
                                currentSourceClass,
                                sourceField,
                                "",
                                targetField,
                                dialectMask
                        ));
                    }
                }
            }
            // Double tab or higher indentation represents parameter mappings / static annotations, skipped safely
        }

        return new ParsedDialectResult(classes, fields, methods, dialectMask);
    }
}
