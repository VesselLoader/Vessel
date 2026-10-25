package dev.vessel.kernel.modelengine.bedrock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge registering custom Model Engine models with GeyserMC custom entity mappings,
 * allowing Bedrock Edition players to see custom 3D entity models and textures.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GeyserEntityRegistrationBridge {

    private final Map<String, GeyserModelDefinition> registeredModels = new ConcurrentHashMap<>();

    /**
     * Constructs a GeyserEntityRegistrationBridge.
     */
    public GeyserEntityRegistrationBridge() {
    }

    /**
     * Registers a custom model definition for Geyser translation.
     *
     * @param definition Geyser model definition
     */
    public void registerModel(GeyserModelDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        registeredModels.put(definition.getModelId(), definition);
    }

    /**
     * Unregisters a model definition.
     *
     * @param modelId model identifier
     */
    public void unregisterModel(String modelId) {
        registeredModels.remove(modelId);
    }

    /**
     * Retrieves a registered model definition.
     *
     * @param modelId model identifier
     * @return model definition, or null if not registered
     */
    public GeyserModelDefinition getModel(String modelId) {
        return registeredModels.get(modelId);
    }

    /**
     * Generates a Geyser custom entity client mapping JSON string.
     *
     * @return formatted custom entity mappings JSON
     */
    public String generateGeyserCustomEntityMappingsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"format_version\": \"1.0.0\",\n");
        sb.append("  \"entities\": {\n");

        int index = 0;
        int total = registeredModels.size();

        for (GeyserModelDefinition def : registeredModels.values()) {
            sb.append("    \"").append(def.getModelId()).append("\": {\n");
            sb.append("      \"bedrock_identifier\": \"").append(def.getBedrockIdentifier()).append("\",\n");
            sb.append("      \"texture\": \"").append(def.getTexturePath()).append("\",\n");
            sb.append("      \"render_controller\": \"").append(def.getRenderController()).append("\"\n");
            sb.append("    }");

            if (++index < total) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  }\n");
        sb.append("}");

        return sb.toString();
    }

    public Map<String, GeyserModelDefinition> getAllModels() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(registeredModels));
    }

    public int getRegisteredModelCount() {
        return registeredModels.size();
    }
}
