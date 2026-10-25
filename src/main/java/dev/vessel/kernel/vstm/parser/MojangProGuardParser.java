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
 * High-performance streaming parser for official Mojang ProGuard format mapping files.
 *
 * <p>Ingests both {@code client_mappings.txt} and {@code server_mappings.txt}, mapping
 * obfuscated bytecode identifiers back into canonical MojMap identifiers without intermediary allocation overhead.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class MojangProGuardParser {

    private static final String ARROW_SEPARATOR = " -> ";
    private static final String COMMENT_PREFIX = "#";
    private static final String DIALECT_NAME = "MojMap-ProGuard";

    /**
     * Parses a Mojang ProGuard mapping stream using the default MOJMAP dialect mask.
     *
     * @param reader mapping character stream
     * @return parsed dialect result
     * @throws IOException             if reading the stream fails
     * @throws UpstreamParserException if mapping file syntax is malformed
     */
    public ParsedDialectResult parse(Reader reader) throws IOException {
        return parse(reader, VstmDialectMask.MOJMAP);
    }

    /**
     * Parses a Mojang ProGuard mapping file from disk using the default MOJMAP dialect mask.
     *
     * @param path path to ProGuard mapping file
     * @return parsed dialect result
     * @throws IOException             if file reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(Path path) throws IOException {
        return parse(path, VstmDialectMask.MOJMAP);
    }

    /**
     * Parses a Mojang ProGuard mapping file from disk with an explicit dialect mask.
     *
     * @param path        path to ProGuard mapping file
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
     * Parses a Mojang ProGuard input stream with an explicit dialect mask.
     *
     * @param in          input stream of mapping definitions
     * @param dialectMask active dialect validity bitmask
     * @return parsed dialect result
     * @throws IOException             if reading fails
     * @throws UpstreamParserException if mapping syntax is malformed
     */
    public ParsedDialectResult parse(InputStream in, int dialectMask) throws IOException {
        Objects.requireNonNull(in, "in must not be null");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parse(reader, dialectMask);
        }
    }

    /**
     * Parses a character stream containing ProGuard format mappings into intermediate parsed records.
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

        String currentSourceClass = null;
        String line;
        int lineNumber = 0;

        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }

            char firstChar = line.charAt(0);
            if (firstChar == '#') {
                continue;
            }

            // Class mapping: non-indented line
            if (firstChar != ' ' && firstChar != '\t') {
                String classLine = line.trim();
                if (classLine.endsWith(":")) {
                    classLine = classLine.substring(0, classLine.length() - 1);
                }

                int arrowIdx = classLine.indexOf(ARROW_SEPARATOR);
                if (arrowIdx == -1) {
                    throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Missing ' -> ' in class definition");
                }

                String targetClass = classLine.substring(0, arrowIdx).trim();
                String sourceClass = classLine.substring(arrowIdx + ARROW_SEPARATOR.length()).trim();

                currentSourceClass = sourceClass.replace('.', '/');
                classes.add(new ParsedClassMapping(sourceClass, targetClass, dialectMask));
                continue;
            }

            // Member mapping: indented line
            String memberLine = line.trim();
            if (memberLine.startsWith(COMMENT_PREFIX) || memberLine.isEmpty()) {
                continue;
            }

            if (currentSourceClass == null) {
                throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Member mapping declared outside of class scope");
            }

            int arrowIdx = memberLine.indexOf(ARROW_SEPARATOR);
            if (arrowIdx == -1) {
                throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Missing ' -> ' in member definition");
            }

            String sourceMemberName = memberLine.substring(arrowIdx + ARROW_SEPARATOR.length()).trim();
            String leftPart = memberLine.substring(0, arrowIdx).trim();

            // Strip line number prefix (e.g. "12:13:void foo()")
            leftPart = stripLineNumbers(leftPart);

            int parenOpen = leftPart.indexOf('(');
            if (parenOpen != -1) {
                // Method mapping
                int parenClose = leftPart.indexOf(')', parenOpen);
                if (parenClose == -1) {
                    throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Unclosed parameter parenthesis in method declaration");
                }

                String params = leftPart.substring(parenOpen + 1, parenClose);
                String beforeParen = leftPart.substring(0, parenOpen).trim();
                int lastSpace = beforeParen.lastIndexOf(' ');
                if (lastSpace == -1) {
                    throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Missing return type in method declaration");
                }

                String returnType = beforeParen.substring(0, lastSpace).trim();
                String targetMethodName = beforeParen.substring(lastSpace + 1).trim();
                String descriptor = ProGuardDescriptorNormalizer.toMethodDescriptor(returnType, params);

                methods.add(new ParsedMethodMapping(
                        currentSourceClass,
                        sourceMemberName,
                        descriptor,
                        targetMethodName,
                        descriptor,
                        dialectMask
                ));
            } else {
                // Field mapping
                int lastSpace = leftPart.lastIndexOf(' ');
                if (lastSpace == -1) {
                    throw new UpstreamParserException(DIALECT_NAME, lineNumber, line, "Malformed field declaration");
                }

                String fieldType = leftPart.substring(0, lastSpace).trim();
                String targetFieldName = leftPart.substring(lastSpace + 1).trim();
                String descriptor = ProGuardDescriptorNormalizer.toJvmType(fieldType);

                fields.add(new ParsedFieldMapping(
                        currentSourceClass,
                        sourceMemberName,
                        descriptor,
                        targetFieldName,
                        dialectMask
                ));
            }
        }

        return new ParsedDialectResult(classes, fields, methods, dialectMask);
    }

    private static String stripLineNumbers(String signature) {
        if (signature.isEmpty() || !Character.isDigit(signature.charAt(0))) {
            return signature;
        }

        int firstColon = signature.indexOf(':');
        if (firstColon == -1) {
            return signature;
        }

        int secondColon = signature.indexOf(':', firstColon + 1);
        if (secondColon != -1) {
            return signature.substring(secondColon + 1).trim();
        }

        return signature.substring(firstColon + 1).trim();
    }
}
