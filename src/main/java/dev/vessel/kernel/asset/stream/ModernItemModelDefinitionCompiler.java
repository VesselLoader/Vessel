package dev.vessel.kernel.asset.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Transpiler compiling universal item model definitions into modern Minecraft 1.20.5+
 * item model definitions (assets/namespace/items/*.json) utilizing minecraft:range_dispatch.
 * Extinguishes AST-MOD.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModernItemModelDefinitionCompiler {

    /**
     * Compiles a collection of model definitions into modern 1.20.5+ JSON file mappings.
     *
     * @param definitions list of item model definitions
     * @return map of pack file relative path -> JSON content
     */
    public Map<String, String> compileModernPack(List<ItemModelSourceDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions cannot be null");

        Map<String, String> fileMap = new HashMap<>();
        Map<String, List<ItemModelSourceDefinition>> groupedByBase = new HashMap<>();

        for (ItemModelSourceDefinition def : definitions) {
            groupedByBase.computeIfAbsent(def.baseItemKey(), k -> new ArrayList<>()).add(def);

            // Generate modern item model JSON referenced by the entries
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
            String itemDefinitionPath = buildModernItemDefinitionPath(first);
            String itemDefinitionJson = buildModernItemDefinitionJson(items);
            fileMap.put(itemDefinitionPath, itemDefinitionJson);
        }

        return Collections.unmodifiableMap(fileMap);
    }

    private String buildModernItemDefinitionPath(ItemModelSourceDefinition def) {
        // Minecraft 1.20.5+ item model definition path: assets/<namespace>/items/<item_name>.json
        return "assets/" + def.extractBaseNamespace() + "/items/" + def.extractBaseItemName() + ".json";
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

    private String buildModernItemDefinitionJson(List<ItemModelSourceDefinition> items) {
        ItemModelSourceDefinition first = items.getFirst();
        String baseModelRef = first.baseItemKey().contains(":")
                ? first.baseItemKey().replace(":", ":item/")
                : "minecraft:item/" + first.baseItemKey();

        StringBuilder sb = new StringBuilder(512);
        sb.append("{\n");
        sb.append("  \"model\": {\n");
        sb.append("    \"type\": \"minecraft:range_dispatch\",\n");
        sb.append("    \"property\": \"minecraft:custom_model_data\",\n");
        sb.append("    \"entries\": [\n");

        for (int i = 0; i < items.size(); i++) {
            ItemModelSourceDefinition def = items.get(i);
            sb.append("      {\n");
            sb.append("        \"threshold\": ").append(def.customModelDataId()).append(",\n");
            sb.append("        \"model\": {\n");
            sb.append("          \"type\": \"minecraft:model\",\n");
            sb.append("          \"model\": \"").append(escapeJson(def.customModelLocation())).append("\"\n");
            sb.append("        }\n");
            sb.append("      }");
            if (i < items.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("    ],\n");
        sb.append("    \"fallback\": {\n");
        sb.append("      \"type\": \"minecraft:model\",\n");
        sb.append("      \"model\": \"").append(escapeJson(baseModelRef)).append("\"\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
