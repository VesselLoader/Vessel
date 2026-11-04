package dev.vessel.kernel.resourcepack;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Texture sanitizer ensuring texture assets meet Power-of-Two (PoT) alignment and
 * prevent UV bleeding (Atlas Seams) on mobile GPUs (Bedrock RenderDragon / Vulkan).
 * <p>
 * Evaluates dimensions and injects a 1-pixel transparent boundary around texture tiles,
 * triệt tiêu hiện tượng rách mép UV khi nội suy bilinear/trilinear hoặc mipmapping trên thiết bị di động.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 * <!-- Changelog:
 *   1.0.0: Initial implementation of PoT detection and 1-pixel transparent boundary padding.
 *   1.0.1: Added overflow protection in nextPowerOfTwo, strict IHDR chunk length check, and padToPowerOfTwo methods.
 * -->
 */
public final class VesselTextureAtlasSanitizer {

    /** Standard 8-byte PNG file magic header. */
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    /** Default border padding size: 1 pixel. */
    public static final int DEFAULT_PADDING_PIXELS = 1;

    /**
     * Checks if an integer is an exact positive power of two ($2^N$).
     *
     * @param value dimension to check
     * @return true if positive power of two
     */
    public static boolean isPowerOfTwo(int value) {
        return value > 0 && (value & (value - 1)) == 0;
    }

    /**
     * Checks whether both width and height are powers of two.
     *
     * @param width width in pixels
     * @param height height in pixels
     * @return true if both width and height are powers of two
     */
    public static boolean isDimensionsPowerOfTwo(int width, int height) {
        return isPowerOfTwo(width) && isPowerOfTwo(height);
    }

    /**
     * Calculates the smallest power of two greater than or equal to the given value.
     *
     * @param value positive integer
     * @return smallest power of two >= value
     * @throws IllegalArgumentException if value exceeds maximum representable power of two (2^30)
     */
    public static int nextPowerOfTwo(int value) {
        if (value <= 1) {
            return 1;
        }
        if (value > (1 << 30)) {
            throw new IllegalArgumentException("Dimension exceeds maximum supported power of two (2^30): " + value);
        }
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Extracts width and height directly from PNG raw bytes without decoding the pixel raster.
     *
     * @param pngBytes raw PNG file bytes
     * @return extracted dimensions
     * @throws IllegalArgumentException if payload is not a valid PNG
     */
    public static VesselTextureDimensions extractDimensions(byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes cannot be null");
        if (pngBytes.length < 24) {
            throw new IllegalArgumentException("PNG byte payload too short: length " + pngBytes.length);
        }

        // Validate 8-byte magic
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (pngBytes[i] != PNG_SIGNATURE[i]) {
                throw new IllegalArgumentException("Invalid PNG signature at offset " + i);
            }
        }

        // Validate IHDR chunk length (bytes 8..11 must be 13)
        int ihdrLen = ((pngBytes[8] & 0xFF) << 24)
                | ((pngBytes[9] & 0xFF) << 16)
                | ((pngBytes[10] & 0xFF) << 8)
                | (pngBytes[11] & 0xFF);
        if (ihdrLen != 13) {
            throw new IllegalArgumentException("Invalid IHDR chunk length in PNG: " + ihdrLen);
        }

        // Validate IHDR chunk tag (bytes 12..15)
        if (pngBytes[12] != 'I' || pngBytes[13] != 'H' || pngBytes[14] != 'D' || pngBytes[15] != 'R') {
            throw new IllegalArgumentException("Missing IHDR chunk in PNG header");
        }

        int width = ((pngBytes[16] & 0xFF) << 24)
                | ((pngBytes[17] & 0xFF) << 16)
                | ((pngBytes[18] & 0xFF) << 8)
                | (pngBytes[19] & 0xFF);

        int height = ((pngBytes[20] & 0xFF) << 24)
                | ((pngBytes[21] & 0xFF) << 16)
                | ((pngBytes[22] & 0xFF) << 8)
                | (pngBytes[23] & 0xFF);

        return new VesselTextureDimensions(width, height);
    }

    /**
     * Pads an in-memory {@link BufferedImage} to the nearest power-of-two canvas ($2^N \times 2^M$).
     *
     * @param source input image
     * @return image padded to next power-of-two dimensions
     */
    public BufferedImage padToPowerOfTwo(BufferedImage source) {
        Objects.requireNonNull(source, "source cannot be null");
        int srcW = source.getWidth();
        int srcH = source.getHeight();
        int targetW = nextPowerOfTwo(srcW);
        int targetH = nextPowerOfTwo(srcH);

        if (srcW == targetW && srcH == targetH) {
            return source;
        }

        BufferedImage padded = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = padded.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return padded;
    }

    /**
     * Pads a raw PNG byte array to the nearest power-of-two canvas ($2^N \times 2^M$).
     *
     * @param pngBytes raw PNG bytes
     * @return padded PNG byte payload
     */
    public byte[] padToPowerOfTwo(byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes cannot be null");
        VesselTextureDimensions dims = extractDimensions(pngBytes);
        if (dims.isPowerOfTwo()) {
            return pngBytes.clone();
        }

        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode PNG image", e);
        }
        if (source == null) {
            throw new IllegalArgumentException("ImageIO could not decode PNG payload (unsupported or corrupted format)");
        }

        BufferedImage padded = padToPowerOfTwo(source);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(padded, "PNG", baos);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode padded PNG to stream", e);
        }

        return baos.toByteArray();
    }

    /**
     * Injects a transparent border around an in-memory {@link BufferedImage}.
     *
     * @param source source image
     * @param paddingPx padding margin in pixels
     * @return new padded {@link BufferedImage} with {@link BufferedImage#TYPE_INT_ARGB}
     */
    public BufferedImage addTransparentPadding(BufferedImage source, int paddingPx) {
        Objects.requireNonNull(source, "source cannot be null");
        if (paddingPx < 0) {
            throw new IllegalArgumentException("paddingPx cannot be negative: " + paddingPx);
        }
        if (paddingPx == 0) {
            return source;
        }

        int srcW = source.getWidth();
        int srcH = source.getHeight();
        int targetW = srcW + (paddingPx * 2);
        int targetH = srcH + (paddingPx * 2);

        BufferedImage padded = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = padded.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(source, paddingPx, paddingPx, null);
        } finally {
            g.dispose();
        }

        return padded;
    }

    /**
     * Injects a transparent border into raw PNG byte payload and re-encodes as PNG.
     *
     * @param pngBytes raw PNG bytes
     * @param paddingPx padding margin in pixels
     * @return sanitized PNG byte payload
     * @throws IllegalArgumentException if image cannot be parsed
     */
    public byte[] addTransparentPadding(byte[] pngBytes, int paddingPx) {
        Objects.requireNonNull(pngBytes, "pngBytes cannot be null");
        if (paddingPx < 0) {
            throw new IllegalArgumentException("paddingPx cannot be negative: " + paddingPx);
        }
        if (paddingPx == 0) {
            return pngBytes.clone();
        }

        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decode PNG image", e);
        }
        if (source == null) {
            throw new IllegalArgumentException("ImageIO could not decode PNG payload (unsupported or corrupted format)");
        }

        BufferedImage padded = addTransparentPadding(source, paddingPx);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(padded, "PNG", baos);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode sanitized PNG to stream", e);
        }

        return baos.toByteArray();
    }

    /**
     * Sanitizes a texture asset by checking its dimensions and adding a standard 1-pixel
     * transparent border to prevent RenderDragon atlas bleeding.
     *
     * @param pngBytes raw PNG bytes
     * @return sanitization result containing metadata and sanitized PNG data
     */
    public VesselSanitizedTextureResult sanitize(byte[] pngBytes) {
        Objects.requireNonNull(pngBytes, "pngBytes cannot be null");
        VesselTextureDimensions dims = extractDimensions(pngBytes);
        boolean wasPot = dims.isPowerOfTwo();

        byte[] paddedData = addTransparentPadding(pngBytes, DEFAULT_PADDING_PIXELS);
        int paddedW = dims.width() + (DEFAULT_PADDING_PIXELS * 2);
        int paddedH = dims.height() + (DEFAULT_PADDING_PIXELS * 2);

        return new VesselSanitizedTextureResult(
                paddedData,
                dims.width(),
                dims.height(),
                paddedW,
                paddedH,
                wasPot
        );
    }
}
