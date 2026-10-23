package dev.vessel.kernel.modelengine.bedrock;

import dev.vessel.kernel.modelengine.bone.BoneHierarchyTree;
import dev.vessel.kernel.modelengine.bone.BoneNode;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Translates Java Model Engine skeletal bone hierarchies into GeyserMC Bedrock Edition
 * {@code geometry.json} format (schema version 1.16.0).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GeyserCustomGeometryTranslator {

    public static final String SCHEMA_VERSION_1_16_0 = "1.16.0";

    /**
     * Constructs a GeyserCustomGeometryTranslator.
     */
    public GeyserCustomGeometryTranslator() {
    }

    /**
     * Translates a bone hierarchy tree and model definition into Bedrock geometry JSON format.
     *
     * @param definition Bedrock model definition
     * @param tree       Java bone hierarchy tree
     * @return formatted Bedrock geometry JSON string
     */
    public String translateToBedrockGeometry(GeyserModelDefinition definition, BoneHierarchyTree tree) {
        Objects.requireNonNull(definition, "definition cannot be null");
        Objects.requireNonNull(tree, "tree cannot be null");

        Map<String, BoneNode> allBones = tree.getAllBones();

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format_version\": \"").append(SCHEMA_VERSION_1_16_0).append("\",\n");
        sb.append("  \"minecraft:geometry\": [\n");
        sb.append("    {\n");
        sb.append("      \"description\": {\n");
        sb.append("        \"identifier\": \"").append(definition.getBedrockIdentifier()).append("\",\n");
        sb.append("        \"texture_width\": ").append(definition.getTextureWidth()).append(",\n");
        sb.append("        \"texture_height\": ").append(definition.getTextureHeight()).append(",\n");
        sb.append("        \"visible_bounds_width\": 3.0,\n");
        sb.append("        \"visible_bounds_height\": 3.0,\n");
        sb.append("        \"visible_bounds_offset\": [0.0, 1.5, 0.0]\n");
        sb.append("      },\n");
        sb.append("      \"bones\": [\n");

        int index = 0;
        int total = allBones.size();

        for (BoneNode bone : allBones.values()) {
            sb.append("        {\n");
            sb.append("          \"name\": \"").append(bone.getBoneName()).append("\",\n");
            if (!bone.isRootBone()) {
                sb.append("          \"parent\": \"").append(bone.getParentBoneName()).append("\",\n");
            }

            float px = bone.getTranslationX() * 16.0f;
            float py = bone.getTranslationY() * 16.0f;
            float pz = bone.getTranslationZ() * 16.0f;
            sb.append(String.format(Locale.ROOT, "          \"pivot\": [%.2f, %.2f, %.2f],\n", px, py, pz));

            float degPitch = (float) Math.toDegrees(bone.getPitch());
            float degYaw = (float) Math.toDegrees(bone.getYaw());
            float degRoll = (float) Math.toDegrees(bone.getRoll());
            sb.append(String.format(Locale.ROOT, "          \"rotation\": [%.2f, %.2f, %.2f],\n", degPitch, degYaw, degRoll));

            // Default cube geometry representing the bone part
            float sx = Math.max(1.0f, bone.getScaleX() * 8.0f);
            float sy = Math.max(1.0f, bone.getScaleY() * 8.0f);
            float sz = Math.max(1.0f, bone.getScaleZ() * 8.0f);

            sb.append("          \"cubes\": [\n");
            sb.append("            {\n");
            sb.append(String.format(Locale.ROOT, "              \"origin\": [%.2f, %.2f, %.2f],\n", px - (sx / 2.0f), py, pz - (sz / 2.0f)));
            sb.append(String.format(Locale.ROOT, "              \"size\": [%.2f, %.2f, %.2f],\n", sx, sy, sz));
            sb.append("              \"uv\": [0, 0]\n");
            sb.append("            }\n");
            sb.append("          ]\n");
            sb.append("        }");

            if (++index < total) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");

        return sb.toString();
    }
}
