package dev.vessel.kernel.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Maximal Rectangles (MaxRects) Bin-Packing algorithm engine for generating optimal
 * Power-of-Two (POT) Texture Atlases for modded items, blocks, and entity skins.
 * Eliminates texture bleeding with configurable gutter padding and minimizes VRAM consumption.
 * Extinguishes ERR-85 (atlas overflow) and AST-TEX (UV mapping distortions).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TextureAtlasMaxRectsBinPacker {

    public static final int DEFAULT_MIN_SIZE = 256;
    public static final int DEFAULT_MAX_SIZE = 8192;
    public static final int DEFAULT_PADDING_PX = 2;

    private static final Comparator<TextureAtlasInputNode> TEXTURE_SORT_COMPARATOR =
            Comparator.comparingInt((TextureAtlasInputNode n) -> Math.max(n.width(), n.height()))
                    .reversed()
                    .thenComparingInt(n -> n.width() * n.height())
                    .reversed();

    private final int minAtlasSize;
    private final int maxAtlasSize;
    private final int padding;

    /**
     * Constructs a bin-packer with default limits (256px to 8192px, 2px padding).
     */
    public TextureAtlasMaxRectsBinPacker() {
        this(DEFAULT_MIN_SIZE, DEFAULT_MAX_SIZE, DEFAULT_PADDING_PX);
    }

    /**
     * Constructs a bin-packer with custom limits.
     *
     * @param minAtlasSize minimum atlas dimension (must be Power of Two)
     * @param maxAtlasSize maximum atlas dimension (must be Power of Two)
     * @param padding pixel margin surrounding each packed sprite
     */
    public TextureAtlasMaxRectsBinPacker(int minAtlasSize, int maxAtlasSize, int padding) {
        if (!isPowerOfTwo(minAtlasSize) || !isPowerOfTwo(maxAtlasSize)) {
            throw new IllegalArgumentException("Atlas sizes must be powers of two");
        }
        if (minAtlasSize > maxAtlasSize) {
            throw new IllegalArgumentException("minAtlasSize cannot exceed maxAtlasSize");
        }
        if (padding < 0) {
            throw new IllegalArgumentException("padding cannot be negative");
        }
        this.minAtlasSize = minAtlasSize;
        this.maxAtlasSize = maxAtlasSize;
        this.padding = padding;
    }

    /**
     * Packs the given list of texture nodes into the smallest possible square Power-of-Two atlas.
     *
     * @param inputs list of textures to pack
     * @return TextureAtlasPackResult containing placed coordinates and normalized UVs
     */
    public TextureAtlasPackResult pack(List<TextureAtlasInputNode> inputs) {
        Objects.requireNonNull(inputs, "inputs cannot be null");
        if (inputs.isEmpty()) {
            return new TextureAtlasPackResult(minAtlasSize, minAtlasSize, 0.0f, List.of(), List.of());
        }

        List<TextureAtlasInputNode> sortedNodes = new ArrayList<>(inputs);
        sortedNodes.sort(TEXTURE_SORT_COMPARATOR);

        int currentSize = minAtlasSize;
        while (currentSize <= maxAtlasSize) {
            TextureAtlasPackResult attempt = tryPackSize(sortedNodes, currentSize, currentSize);
            if (attempt.isFullyPacked()) {
                return attempt;
            }
            if (currentSize == maxAtlasSize) {
                return attempt; // Return partial result with unplaced nodes
            }
            currentSize *= 2;
        }

        return tryPackSize(sortedNodes, maxAtlasSize, maxAtlasSize);
    }

    private TextureAtlasPackResult tryPackSize(List<TextureAtlasInputNode> nodes, int atlasW, int atlasH) {
        List<FreeRectangularArea> freeRects = new ArrayList<>();
        freeRects.add(new FreeRectangularArea(0, 0, atlasW, atlasH));

        List<TextureAtlasPackedRegion> packed = new ArrayList<>();
        List<TextureAtlasInputNode> unplaced = new ArrayList<>();
        long totalAreaUsed = 0;

        for (TextureAtlasInputNode node : nodes) {
            int nodeW = node.width() + (padding * 2);
            int nodeH = node.height() + (padding * 2);

            int bestShortSideFit = Integer.MAX_VALUE;
            int bestIndex = -1;
            int bestX = 0;
            int bestY = 0;

            for (int i = 0; i < freeRects.size(); i++) {
                FreeRectangularArea rect = freeRects.get(i);
                if (rect.width() >= nodeW && rect.height() >= nodeH) {
                    int leftoverX = rect.width() - nodeW;
                    int leftoverY = rect.height() - nodeH;
                    int shortSide = Math.min(leftoverX, leftoverY);

                    if (shortSide < bestShortSideFit) {
                        bestShortSideFit = shortSide;
                        bestIndex = i;
                        bestX = rect.x();
                        bestY = rect.y();
                    }
                }
            }

            if (bestIndex == -1) {
                unplaced.add(node);
                continue;
            }

            // Split free rectangles intersecting with newly placed area
            int placeX = bestX;
            int placeY = bestY;
            splitIntersectingFreeRects(freeRects, placeX, placeY, nodeW, nodeH);
            pruneRedundantFreeRects(freeRects);

            // Sprite content offset by padding
            int spriteX = placeX + padding;
            int spriteY = placeY + padding;

            float uMin = (float) spriteX / (float) atlasW;
            float vMin = (float) spriteY / (float) atlasH;
            float uMax = (float) (spriteX + node.width()) / (float) atlasW;
            float vMax = (float) (spriteY + node.height()) / (float) atlasH;

            packed.add(new TextureAtlasPackedRegion(
                    node.textureId(),
                    spriteX,
                    spriteY,
                    node.width(),
                    node.height(),
                    uMin,
                    vMin,
                    uMax,
                    vMax
            ));

            totalAreaUsed += (long) node.width() * node.height();
        }

        float occupancy = (float) ((double) totalAreaUsed / (double) ((long) atlasW * atlasH));
        return new TextureAtlasPackResult(atlasW, atlasH, occupancy, packed, unplaced);
    }

    private void splitIntersectingFreeRects(List<FreeRectangularArea> freeRects, int px, int py, int pw, int ph) {
        List<FreeRectangularArea> newlyGenerated = new ArrayList<>();
        Iterator<FreeRectangularArea> it = freeRects.iterator();

        while (it.hasNext()) {
            FreeRectangularArea r = it.next();

            // Check if placed box intersects r
            if (px < r.x() + r.width() && px + pw > r.x() && py < r.y() + r.height() && py + ph > r.y()) {
                it.remove();

                // Generate up to 4 non-overlapping splits
                // New rect to the left
                if (px > r.x() && px < r.x() + r.width()) {
                    newlyGenerated.add(new FreeRectangularArea(r.x(), r.y(), px - r.x(), r.height()));
                }
                // New rect to the right
                if (px + pw < r.x() + r.width()) {
                    newlyGenerated.add(new FreeRectangularArea(px + pw, r.y(), r.x() + r.width() - (px + pw), r.height()));
                }
                // New rect to the top
                if (py > r.y() && py < r.y() + r.height()) {
                    newlyGenerated.add(new FreeRectangularArea(r.x(), r.y(), r.width(), py - r.y()));
                }
                // New rect to the bottom
                if (py + ph < r.y() + r.height()) {
                    newlyGenerated.add(new FreeRectangularArea(r.x(), py + ph, r.width(), r.y() + r.height() - (py + ph)));
                }
            }
        }

        freeRects.addAll(newlyGenerated);
    }

    private void pruneRedundantFreeRects(List<FreeRectangularArea> freeRects) {
        for (int i = 0; i < freeRects.size(); i++) {
            FreeRectangularArea a = freeRects.get(i);
            for (int j = i + 1; j < freeRects.size(); j++) {
                FreeRectangularArea b = freeRects.get(j);
                if (b.contains(a.x(), a.y(), a.width(), a.height())) {
                    freeRects.remove(i);
                    i--;
                    break;
                }
                if (a.contains(b.x(), b.y(), b.width(), b.height())) {
                    freeRects.remove(j);
                    j--;
                }
            }
        }
    }

    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    public int getMinAtlasSize() {
        return minAtlasSize;
    }

    public int getMaxAtlasSize() {
        return maxAtlasSize;
    }

    public int getPadding() {
        return padding;
    }
}
