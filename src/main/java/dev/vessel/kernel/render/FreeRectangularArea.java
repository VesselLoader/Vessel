package dev.vessel.kernel.render;

/**
 * Immutable rectangular free space slot within the bin-packing atlas canvas.
 *
 * @param x top-left horizontal coordinate
 * @param y top-left vertical coordinate
 * @param width available pixel width
 * @param height available pixel height
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FreeRectangularArea(
        int x,
        int y,
        int width,
        int height
) {
    public FreeRectangularArea {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive: " + width + "x" + height);
        }
    }

    /**
     * Checks if this free rectangle completely contains another rectangular area.
     */
    public boolean contains(int ox, int oy, int ow, int oh) {
        return ox >= x && oy >= y && (ox + ow) <= (x + width) && (oy + oh) <= (y + height);
    }
}
