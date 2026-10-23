package dev.vessel.kernel.modelengine.bedrock;

import java.util.Objects;

/**
 * Definition of a Model Engine entity model mapped for Bedrock Edition crossplay via GeyserMC.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GeyserModelDefinition {

    private final String modelId;
    private final String bedrockIdentifier;
    private final String texturePath;
    private final int textureWidth;
    private final int textureHeight;
    private final String renderController;

    /**
     * Constructs a GeyserModelDefinition with default 64x64 texture dimensions and default controller.
     *
     * @param modelId           model identifier (e.g. "custom_dragon")
     * @param bedrockIdentifier Bedrock geometry identifier (e.g. "geometry.vessel.custom_dragon")
     * @param texturePath       texture path (e.g. "textures/entity/custom_dragon.png")
     */
    public GeyserModelDefinition(String modelId, String bedrockIdentifier, String texturePath) {
        this(modelId, bedrockIdentifier, texturePath, 64, 64, "controller.render.default");
    }

    /**
     * Constructs a GeyserModelDefinition with explicit texture dimensions and render controller.
     */
    public GeyserModelDefinition(
            String modelId,
            String bedrockIdentifier,
            String texturePath,
            int textureWidth,
            int textureHeight,
            String renderController
    ) {
        this.modelId = Objects.requireNonNull(modelId, "modelId cannot be null");
        this.bedrockIdentifier = Objects.requireNonNull(bedrockIdentifier, "bedrockIdentifier cannot be null");
        this.texturePath = Objects.requireNonNull(texturePath, "texturePath cannot be null");
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive: " + textureWidth + "x" + textureHeight);
        }
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.renderController = Objects.requireNonNull(renderController, "renderController cannot be null");
    }

    public String getModelId() {
        return modelId;
    }

    public String getBedrockIdentifier() {
        return bedrockIdentifier;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public int getTextureWidth() {
        return textureWidth;
    }

    public int getTextureHeight() {
        return textureHeight;
    }

    public String getRenderController() {
        return renderController;
    }
}
