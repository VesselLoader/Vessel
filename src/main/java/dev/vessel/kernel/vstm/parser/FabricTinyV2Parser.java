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
 * High-performance streaming parser for Fabric Tiny v2 format mapping files.
 *
 * <p>Ingests Yarn mappings (e.g., {@code yarn-mergedv2.tiny}) and Fabric Intermediary
 * definitions (e.g., {@code intermediary-1.21.1.tiny}) across configurable source/target namespaces.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class FabricTinyV2Parser {

    private static final String TINY_V2_PREFIX = "tiny\t2\t0\t";
    private static final String DIALECT_NAME = "Fabric-TinyV2";

    /**
     * Parses a Tiny v2 mapping file from disk using default YARN dialect mask.
     *
     * @param path path to .tiny file
     * @return parsed dialect result
     * @throws IOException             if file reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(Path path) throws IOException {
        return parse(path, VstmDialectMask.YARN);
    }

    /**
     * Parses a Tiny v2 character stream using default YARN dialect mask.
     *
     * @param reader character stream
     * @return parsed dialect result
     * @throws IOException             if stream reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(Reader reader) throws IOException {
        return parse(reader, VstmDialectMask.YARN);
    }

    /**
     * Parses a Tiny v2 mapping file from disk using automatic namespace resolution based on dialect mask.
     *
     * @param path        path to .tiny file
     * @param dialectMask active dialect validity bitmask (e.g. YARN or INTERMEDIARY)
     * @return parsed dialect result
     * @throws IOException             if file reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(Path path, int dialectMask) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a Tiny v2 input stream with automatic namespace resolution.
     *
     * @param in          input stream
     * @param dialectMask active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(InputStream in, int dialectMask) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a Tiny v2 character stream with automatic namespace resolution.
     *
     * @param reader      character stream
     * @param dialectMask active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if stream reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(Reader reader, int dialectMask) throws IOException {
        return parse(reader, null, null, dialectMask);
    }

    /**
     * Parses a Tiny v2 file with explicitly designated source and target namespaces.
     *
     * @param path            path to file
     * @param sourceNamespace source namespace (e.g. "intermediary" or "official")
     * @param targetNamespace target namespace (e.g. "named" or "intermediary")
     * @param dialectMask     active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if file reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(
            Path path,
            String sourceNamespace,
            String targetNamespace,
            int dialectMask
    ) throws IOException {
        Objects.requireNonNull(path, "path must not be null");
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader, sourceNamespace, targetNamespace, dialectMask);
        }
    }

    /**
     * Parses a Tiny v2 stream with explicit source and target namespaces.
     *
     * @param reader          character stream
     * @param sourceNamespace source namespace name, or null for automatic detection
     * @param targetNamespace target namespace name, or null for automatic detection
     * @param dialectMask     active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if stream reading fails
     * @throws UpstreamParserException if syntax is invalid
     */
    public ParsedDialectResult parse(
            Reader reader,
            String sourceNamespace,
            String targetNamespace,
            int dialectMask
    ) throws IOException {
        Objects.requireNonNull(reader, "reader must not be null");

        BufferedReader bufferedReader = (reader instanceof BufferedReader br) ? br : new BufferedReader(reader);
        String headerLine = bufferedReader.readLine();
        if (headerLine == null || !headerLine.startsWith(TINY_V2_PREFIX)) {
            throw new UpstreamParserException(DIALECT_NAME, 1, headerLine, "Missing or invalid Tiny v2 header prefix");
        }

        String[] headerTokens = headerLine.substring(TINY_V2_PREFIX.length()).split("\t");
        List<String> namespaces = new ArrayList<>();
        for (String ns : headerTokens) {
            String trimmed = ns.trim();
            if (!trimmed.isEmpty()) {
                namespaces.add(trimmed);
            }
        }

        if (namespaces.size() < 2) {
            throw new UpstreamParserException(DIALECT_NAME, 1, headerLine, "Tiny v2 requires at least 2 namespaces, found: " + namespaces.size());
        }

        int sourceCol = resolveNamespaceIndex(namespaces, sourceNamespace, dialectMask, true);
        int targetCol = resolveNamespaceIndex(namespaces, targetNamespace, dialectMask, false);

        List<ParsedClassMapping> classes = new ArrayList<>();
        List<ParsedFieldMapping> fields = new ArrayList<>();
        List<ParsedMethodMapping> methods = new ArrayList<>();

        String currentSourceClass = null;
        String line;
        int lineNumber = 1;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }

            // Top-level class line: "c\t<name0>\t<name1>..."
            if (line.startsWith("c\t")) {
                String[] parts = line.split("\t", -1);
                int neededCol = Math.max(sourceCol, targetCol) + 1;
                if (parts.length <= neededCol) {
                    continue;
                }

                String src = parts[1 + sourceCol].trim();
                String tgt = parts[1 + targetCol].trim();

                if (tgt.isEmpty()) {
                    tgt = src;
                }

                if (!src.isEmpty() && !tgt.isEmpty()) {
                    currentSourceClass = src.replace('.', '/');
                    classes.add(new ParsedClassMapping(src, tgt, dialectMask));
                }
                continue;
            }

            // Member lines with 1 tab
            if (line.startsWith("\t") && !line.startsWith("\t\t")) {
                String memberContent = line.substring(1);

                if (memberContent.startsWith("f\t")) {
                    // Field line: "f\t<desc>\t<name0>\t<name1>..."
                    if (currentSourceClass == null) {
                        throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Field declared without class context");
                    }

                    String[] parts = memberContent.split("\t", -1);
                    int neededCol = Math.max(sourceCol, targetCol) + 2;
                    if (parts.length <= neededCol) {
                        continue;
                    }

                    String descriptor = parts[1].trim();
                    String srcField = parts[2 + sourceCol].trim();
                    String tgtField = parts[2 + targetCol].trim();

                    if (tgtField.isEmpty()) {
                        tgtField = srcField;
                    }

                    if (!srcField.isEmpty()) {
                        fields.add(new ParsedFieldMapping(currentSourceClass, srcField, descriptor, tgtField, dialectMask));
                    }
                } else if (memberContent.startsWith("m\t")) {
                    // Method line: "m\t<desc>\t<name0>\t<name1>..."
                    if (currentSourceClass == null) {
                        throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Method declared without class context");
                    }

                    String[] parts = memberContent.split("\t", -1);
                    int neededCol = Math.max(sourceCol, targetCol) + 2;
                    if (parts.length <= neededCol) {
                        continue;
                    }

                    String descriptor = parts[1].trim();
                    String srcMethod = parts[2 + sourceCol].trim();
                    String tgtMethod = parts[2 + targetCol].trim();

                    if (tgtMethod.isEmpty()) {
                        tgtMethod = srcMethod;
                    }

                    if (!srcMethod.isEmpty()) {
                        methods.add(new ParsedMethodMapping(
                                currentSourceClass,
                                srcMethod,
                                descriptor,
                                tgtMethod,
                                descriptor,
                                dialectMask
                        ));
                    }
                }
                // Comment lines ("c\t...") inside members are ignored
            }
            // 2-tab lines ("\t\tp\t...", "\t\tc\t...") are sub-parameters or comments, safely ignored
        }

        return new ParsedDialectResult(classes, fields, methods, dialectMask);
    }

    private static int resolveNamespaceIndex(
            List<String> namespaces,
            String requestedNamespace,
            int dialectMask,
            boolean isSource
    ) {
        if (requestedNamespace != null && !requestedNamespace.isBlank()) {
            for (int i = 0; i < namespaces.size(); i++) {
                if (namespaces.get(i).equalsIgnoreCase(requestedNamespace.trim())) {
                    return i;
                }
            }
            throw new UpstreamParserException(DIALECT_NAME, 1, "", "Requested namespace '" + requestedNamespace + "' not found in " + namespaces);
        }

        // Automatic inference by dialect bitmask
        if (VstmDialectMask.hasDialect(dialectMask, VstmDialectMask.YARN)) {
            if (isSource) {
                int intermediaryIdx = findIndex(namespaces, "intermediary");
                if (intermediaryIdx != -1) return intermediaryIdx;
                int officialIdx = findIndex(namespaces, "official");
                return officialIdx != -1 ? officialIdx : 0;
            } else {
                int namedIdx = findIndex(namespaces, "named");
                if (namedIdx != -1) return namedIdx;
                return namespaces.size() - 1;
            }
        }

        if (VstmDialectMask.hasDialect(dialectMask, VstmDialectMask.INTERMEDIARY)) {
            if (isSource) {
                int officialIdx = findIndex(namespaces, "official");
                return officialIdx != -1 ? officialIdx : 0;
            } else {
                int intermediaryIdx = findIndex(namespaces, "intermediary");
                if (intermediaryIdx != -1) return intermediaryIdx;
                return namespaces.size() - 1;
            }
        }

        // Default: 0 for source, last for target
        return isSource ? 0 : namespaces.size() - 1;
    }

    private static int findIndex(List<String> list, String target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equalsIgnoreCase(target)) {
                return i;
            }
        }
        return -1;
    }
}
