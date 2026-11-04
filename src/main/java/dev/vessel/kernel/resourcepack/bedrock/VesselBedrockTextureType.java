package dev.vessel.kernel.resourcepack.bedrock;

import java.util.Objects;

/**
 * Enumeration of Bedrock Edition texture atlas dictionary targets.
 * <p>
 * Distinguishes between item atlas definitions ({@code textures/item_texture.json})
 * and terrain block atlas definitions ({@code textures/terrain_texture.json}).
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public enum VesselBedrockTextureType {

    /**
     * Item texture atlas mapping individual item identifiers to icon textures.
     */
    ITEM("atlas.items", "textures/item_texture.json"),

    /**
     * Terrain texture atlas mapping block / tile identifiers to block face textures.
     */
    TERRAIN("atlas.terrain", "textures/terrain_texture.json");

    private final String atlasName;
    private final String dictionaryPath;

    VesselBedrockTextureType(String atlasName, String dictionaryPath) {
        this.atlasName = Objects.requireNonNull(atlasName, "atlasName cannot be null");
        this.dictionaryPath = Objects.requireNonNull(dictionaryPath, "dictionaryPath cannot be null");
    }

    /**
     * Returns the canonical Bedrock atlas name (e.g. {@code "atlas.items"} or {@code "atlas.terrain"}).
     *
     * @return atlas identifier
     */
    public String atlasName() {
        return atlasName;
    }

    /**
     * Returns the relative pack path for this dictionary file.
     *
     * @return dictionary path
     */
    public String dictionaryPath() {
        return dictionaryPath;
    }
}
