package dev.vessel.kernel.asset.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Transpiler compiling universal item model definitions into Minecraft 1.20.1 - 1.20.4
 * CustomModelData overrides JSON structures.
 * Extinguishes AST-MOD.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LegacyCustomModelDataCompiler {

    /**
     * Compiles a collection of model definitions into legacy JSON file mappings.
     *
     * @param definitions list of item model definitions
     * @return map of pack file relative path -> JSON content
     */
    public Map<String, String> compileLegacyPack(List<ItemModelSourceDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions cannot be null");

        Map<String, String> fileMap = new HashMap<>();
        Map<String, List<ItemModelSourceDefinition>> groupedByBase = new HashMap<>();

        for (ItemModelSourceDefinition def : definitions) {
            groupedByBase.computeIfAbsent(def.baseItemKey(), k -> new ArrayList<>()).add(def);

            // Generate child model JSON for the custom model itself
            String customModelPath = buildCustomModelFilePath(def);
            String customModelJson = buildCustomModelJson(def);
            fileMap.put(customModelPath, customModelJson);
        }

        for (Map.Entry<String, List<ItemModelSourceDefinition>> entry : groupedByBase.entrySet()) {
            List<ItemModelSourceDefinition> items = entry.getValue();
            if (items.isEmpty()) {
                continue;
            }
            ItemModelSourceDefinition first = items.getFirst();
            String baseModelPath = buildBaseModelFilePath(first);
            String baseModelJson = buildBaseOverrideJson(items);
            fileMap.put(baseModelPath, baseModelJson);
        }

        return Collections.unmodifiableMap(fileMap);
    }

    private String buildBaseModelFilePath(ItemModelSourceDefinition def) {
        return "assets/" + def.extractBaseNamespace() + "/models/item/" + def.extractBaseItemName() + ".json";
    }

    private String buildCustomModelFilePath(ItemModelSourceDefinition def) {
        String location = def.customModelLocation();
        int colon = location.indexOf(':');
        String namespace = colon >= 0 ? location.substring(0, colon) : "vessel";
        String path = colon >= 0 ? location.substring(colon + 1) : location;
        return "assets/" + namespace + "/models/" + path + ".json";
    }

    private String buildCustomModelJson(ItemModelSourceDefinition def) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\n");
        sb.append("  \"parent\": \"").append(escapeJson(def.parentModel())).append("\",\n");
        sb.append("  \"textures\": {\n");

        int index = 0;
        int size = def.textureMap().size();
        for (Map.Entry<String, String> tex : def.textureMap().entrySet()) {
            sb.append("    \"").append(escapeJson(tex.getKey())).append("\": \"")
              .append(escapeJson(tex.getValue())).append("\"");
            index++;
            if (index < size) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String buildBaseOverrideJson(List<ItemModelSourceDefinition> items) {
        ItemModelSourceDefinition first = items.getFirst();
        String baseNamespace = first.extractBaseNamespace();
        String baseName = first.extractBaseItemName();

        StringBuilder sb = new StringBuilder(512);
        sb.append("{\n");
        sb.append("  \"parent\": \"").append(escapeJson(first.parentModel())).append("\",\n");
        sb.append("  \"textures\": {\n");
        sb.append("    \"layer0\": \"").append(baseNamespace).append(":item/").append(baseName).append("\"\n");
        sb.append("  },\n");
        sb.append("  \"overrides\": [\n");

        for (int i = 0; i < items.size(); i++) {
            ItemModelSourceDefinition def = items.get(i);
            sb.append("    {\n");
            sb.append("      \"predicate\": {\n");
            sb.append("        \"custom_model_data\": ").append(def.customModelDataId()).append("\n");
            sb.append("      },\n");
            sb.append("      \"model\": \"").append(escapeJson(def.customModelLocation())).append("\"\n");
            sb.append("    }");
            if (i < items.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
