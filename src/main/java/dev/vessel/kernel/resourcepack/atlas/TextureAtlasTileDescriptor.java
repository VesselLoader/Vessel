package dev.vessel.kernel.resourcepack.atlas;

import java.util.Objects;

/**
 * Metadata descriptor for an individual texture tile participating in an atlas pack.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TextureAtlasTileDescriptor {

    private final String textureId;
    private final int width;
    private final int height;
    private final boolean animated;

    private int allocatedX = -1;
    private int allocatedY = -1;

    /**
     * Constructs a TextureAtlasTileDescriptor.
     *
     * @param textureId unique resource location of the texture
     * @param width width in pixels
     * @param height height in pixels
     * @param animated whether the tile contains vertical animation frames
     */
    public TextureAtlasTileDescriptor(String textureId, int width, int height, boolean animated) {
        this.textureId = Objects.requireNonNull(textureId, "textureId cannot be null");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Tile dimensions must be positive, got " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.animated = animated;
    }

    public void setAllocatedPosition(int x, int y) {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates cannot be negative: (" + x + ", " + y + ")");
        }
        this.allocatedX = x;
        this.allocatedY = y;
    }

    public String getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isAnimated() {
        return animated;
    }

    public int getAllocatedX() {
        return allocatedX;
    }

    public int getAllocatedY() {
        return allocatedY;
    }

    public boolean isAllocated() {
        return allocatedX >= 0 && allocatedY >= 0;
    }
}
