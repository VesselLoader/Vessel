package dev.vessel.kernel.resourcepack.atlas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 2D Shelf Texture Atlas Packer optimized for Minecraft resource pack compilation.
 * Packs hundreds of small mod item and block textures into a single Power-of-Two (PoT)
 * composite atlas to minimize client texture swaps and draw calls.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastTextureAtlasPacker {

    private final int maxAtlasDimension;

    /**
     * Constructs a FastTextureAtlasPacker with a maximum dimension limit (e.g. 4096 or 8192).
     *
     * @param maxAtlasDimension maximum allowable width/height in pixels
     */
    public FastTextureAtlasPacker(int maxAtlasDimension) {
        if (maxAtlasDimension <= 0 || (maxAtlasDimension & (maxAtlasDimension - 1)) != 0) {
            throw new IllegalArgumentException("maxAtlasDimension must be a positive power of two: " + maxAtlasDimension);
        }
        this.maxAtlasDimension = maxAtlasDimension;
    }

    public FastTextureAtlasPacker() {
        this(4096);
    }

    /**
     * Packs all input tiles into a compact 2D shelf atlas layout.
     *
     * @param tiles list of tiles to pack
     * @return the resolved power-of-two atlas dimension (e.g. 256, 512, 1024)
     * @throws IllegalStateException if tiles exceed the maximum atlas dimension
     */
    public int pack(List<TextureAtlasTileDescriptor> tiles) {
        Objects.requireNonNull(tiles, "tiles cannot be null");
        if (tiles.isEmpty()) {
            return 16;
        }

        // Sort descending by height to optimize packing efficiency
        List<TextureAtlasTileDescriptor> sorted = new ArrayList<>(tiles);
        sorted.sort(Comparator.comparingInt(TextureAtlasTileDescriptor::getHeight).reversed());

        int currentAtlasWidth = 64;
        while (currentAtlasWidth <= maxAtlasDimension) {
            if (tryPackWithWidth(sorted, currentAtlasWidth)) {
                return currentAtlasWidth;
            }
            currentAtlasWidth <<= 1; // Double width (power of two)
        }

        throw new IllegalStateException("Textures exceed maximum atlas capacity of " + maxAtlasDimension + "x" + maxAtlasDimension);
    }

    private boolean tryPackWithWidth(List<TextureAtlasTileDescriptor> sortedTiles, int targetWidth) {
        int cursorX = 0;
        int cursorY = 0;
        int currentShelfHeight = 0;

        for (TextureAtlasTileDescriptor tile : sortedTiles) {
            if (tile.getWidth() > targetWidth) {
                return false;
            }

            // If tile exceeds current row, start new shelf
            if (cursorX + tile.getWidth() > targetWidth) {
                cursorX = 0;
                cursorY += currentShelfHeight;
                currentShelfHeight = 0;
            }

            if (cursorY + tile.getHeight() > targetWidth) {
                return false; // Does not fit within target square
            }

            tile.setAllocatedPosition(cursorX, cursorY);
            cursorX += tile.getWidth();
            currentShelfHeight = Math.max(currentShelfHeight, tile.getHeight());
        }

        return true;
    }
}
