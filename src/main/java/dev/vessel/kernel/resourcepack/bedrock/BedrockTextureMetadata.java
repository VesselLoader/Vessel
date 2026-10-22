package dev.vessel.kernel.resourcepack.bedrock;

import java.util.Objects;

/**
 * Immutable metadata record representing a Bedrock-converted texture.
 * <p>
 * Stores converted asset paths, animation frame properties, and generates
 * standard Bedrock flipbook JSON entries for animated block/item textures.
 * </p>
 *
 * @param bedrockPath relative Bedrock path (e.g. "textures/blocks/custom_ore.png")
 * @param javaPath original Java resource pack path (e.g. "assets/mymod/textures/block/custom_ore.png")
 * @param animated whether this texture contains an animation sequence
 * @param ticksPerFrame duration in game ticks each frame remains visible (default 1)
 * @param frameCount number of vertical frames in the texture strip (>= 1)
 * @param interpolate whether to blend/interpolate transitions between frames
 * @param customFrames optional specific frame indices sequence, or null for default 0..frameCount-1
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public record BedrockTextureMetadata(
        String bedrockPath,
        String javaPath,
        boolean animated,
        int ticksPerFrame,
        int frameCount,
        boolean interpolate,
        int[] customFrames
) {

    public BedrockTextureMetadata {
        Objects.requireNonNull(bedrockPath, "bedrockPath cannot be null");
        Objects.requireNonNull(javaPath, "javaPath cannot be null");
        if (ticksPerFrame <= 0) {
            throw new IllegalArgumentException("ticksPerFrame must be positive: " + ticksPerFrame);
        }
        if (frameCount <= 0) {
            throw new IllegalArgumentException("frameCount must be positive: " + frameCount);
        }
        customFrames = (customFrames != null && customFrames.length > 0) ? customFrames.clone() : null;
    }

    /**
     * Backward-compatible 6-parameter constructor for standard linear frames.
     */
    public BedrockTextureMetadata(
            String bedrockPath,
            String javaPath,
            boolean animated,
            int ticksPerFrame,
            int frameCount,
            boolean interpolate
    ) {
        this(bedrockPath, javaPath, animated, ticksPerFrame, frameCount, interpolate, null);
    }

    /**
     * Creates static non-animated texture metadata.
     *
     * @param bedrockPath converted Bedrock path
     * @param javaPath original Java path
     * @return non-animated metadata record
     */
    public static BedrockTextureMetadata ofStatic(String bedrockPath, String javaPath) {
        return new BedrockTextureMetadata(bedrockPath, javaPath, false, 1, 1, false, null);
    }

    /**
     * Creates animated texture metadata with linear frames.
     *
     * @param bedrockPath converted Bedrock path
     * @param javaPath original Java path
     * @param ticksPerFrame ticks per frame
     * @param frameCount total frames
     * @param interpolate smooth transition flag
     * @return animated metadata record
     */
    public static BedrockTextureMetadata ofAnimated(
            String bedrockPath,
            String javaPath,
            int ticksPerFrame,
            int frameCount,
            boolean interpolate
    ) {
        return new BedrockTextureMetadata(bedrockPath, javaPath, true, ticksPerFrame, frameCount, interpolate, null);
    }

    /**
     * Creates animated texture metadata with custom frame sequence.
     *
     * @param bedrockPath converted Bedrock path
     * @param javaPath original Java path
     * @param ticksPerFrame ticks per frame
     * @param frameCount total frames
     * @param interpolate smooth transition flag
     * @param customFrames custom frame index ordering
     * @return animated metadata record
     */
    public static BedrockTextureMetadata ofAnimated(
            String bedrockPath,
            String javaPath,
            int ticksPerFrame,
            int frameCount,
            boolean interpolate,
            int[] customFrames
    ) {
        return new BedrockTextureMetadata(bedrockPath, javaPath, true, ticksPerFrame, frameCount, interpolate, customFrames);
    }

    @Override
    public int[] customFrames() {
        return customFrames != null ? customFrames.clone() : null;
    }

    /**
     * Generates a Bedrock {@code flipbook_textures.json} definition entry snippet.
     *
     * @param atlasTile atlas tile key name
     * @return JSON fragment for this flipbook texture
     */
    public String toFlipbookJsonEntry(String atlasTile) {
        Objects.requireNonNull(atlasTile, "atlasTile cannot be null");
        String textureWithoutExt = bedrockPath;
        if (textureWithoutExt.endsWith(".png")) {
            textureWithoutExt = textureWithoutExt.substring(0, textureWithoutExt.length() - 4);
        }
        if (textureWithoutExt.endsWith(".tga")) {
            textureWithoutExt = textureWithoutExt.substring(0, textureWithoutExt.length() - 4);
        }

        StringBuilder framesArray = new StringBuilder("[");
        if (customFrames != null && customFrames.length > 0) {
            for (int i = 0; i < customFrames.length; i++) {
                framesArray.append(customFrames[i]);
                if (i < customFrames.length - 1) {
                    framesArray.append(", ");
                }
            }
        } else {
            for (int i = 0; i < frameCount; i++) {
                framesArray.append(i);
                if (i < frameCount - 1) {
                    framesArray.append(", ");
                }
            }
        }
        framesArray.append("]");

        return "{\n"
                + "  \"flipbook_texture\": \"" + textureWithoutExt + "\",\n"
                + "  \"atlas_tile\": \"" + atlasTile + "\",\n"
                + "  \"ticks_per_frame\": " + ticksPerFrame + ",\n"
                + "  \"frames\": " + framesArray + ",\n"
                + "  \"blend_frames\": " + interpolate + "\n"
                + "}";
    }
}
