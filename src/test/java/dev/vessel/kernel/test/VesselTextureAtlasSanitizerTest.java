package dev.vessel.kernel.test;

import dev.vessel.kernel.resourcepack.VesselSanitizedTextureResult;
import dev.vessel.kernel.resourcepack.VesselTextureAtlasSanitizer;
import dev.vessel.kernel.resourcepack.VesselTextureDimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for {@link VesselTextureAtlasSanitizer}.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class VesselTextureAtlasSanitizerTest {

    private byte[] createTestPngBytes(int width, int height, Color color) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("isPowerOfTwo correctly identifies power-of-two values")
    void testPowerOfTwoDetection() {
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(1)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(2)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(4)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(16)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(32)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(64)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(256)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(512)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(4096)).isTrue();

        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(0)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(-1)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(-16)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(3)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(15)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(48)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(100)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isPowerOfTwo(1000)).isFalse();
    }

    @Test
    @DisplayName("isDimensionsPowerOfTwo verifies both width and height")
    void testDimensionsPowerOfTwo() {
        assertThat(VesselTextureAtlasSanitizer.isDimensionsPowerOfTwo(16, 16)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isDimensionsPowerOfTwo(64, 32)).isTrue();
        assertThat(VesselTextureAtlasSanitizer.isDimensionsPowerOfTwo(16, 15)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isDimensionsPowerOfTwo(15, 16)).isFalse();
        assertThat(VesselTextureAtlasSanitizer.isDimensionsPowerOfTwo(0, 16)).isFalse();
    }

    @Test
    @DisplayName("nextPowerOfTwo computes correct ceiling power of two")
    void testNextPowerOfTwo() {
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(0)).isEqualTo(1);
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(1)).isEqualTo(1);
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(2)).isEqualTo(2);
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(3)).isEqualTo(4);
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(15)).isEqualTo(16);
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(17)).isEqualTo(32);
        assertThat(VesselTextureAtlasSanitizer.nextPowerOfTwo(256)).isEqualTo(256);
    }

    @Test
    @DisplayName("extractDimensions reads PNG dimensions directly from raw header without raster decoding")
    void testExtractDimensions() throws IOException {
        byte[] png16 = createTestPngBytes(16, 32, Color.RED);
        VesselTextureDimensions dims = VesselTextureAtlasSanitizer.extractDimensions(png16);

        assertThat(dims.width()).isEqualTo(16);
        assertThat(dims.height()).isEqualTo(32);
        assertThat(dims.isPowerOfTwo()).isTrue();
        assertThat(dims.isSquare()).isFalse();

        byte[] pngNonPot = createTestPngBytes(15, 20, Color.BLUE);
        VesselTextureDimensions nonPotDims = VesselTextureAtlasSanitizer.extractDimensions(pngNonPot);
        assertThat(nonPotDims.width()).isEqualTo(15);
        assertThat(nonPotDims.height()).isEqualTo(20);
        assertThat(nonPotDims.isPowerOfTwo()).isFalse();
    }

    @Test
    @DisplayName("extractDimensions rejects corrupted or non-PNG byte payloads")
    void testCorruptedPngHeaderRejection() {
        assertThatThrownBy(() -> VesselTextureAtlasSanitizer.extractDimensions(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class);

        byte[] fakeMagic = new byte[30];
        assertThatThrownBy(() -> VesselTextureAtlasSanitizer.extractDimensions(fakeMagic))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("addTransparentPadding adds 1-pixel transparent boundary around image")
    void testTransparentPaddingOnBufferedImage() {
        VesselTextureAtlasSanitizer sanitizer = new VesselTextureAtlasSanitizer();

        BufferedImage source = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = source.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 16, 16);
        g.dispose();

        BufferedImage padded = sanitizer.addTransparentPadding(source, 1);

        assertThat(padded.getWidth()).isEqualTo(18);
        assertThat(padded.getHeight()).isEqualTo(18);

        // Verify corner border pixels are fully transparent (alpha channel == 0)
        assertThat((padded.getRGB(0, 0) >>> 24) & 0xFF).isEqualTo(0);
        assertThat((padded.getRGB(17, 0) >>> 24) & 0xFF).isEqualTo(0);
        assertThat((padded.getRGB(0, 17) >>> 24) & 0xFF).isEqualTo(0);
        assertThat((padded.getRGB(17, 17) >>> 24) & 0xFF).isEqualTo(0);

        // Verify original content at (1, 1) is preserved (RED with alpha = 255)
        int centerPixel = padded.getRGB(1, 1);
        assertThat((centerPixel >>> 24) & 0xFF).isEqualTo(255);
        assertThat((centerPixel >> 16) & 0xFF).isEqualTo(255); // Red
        assertThat((centerPixel >> 8) & 0xFF).isEqualTo(0);   // Green
        assertThat(centerPixel & 0xFF).isEqualTo(0);          // Blue
    }

    @Test
    @DisplayName("sanitize processes raw PNG byte array and returns complete sanitization result")
    void testSanitize() throws IOException {
        VesselTextureAtlasSanitizer sanitizer = new VesselTextureAtlasSanitizer();
        byte[] inputPng = createTestPngBytes(32, 32, Color.GREEN);

        VesselSanitizedTextureResult result = sanitizer.sanitize(inputPng);

        assertThat(result.originalWidth()).isEqualTo(32);
        assertThat(result.originalHeight()).isEqualTo(32);
        assertThat(result.paddedWidth()).isEqualTo(34);
        assertThat(result.paddedHeight()).isEqualTo(34);
        assertThat(result.originallyPowerOfTwo()).isTrue();
        assertThat(result.sanitizedPngData()).isNotEmpty();

        // Decode sanitized PNG and verify dimensions
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.sanitizedPngData()));
        assertThat(decoded.getWidth()).isEqualTo(34);
        assertThat(decoded.getHeight()).isEqualTo(34);
    }

    @Test
    @DisplayName("padToPowerOfTwo pads non-PoT images up to nearest power of two canvas")
    void testPadToPowerOfTwo() throws IOException {
        VesselTextureAtlasSanitizer sanitizer = new VesselTextureAtlasSanitizer();
        byte[] inputNonPot = createTestPngBytes(15, 20, Color.MAGENTA);
        byte[] padded = sanitizer.padToPowerOfTwo(inputNonPot);

        VesselTextureDimensions dims = VesselTextureAtlasSanitizer.extractDimensions(padded);
        assertThat(dims.width()).isEqualTo(16);
        assertThat(dims.height()).isEqualTo(32);
        assertThat(dims.isPowerOfTwo()).isTrue();
    }

    @Test
    @DisplayName("nextPowerOfTwo rejects values greater than 2^30")
    void testNextPowerOfTwoOverflow() {
        assertThatThrownBy(() -> VesselTextureAtlasSanitizer.nextPowerOfTwo((1 << 30) + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
