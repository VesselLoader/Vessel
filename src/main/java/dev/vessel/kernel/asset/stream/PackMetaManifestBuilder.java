package dev.vessel.kernel.asset.stream;

import java.util.Objects;

/**
 * Manifest generator for pack.mcmeta supporting multi-version format ranges.
 * Extinguishes AST-MOD.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PackMetaManifestBuilder {

    /**
     * Builds standard pack.mcmeta JSON content.
     *
     * @param targetFormat pack format version enum
     * @param description pack description string
     * @return formatted JSON string for pack.mcmeta
     */
    public String buildPackMetaJson(TargetPackFormat targetFormat, String description) {
        Objects.requireNonNull(targetFormat, "targetFormat cannot be null");
        Objects.requireNonNull(description, "description cannot be null");

        int format = targetFormat.getFormatNumber();
        String escapedDesc = description.replace("\\", "\\\\").replace("\"", "\\\"");

        StringBuilder sb = new StringBuilder(256);
        sb.append("{\n");
        sb.append("  \"pack\": {\n");
        sb.append("    \"pack_format\": ").append(format).append(",\n");
        sb.append("    \"description\": \"").append(escapedDesc).append("\",\n");
        sb.append("    \"supported_formats\": {\n");
        sb.append("      \"min_inclusive\": 15,\n");
        sb.append("      \"max_inclusive\": 34\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }
}
