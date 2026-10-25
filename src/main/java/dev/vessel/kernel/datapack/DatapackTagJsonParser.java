package dev.vessel.kernel.datapack;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust clean-room JSON parser for Minecraft datapack tag definition files.
 * <p>
 * Accurately extracts {@code replace} directives and parses both simple string entries
 * (e.g., {@code "minecraft:iron_ingot"}, {@code "#c:iron_ores"}) and advanced tag entry objects
 * (e.g., {@code {"id": "c:iron_ingots", "required": false}}) without misinterpreting JSON keys
 * like {@code "id"} or {@code "required"} as tag member items.
 * <p>
 * Strips comments and extraneous formatting in accordance with JSON5 / modded data conventions.
 * Strictly isolated in its own compilation unit in accordance with Rule 17.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DatapackTagJsonParser {

    private static final Pattern REPLACE_PATTERN = Pattern.compile(
            "\"replace\"\\s*:\\s*(true|false)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern VALUES_ARRAY_PATTERN = Pattern.compile(
            "\"values\"\\s*:\\s*\\[([^\\]]*)\\]",
            Pattern.DOTALL);

    private static final Pattern OBJECT_ID_PATTERN = Pattern.compile(
            "\"id\"\\s*:\\s*\"([^\"]+)\"");

    private DatapackTagJsonParser() {
        // Utility class
    }

    /**
     * Parses raw JSON text into a {@link DatapackTagDefinition}.
     *
     * @param jsonText raw JSON content
     * @return parsed tag definition
     */
    public static DatapackTagDefinition parseTag(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            return new DatapackTagDefinition();
        }

        String cleaned = stripComments(jsonText);

        boolean replace = false;
        Matcher replaceMatcher = REPLACE_PATTERN.matcher(cleaned);
        if (replaceMatcher.find()) {
            replace = Boolean.parseBoolean(replaceMatcher.group(1));
        }

        DatapackTagDefinition definition = new DatapackTagDefinition(replace);

        Matcher arrayMatcher = VALUES_ARRAY_PATTERN.matcher(cleaned);
        if (arrayMatcher.find()) {
            String arrayBody = arrayMatcher.group(1).trim();
            parseValuesArray(arrayBody, definition);
        }

        return definition;
    }

    /**
     * Parses UTF-8 encoded byte content into a {@link DatapackTagDefinition}.
     *
     * @param content raw byte array
     * @return parsed tag definition
     */
    public static DatapackTagDefinition parseTag(byte[] content) {
        Objects.requireNonNull(content, "content cannot be null");
        return parseTag(new String(content, StandardCharsets.UTF_8));
    }

    private static void parseValuesArray(String body, DatapackTagDefinition target) {
        if (body.isEmpty()) {
            return;
        }

        // Tokenize items inside the array body by scanning brackets and quotes
        int len = body.length();
        int i = 0;
        while (i < len) {
            char c = body.charAt(i);

            // Skip whitespace and commas
            if (Character.isWhitespace(c) || c == ',') {
                i++;
                continue;
            }

            // Case 1: String literal "namespace:name"
            if (c == '"') {
                int start = i + 1;
                int end = start;
                while (end < len && body.charAt(end) != '"') {
                    if (body.charAt(end) == '\\' && end + 1 < len) {
                        end += 2;
                    } else {
                        end++;
                    }
                }
                if (end < len) {
                    String strVal = body.substring(start, end).trim();
                    target.addValue(strVal);
                    i = end + 1;
                } else {
                    break;
                }
            } else if (c == '{') {
                // Case 2: Object entry: {"id": "namespace:name", "required": false}
                int objStart = i;
                int depth = 0;
                int objEnd = i;
                while (objEnd < len) {
                    char ch = body.charAt(objEnd);
                    if (ch == '{') {
                        depth++;
                    } else if (ch == '}') {
                        depth--;
                        if (depth == 0) {
                            objEnd++;
                            break;
                        }
                    }
                    objEnd++;
                }

                String objContent = body.substring(objStart, Math.min(objEnd, len));
                Matcher idMatcher = OBJECT_ID_PATTERN.matcher(objContent);
                if (idMatcher.find()) {
                    String id = idMatcher.group(1).trim();
                    target.addValue(id);
                }
                i = objEnd;
            } else {
                i++;
            }
        }
    }

    private static String stripComments(String input) {
        // Strip block comments /* ... */
        String withoutBlock = input.replaceAll("/\\*.*?\\*/", "");
        // Strip line comments // ...
        return withoutBlock.replaceAll("//.*", "");
    }
}
