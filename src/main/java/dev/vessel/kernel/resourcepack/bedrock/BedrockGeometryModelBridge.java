package dev.vessel.kernel.resourcepack.bedrock;

import dev.vessel.kernel.display.model.CompactModelMeshDefinition;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Bridge transforming compact 3D model meshes ({@link CompactModelMeshDefinition})
 * into Bedrock Model Format {@code geometry.json} (schema 1.12.0 / 1.16.0).
 * <p>
 * Calculates spatial bounding boxes, visible boundary extents, pivots, and bone cube definitions,
 * enabling seamless custom model rendering on Bedrock Edition clients with low overhead.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class BedrockGeometryModelBridge {

    /** Default Bedrock geometry schema version 1.16.0. */
    public static final String SCHEMA_1_16_0 = "1.16.0";

    /** Legacy Bedrock geometry schema version 1.12.0. */
    public static final String SCHEMA_1_12_0 = "1.12.0";

    /** Default texture resolution width for Bedrock models. */
    public static final int DEFAULT_TEXTURE_WIDTH = 64;

    /** Default texture resolution height for Bedrock models. */
    public static final int DEFAULT_TEXTURE_HEIGHT = 64;

    /**
     * Sanitizes a Java model ID into a valid Bedrock geometry identifier.
     * E.g. "mymod:block/pedestal" &rarr; "geometry.mymod.block.pedestal".
     *
     * @param modelId original model identifier
     * @return sanitized Bedrock geometry identifier
     * @throws IllegalArgumentException if modelId is blank or contains no valid characters
     */
    public String sanitizeIdentifier(String modelId) {
        Objects.requireNonNull(modelId, "modelId cannot be null");
        if (modelId.trim().isEmpty()) {
            throw new IllegalArgumentException("modelId cannot be empty or blank");
        }
        String sanitized = modelId.trim()
                .toLowerCase(Locale.ROOT)
                .replace(':', '.')
                .replace('/', '.')
                .replaceAll("[^a-z0-9._-]", "_");

        while (sanitized.startsWith(".")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("modelId does not contain valid identifier characters: " + modelId);
        }
        if (!sanitized.startsWith("geometry.")) {
            sanitized = "geometry." + sanitized;
        }
        return sanitized;
    }

    /**
     * Converts a single {@link CompactModelMeshDefinition} into a Bedrock geometry JSON string using default settings.
     *
     * @param mesh input model mesh definition
     * @return formatted Bedrock geometry JSON string
     */
    public String convertMeshToGeometryJson(CompactModelMeshDefinition mesh) {
        return convertMeshToGeometryJson(mesh, DEFAULT_TEXTURE_WIDTH, DEFAULT_TEXTURE_HEIGHT, SCHEMA_1_16_0);
    }

    /**
     * Converts a single {@link CompactModelMeshDefinition} into a Bedrock geometry JSON string.
     *
     * @param mesh input model mesh definition
     * @param textureWidth texture width in pixels (e.g. 64)
     * @param textureHeight texture height in pixels (e.g. 64)
     * @param schemaVersion Bedrock geometry schema version ("1.12.0" or "1.16.0")
     * @return formatted Bedrock geometry JSON string
     */
    public String convertMeshToGeometryJson(
            CompactModelMeshDefinition mesh,
            int textureWidth,
            int textureHeight,
            String schemaVersion
    ) {
        Objects.requireNonNull(mesh, "mesh cannot be null");
        return convertMeshesToGeometryJson(List.of(mesh), textureWidth, textureHeight, schemaVersion);
    }

    /**
     * Converts multiple model meshes into a unified Bedrock {@code geometry.json} specification.
     *
     * @param meshes list of mesh definitions
     * @param textureWidth texture width in pixels
     * @param textureHeight texture height in pixels
     * @param schemaVersion Bedrock geometry schema version
     * @return formatted Bedrock geometry JSON string
     */
    public String convertMeshesToGeometryJson(
            List<CompactModelMeshDefinition> meshes,
            int textureWidth,
            int textureHeight,
            String schemaVersion
    ) {
        Objects.requireNonNull(meshes, "meshes cannot be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion cannot be null");
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive: " + textureWidth + "x" + textureHeight);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format_version\": \"").append(schemaVersion).append("\",\n");
        sb.append("  \"minecraft:geometry\": [\n");

        for (int m = 0; m < meshes.size(); m++) {
            CompactModelMeshDefinition mesh = meshes.get(m);
            float[] bbox = mesh.computeBoundingBox();
            float minX = bbox[0];
            float minY = bbox[1];
            float minZ = bbox[2];
            float maxX = bbox[3];
            float maxY = bbox[4];
            float maxZ = bbox[5];

            float sizeX = Math.max(0.001f, maxX - minX);
            float sizeY = Math.max(0.001f, maxY - minY);
            float sizeZ = Math.max(0.001f, maxZ - minZ);

            float visibleWidth = (float) Math.ceil(Math.max(sizeX, sizeZ) / 16.0f);
            float visibleHeight = (float) Math.ceil(sizeY / 16.0f);
            float visibleOffsetY = (minY + sizeY / 2.0f) / 16.0f;

            // UV coordinates from first vertex if available
            float[] uvs = mesh.getUvCoordinatesCopy();
            float u0 = (uvs.length >= 2) ? uvs[0] * textureWidth : 0.0f;
            float v0 = (uvs.length >= 2) ? uvs[1] * textureHeight : 0.0f;

            String identifier = sanitizeIdentifier(mesh.getModelId());

            sb.append("    {\n");
            sb.append("      \"description\": {\n");
            sb.append("        \"identifier\": \"").append(identifier).append("\",\n");
            sb.append("        \"texture_width\": ").append(textureWidth).append(",\n");
            sb.append("        \"texture_height\": ").append(textureHeight).append(",\n");
            sb.append(String.format(Locale.ROOT, "        \"visible_bounds_width\": %.2f,\n", visibleWidth));
            sb.append(String.format(Locale.ROOT, "        \"visible_bounds_height\": %.2f,\n", visibleHeight));
            sb.append(String.format(Locale.ROOT, "        \"visible_bounds_offset\": [0.0, %.2f, 0.0]\n", visibleOffsetY));
            sb.append("      },\n");
            sb.append("      \"bones\": [\n");
            sb.append("        {\n");
            sb.append("          \"name\": \"root\",\n");
            sb.append("          \"pivot\": [0.0, 0.0, 0.0],\n");
            sb.append("          \"cubes\": [\n");
            sb.append("            {\n");
            sb.append(String.format(Locale.ROOT, "              \"origin\": [%.2f, %.2f, %.2f],\n", minX, minY, minZ));
            sb.append(String.format(Locale.ROOT, "              \"size\": [%.2f, %.2f, %.2f],\n", sizeX, sizeY, sizeZ));
            sb.append(String.format(Locale.ROOT, "              \"uv\": [%.1f, %.1f]\n", u0, v0));
            sb.append("            }\n");
            sb.append("          ]\n");
            sb.append("        }\n");
            sb.append("      ]\n");
            sb.append("    }");

            if (m < meshes.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }
}
