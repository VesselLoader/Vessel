package dev.vessel.kernel.datapack;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Deterministic generator for {@code pack.mcmeta} datapack manifest files.
 * <p>
 * Supports multi-version Minecraft datapack formats (formats 15 through 48+),
 * formatting JSON payloads without external serialization dependencies.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DatapackMetadataGenerator {

    /**
     * Default datapack format for Minecraft 1.20.5 - 1.21.x ecosystems.
     */
    public static final int DEFAULT_PACK_FORMAT = 48;

    /**
     * Default datapack description header.
     */
    public static final String DEFAULT_DESCRIPTION = "VESSEL Universal Harmonized Datapack";

    /**
     * Generates a valid pack.mcmeta JSON string.
     *
     * @param packFormat  datapack format version integer (>= 1)
     * @param description human-readable pack description
     * @return formatted JSON text
     */
    public static String generateMetadataJson(int packFormat, String description) {
        if (packFormat < 1) {
            throw new IllegalArgumentException("packFormat must be >= 1: " + packFormat);
        }
        Objects.requireNonNull(description, "description cannot be null");

        String escapedDesc = escapeJsonString(description);
        return "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": " + packFormat + ",\n"
                + "    \"description\": \"" + escapedDesc + "\"\n"
                + "  }\n"
                + "}\n";
    }

    /**
     * Generates a valid pack.mcmeta as a UTF-8 encoded byte array.
     *
     * @param packFormat  datapack format version integer (>= 1)
     * @param description human-readable pack description
     * @return UTF-8 encoded JSON bytes
     */
    public static byte[] generateMetadataBytes(int packFormat, String description) {
        return generateMetadataJson(packFormat, description).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Escapes standard JSON special characters.
     */
    private static String escapeJsonString(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 8);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
