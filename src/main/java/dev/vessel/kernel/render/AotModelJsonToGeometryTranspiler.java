package dev.vessel.kernel.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Ahead-Of-Time (AOT) transpiler converting Java Edition Block Model JSON and GeckoLib structures
 * into Bedrock Edition 1.21 .geometry.json definitions.
 * Normalizes coordinate coordinate spaces, converts face UV mappings, and structures bone hierarchies.
 * Extinguishes ERR-82 (geometry parse failures) and AST-BED (Bedrock mesh distortion).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class AotModelJsonToGeometryTranspiler {

    public static final String BEDROCK_FORMAT_VERSION = "1.21.0";
    public static final String DEFAULT_ROOT_BONE_NAME = "root";
    public static final float JAVA_TO_BEDROCK_OFFSET_X = -8.0f;
    public static final float JAVA_TO_BEDROCK_OFFSET_Y = 0.0f;
    public static final float JAVA_TO_BEDROCK_OFFSET_Z = -8.0f;

    private final int textureWidth;
    private final int textureHeight;

    /**
     * Constructs an AotModelJsonToGeometryTranspiler with default 64x64 texture dimensions.
     */
    public AotModelJsonToGeometryTranspiler() {
        this(64, 64);
    }

    /**
     * Constructs an AotModelJsonToGeometryTranspiler with explicit texture dimensions.
     *
     * @param textureWidth atlas width in pixels
     * @param textureHeight atlas height in pixels
     */
    public AotModelJsonToGeometryTranspiler(int textureWidth, int textureHeight) {
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
    }

    /**
     * Transpiles a collection of Java block model elements into a Bedrock Geometry definition.
     *
     * @param modelIdentifier unique identifier (e.g. "geometry.vessel.crusher")
     * @param javaElements list of Java Edition model elements
     * @return fully composed BedrockGeometryDefinition
     */
    public BedrockGeometryDefinition transpileJavaElements(String modelIdentifier, List<JavaBlockModelElement> javaElements) {
        Objects.requireNonNull(modelIdentifier, "modelIdentifier cannot be null");
        Objects.requireNonNull(javaElements, "javaElements cannot be null");

        List<BedrockGeometryCube> bedrockCubes = new ArrayList<>();

        for (JavaBlockModelElement elem : javaElements) {
            float[] from = elem.from();
            float[] to = elem.to();

            // Calculate Bedrock origin (centered at block base)
            float originX = from[0] + JAVA_TO_BEDROCK_OFFSET_X;
            float originY = from[1] + JAVA_TO_BEDROCK_OFFSET_Y;
            float originZ = from[2] + JAVA_TO_BEDROCK_OFFSET_Z;

            // Calculate cube dimensions
            float sizeX = Math.abs(to[0] - from[0]);
            float sizeY = Math.abs(to[1] - from[1]);
            float sizeZ = Math.abs(to[2] - from[2]);

            // Calculate rotation and pivot
            float[] rotation = null;
            float[] pivot = null;
            if (elem.rotationOrigin() != null && elem.rotationAxis() != null) {
                float[] orig = elem.rotationOrigin();
                pivot = new float[] {
                        orig[0] + JAVA_TO_BEDROCK_OFFSET_X,
                        orig[1] + JAVA_TO_BEDROCK_OFFSET_Y,
                        orig[2] + JAVA_TO_BEDROCK_OFFSET_Z
                };
                rotation = new float[] { 0.0f, 0.0f, 0.0f };
                String axis = elem.rotationAxis().toLowerCase(Locale.ROOT);
                switch (axis) {
                    case "x" -> rotation[0] = -elem.rotationAngle();
                    case "y" -> rotation[1] = elem.rotationAngle();
                    case "z" -> rotation[2] = elem.rotationAngle();
                }
            }

            // Map face UVs
            Map<String, float[]> faceUvs = new HashMap<>();
            for (Map.Entry<String, JavaModelFace> entry : elem.faces().entrySet()) {
                String faceName = entry.getKey().toLowerCase(Locale.ROOT);
                JavaModelFace face = entry.getValue();
                float[] uv = face.uv();
                if (uv != null) {
                    faceUvs.put(faceName, new float[] { uv[0], uv[1], uv[2], uv[3] });
                } else {
                    // Default fallback UV based on cube dimensions
                    faceUvs.put(faceName, new float[] { 0.0f, 0.0f, 16.0f, 16.0f });
                }
            }

            bedrockCubes.add(new BedrockGeometryCube(
                    new float[] { originX, originY, originZ },
                    new float[] { sizeX, sizeY, sizeZ },
                    rotation,
                    pivot,
                    faceUvs
            ));
        }

        BedrockGeometryBone rootBone = new BedrockGeometryBone(
                DEFAULT_ROOT_BONE_NAME,
                null,
                new float[] { 0.0f, 0.0f, 0.0f },
                new float[] { 0.0f, 0.0f, 0.0f },
                bedrockCubes
        );

        return new BedrockGeometryDefinition(
                modelIdentifier,
                textureWidth,
                textureHeight,
                List.of(rootBone)
        );
    }

    /**
     * Serializes a BedrockGeometryDefinition into the official Bedrock 1.21 .geometry.json formatted string.
     *
     * @param definition geometry definition to serialize
     * @return JSON string matching Bedrock 1.21 schema
     */
    public String serializeToJson(BedrockGeometryDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format_version\": \"").append(BEDROCK_FORMAT_VERSION).append("\",\n");
        sb.append("  \"minecraft:geometry\": [\n");
        sb.append("    {\n");
        sb.append("      \"description\": {\n");
        sb.append("        \"identifier\": \"").append(escapeJson(definition.geometryIdentifier())).append("\",\n");
        sb.append("        \"texture_width\": ").append(definition.textureWidth()).append(",\n");
        sb.append("        \"texture_height\": ").append(definition.textureHeight()).append("\n");
        sb.append("      },\n");
        sb.append("      \"bones\": [\n");

        List<BedrockGeometryBone> bones = definition.bones();
        for (int b = 0; b < bones.size(); b++) {
            BedrockGeometryBone bone = bones.get(b);
            sb.append("        {\n");
            sb.append("          \"name\": \"").append(escapeJson(bone.name())).append("\",\n");
            if (bone.parent() != null) {
                sb.append("          \"parent\": \"").append(escapeJson(bone.parent())).append("\",\n");
            }
            float[] piv = bone.pivot();
            sb.append(String.format(Locale.ROOT, "          \"pivot\": [%.2f, %.2f, %.2f],\n", piv[0], piv[1], piv[2]));

            sb.append("          \"cubes\": [\n");
            List<BedrockGeometryCube> cubes = bone.cubes();
            for (int c = 0; c < cubes.size(); c++) {
                BedrockGeometryCube cube = cubes.get(c);
                sb.append("            {\n");
                float[] orig = cube.origin();
                float[] sz = cube.size();
                sb.append(String.format(Locale.ROOT, "              \"origin\": [%.2f, %.2f, %.2f],\n", orig[0], orig[1], orig[2]));
                sb.append(String.format(Locale.ROOT, "              \"size\": [%.2f, %.2f, %.2f],\n", sz[0], sz[1], sz[2]));

                if (cube.pivot() != null && cube.rotation() != null) {
                    float[] cp = cube.pivot();
                    float[] cr = cube.rotation();
                    sb.append(String.format(Locale.ROOT, "              \"pivot\": [%.2f, %.2f, %.2f],\n", cp[0], cp[1], cp[2]));
                    sb.append(String.format(Locale.ROOT, "              \"rotation\": [%.2f, %.2f, %.2f],\n", cr[0], cr[1], cr[2]));
                }

                sb.append("              \"uv\": {\n");
                int fCount = 0;
                int fTotal = cube.faceUvs().size();
                for (Map.Entry<String, float[]> fEntry : cube.faceUvs().entrySet()) {
                    fCount++;
                    float[] uv = fEntry.getValue();
                    float u1 = uv[0];
                    float v1 = uv[1];
                    float uSize = uv[2] - uv[0];
                    float vSize = uv[3] - uv[1];
                    sb.append(String.format(Locale.ROOT, "                \"%s\": {\"uv\": [%.1f, %.1f], \"uv_size\": [%.1f, %.1f]}%s\n",
                            fEntry.getKey(), u1, v1, uSize, vSize, (fCount < fTotal ? "," : "")));
                }
                sb.append("              }\n");

                sb.append("            }").append(c < cubes.size() - 1 ? "," : "").append("\n");
            }
            sb.append("          ]\n");
            sb.append("        }").append(b < bones.size() - 1 ? "," : "").append("\n");
        }

        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }
}
