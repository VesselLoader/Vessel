package dev.vessel.kernel.modelengine.bone;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and mapper linking Model Engine model IDs and bone names to vanilla resource pack
 * CustomModelData (CMD) integer tags and asset JSON paths ({@code assets/<namespace>/models/item/...json}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ResourcePackModelMapping {

    private final Map<String, Integer> boneToCustomModelData = new ConcurrentHashMap<>();
    private final Map<String, String> boneToModelJsonPath = new ConcurrentHashMap<>();
    private final Map<Integer, String> cmdToBoneKey = new ConcurrentHashMap<>();

    /**
     * Constructs an empty ResourcePackModelMapping.
     */
    public ResourcePackModelMapping() {
    }

    /**
     * Registers a bone resource pack mapping.
     *
     * @param modelId         model identifier (e.g. "custom_golem")
     * @param boneName        bone name (e.g. "head", "left_arm")
     * @param customModelData unique CustomModelData integer
     * @param modelJsonPath   resource pack JSON path (e.g. "assets/vessel/models/item/golem/head.json")
     */
    public void registerMapping(String modelId, String boneName, int customModelData, String modelJsonPath) {
        Objects.requireNonNull(modelId, "modelId cannot be null");
        Objects.requireNonNull(boneName, "boneName cannot be null");
        Objects.requireNonNull(modelJsonPath, "modelJsonPath cannot be null");

        String key = buildKey(modelId, boneName);
        boneToCustomModelData.put(key, customModelData);
        boneToModelJsonPath.put(key, modelJsonPath);
        cmdToBoneKey.put(customModelData, key);
    }

    /**
     * Retrieves the CustomModelData integer for a given model bone.
     *
     * @param modelId  model identifier
     * @param boneName bone name
     * @return CustomModelData integer, or 0 if not registered
     */
    public int getCustomModelData(String modelId, String boneName) {
        String key = buildKey(modelId, boneName);
        Integer cmd = boneToCustomModelData.get(key);
        return cmd != null ? cmd : 0;
    }

    /**
     * Retrieves the model asset JSON path for a given model bone.
     *
     * @param modelId  model identifier
     * @param boneName bone name
     * @return asset path, or null if not registered
     */
    public String getModelJsonPath(String modelId, String boneName) {
        return boneToModelJsonPath.get(buildKey(modelId, boneName));
    }

    /**
     * Reverses lookup from CustomModelData integer to model bone composite key.
     *
     * @param customModelData CustomModelData tag
     * @return "modelId:boneName", or null if not found
     */
    public String findBoneKeyByCmd(int customModelData) {
        return cmdToBoneKey.get(customModelData);
    }

    private String buildKey(String modelId, String boneName) {
        return modelId.trim().toLowerCase() + ":" + boneName.trim().toLowerCase();
    }

    public Map<String, Integer> getAllMappings() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(boneToCustomModelData));
    }
}
