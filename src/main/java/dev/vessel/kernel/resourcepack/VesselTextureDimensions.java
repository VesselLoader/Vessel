package dev.vessel.kernel.resourcepack;

/**
 * 2D pixel dimensions descriptor for texture assets.
 *
 * @param width horizontal pixel width
 * @param height vertical pixel height
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VesselTextureDimensions(int width, int height) {

    public VesselTextureDimensions {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be positive: " + width);
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be positive: " + height);
        }
    }

    /**
     * Checks if both width and height are exact powers of two ($2^N$).
     *
     * @return true if both dimensions are powers of two
     */
    public boolean isPowerOfTwo() {
        return (width & (width - 1)) == 0 && (height & (height - 1)) == 0;
    }

    /**
     * Checks if the texture is a square ($W == H$).
     *
     * @return true if square
     */
    public boolean isSquare() {
        return width == height;
    }
}
