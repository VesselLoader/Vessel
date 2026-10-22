package dev.vessel.kernel.test;

import dev.vessel.kernel.optimization.network.FastVarIntCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Krypton & ImmediatelyFast-inspired VarInt codec.
 *
 * @version 1.0.0
 */
public class NetworkOptimizationCodecTest {

    @Test
    @DisplayName("FastVarIntCodec accurately computes required byte size")
    void testVarIntSize() {
        assertThat(FastVarIntCodec.getVarIntSize(0)).isEqualTo(1);
        assertThat(FastVarIntCodec.getVarIntSize(127)).isEqualTo(1);
        assertThat(FastVarIntCodec.getVarIntSize(128)).isEqualTo(2);
        assertThat(FastVarIntCodec.getVarIntSize(255)).isEqualTo(2);
        assertThat(FastVarIntCodec.getVarIntSize(16383)).isEqualTo(2);
        assertThat(FastVarIntCodec.getVarIntSize(16384)).isEqualTo(3);
        assertThat(FastVarIntCodec.getVarIntSize(2097151)).isEqualTo(3);
        assertThat(FastVarIntCodec.getVarIntSize(2097152)).isEqualTo(4);
        assertThat(FastVarIntCodec.getVarIntSize(268435455)).isEqualTo(4);
        assertThat(FastVarIntCodec.getVarIntSize(268435456)).isEqualTo(5);
        assertThat(FastVarIntCodec.getVarIntSize(-1)).isEqualTo(5);
    }

    @Test
    @DisplayName("FastVarIntCodec encodes and decodes values without allocations")
    void testEncodeDecodeRoundTrip() {
        int[] testValues = new int[]{0, 1, 127, 128, 255, 1000, 16384, 65535, 1000000, Integer.MAX_VALUE, -1, -128, -32768};
        byte[] buffer = new byte[16];

        for (int value : testValues) {
            int bytesWritten = FastVarIntCodec.writeVarInt(value, buffer, 0);
            assertThat(bytesWritten).isEqualTo(FastVarIntCodec.getVarIntSize(value));

            long packed = FastVarIntCodec.readVarIntPacked(buffer, 0);
            int decodedValue = FastVarIntCodec.unpackValue(packed);
            int bytesRead = FastVarIntCodec.unpackBytesRead(packed);

            assertThat(decodedValue).isEqualTo(value);
            assertThat(bytesRead).isEqualTo(bytesWritten);
        }
    }

    @Test
    @DisplayName("FastVarIntCodec detects truncated data")
    void testTruncationDetection() {
        byte[] truncated = new byte[]{(byte) 0x80}; // MSB set indicating more bytes, but stream ends

        assertThatThrownBy(() -> FastVarIntCodec.readVarIntPacked(truncated, 0))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
