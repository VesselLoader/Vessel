package dev.vessel.kernel.asset.stream;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Universal canonical definition of a custom item model before version transpilation.
 * Serves as the single source of truth for both legacy overrides and modern item_models.
 * Extinguishes AST-MOD.
 *
 * @param baseItemKey base Minecraft item identifier (e.g. "minecraft:diamond_sword")
 * @param customModelDataId integer CustomModelData index (e.g. 10001)
 * @param customModelLocation custom model target location (e.g. "vessel:item/custom_sword")
 * @param parentModel parent model reference (e.g. "minecraft:item/handheld")
 * @param textureMap texture layer mappings (e.g. "layer0" -> "vessel:item/custom_sword")
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ItemModelSourceDefinition(
        String baseItemKey,
        int customModelDataId,
        String customModelLocation,
        String parentModel,
        Map<String, String> textureMap
) {

    public ItemModelSourceDefinition {
        Objects.requireNonNull(baseItemKey, "baseItemKey cannot be null");
        Objects.requireNonNull(customModelLocation, "customModelLocation cannot be null");
        Objects.requireNonNull(parentModel, "parentModel cannot be null");
        if (textureMap == null) {
            textureMap = Collections.emptyMap();
        } else {
            textureMap = Collections.unmodifiableMap(Map.copyOf(textureMap));
        }
        if (customModelDataId < 0) {
            throw new IllegalArgumentException("customModelDataId must be non-negative: " + customModelDataId);
        }
    }

    /**
     * Extracts the base item name without namespace (e.g. "diamond_sword").
     *
     * @return base item path
     */
    public String extractBaseItemName() {
        int colon = baseItemKey.indexOf(':');
        return colon >= 0 ? baseItemKey.substring(colon + 1) : baseItemKey;
    }

    /**
     * Extracts the namespace of the base item (e.g. "minecraft").
     *
     * @return namespace
     */
    public String extractBaseNamespace() {
        int colon = baseItemKey.indexOf(':');
        return colon >= 0 ? baseItemKey.substring(0, colon) : "minecraft";
    }

    /**
     * Extracts the custom item name without namespace (e.g. "custom_sword").
     *
     * @return custom model path
     */
    public String extractCustomModelName() {
        int colon = customModelLocation.indexOf(':');
        String path = colon >= 0 ? customModelLocation.substring(colon + 1) : customModelLocation;
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /**
     * Extracts the custom model namespace (e.g. "vessel").
     *
     * @return custom namespace
     */
    public String extractCustomNamespace() {
        int colon = customModelLocation.indexOf(':');
        return colon >= 0 ? customModelLocation.substring(0, colon) : "vessel";
    }
}
