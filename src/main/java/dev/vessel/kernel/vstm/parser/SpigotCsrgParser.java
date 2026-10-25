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
 * High-performance streaming parser for Spigot / CraftBukkit CSRG format mapping files.
 *
 * <p>Ingests Spigot NMS class, field, and method deobfuscation maps (e.g., {@code bukkit-1.21.1-cl.csrg})
 * enabling hybrid Bukkit/Paper runtime compatibility (Catalog ref: TRN-0020).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class SpigotCsrgParser {

    private static final String DIALECT_NAME = "Spigot-CSRG";

    /**
     * Parses a CSRG mapping file from disk using the default SPIGOT_NMS dialect mask.
     *
     * @param path path to .csrg file
     * @return parsed dialect result
     * @throws IOException             if file reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(Path path) throws IOException {
        return parse(path, VstmDialectMask.SPIGOT_NMS);
    }

    /**
     * Parses a CSRG mapping file from disk with an explicit dialect mask.
     *
     * @param path        path to .csrg file
     * @param dialectMask active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if file reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(Path path, int dialectMask) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a CSRG input stream with an explicit dialect mask.
     *
     * @param in          input stream
     * @param dialectMask active dialect bitmask
     * @return parsed dialect result
     * @throws IOException             if stream reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(InputStream in, int dialectMask) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a CSRG character stream using default SPIGOT_NMS dialect mask.
     *
     * @param reader character stream
     * @return parsed dialect result
     * @throws IOException             if stream reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(Reader reader) throws IOException {
        return parse(reader, VstmDialectMask.SPIGOT_NMS);
    }

    /**
     * Parses a CSRG character stream with an explicit dialect mask.
     *
     * @param reader      character stream
     * @param dialectMask active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if stream reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(Reader reader, int dialectMask) throws IOException {
        Objects.requireNonNull(reader, "reader must not be null");

        BufferedReader bufferedReader = (reader instanceof BufferedReader br) ? br : new BufferedReader(reader);
        List<ParsedClassMapping> classes = new ArrayList<>();
        List<ParsedFieldMapping> fields = new ArrayList<>();
        List<ParsedMethodMapping> methods = new ArrayList<>();

        String line;
        int lineNumber = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] tokens = trimmed.split("\\s+");
            switch (tokens.length) {
                case 2 -> {
                    // Class mapping: <obfClass> <targetClass>
                    classes.add(new ParsedClassMapping(tokens[0], tokens[1], dialectMask));
                }
                case 3 -> {
                    // Field mapping: <ownerClass> <obfField> <targetField>
                    fields.add(new ParsedFieldMapping(tokens[0], tokens[1], "", tokens[2], dialectMask));
                }
                default -> {
                    // Method mapping: <ownerClass> <obfMethod> <descriptor> <targetMethod>
                    if (tokens.length >= 4) {
                        methods.add(new ParsedMethodMapping(
                                tokens[0],
                                tokens[1],
                                tokens[2],
                                tokens[3],
                                tokens[2],
                                dialectMask
                        ));
                    } else {
                        throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Incomplete CSRG mapping token entry");
                    }
                }
            }
        }

        return new ParsedDialectResult(classes, fields, methods, dialectMask);
    }
}
