package dev.vessel.kernel.test;

import dev.vessel.kernel.vfs.CompactBytecodeStorage;
import dev.vessel.kernel.vfs.InMemoryVirtualFileSystemMount;
import dev.vessel.kernel.vfs.ZeroCopyByteBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for VFS Zero-Copy and Compact Bytecode Storage optimizations.
 *
 * @version 1.0.1
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public class VfsOptimizationTest {

    @Test
    @DisplayName("ZeroCopyByteBuffer provides allocation-free views and safe conversions")
    void testZeroCopyByteBuffer() throws Exception {
        byte[] original = "VESSEL_ZERO_COPY_PAYLOAD_TEST".getBytes(StandardCharsets.UTF_8);
        ZeroCopyByteBuffer buffer = ZeroCopyByteBuffer.wrap(original);

        assertThat(buffer.length()).isEqualTo(original.length);
        assertThat(buffer.get(0)).isEqualTo(original[0]);
        assertThat(buffer.toByteArrayCopy()).isEqualTo(original);

        try (InputStream in = buffer.asInputStream()) {
            byte[] readBack = in.readAllBytes();
            assertThat(readBack).isEqualTo(original);
        }

        ByteBuffer nioBuffer = buffer.asReadOnlyByteBuffer();
        assertThat(nioBuffer.isReadOnly()).isTrue();
        assertThat(nioBuffer.remaining()).isEqualTo(original.length);

        assertThatThrownBy(() -> buffer.get(buffer.length() + 10))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    @DisplayName("CompactBytecodeStorage compresses large classes and passes through small payloads")
    void testCompactBytecodeStorage() {
        CompactBytecodeStorage storage = new CompactBytecodeStorage();

        // 1. Small payload (<256 bytes): raw mode
        byte[] small = "small payload".getBytes(StandardCharsets.UTF_8);
        byte[] compactedSmall = storage.compact(small);
        assertThat(compactedSmall[0]).isEqualTo((byte) 0x00);
        assertThat(storage.decompress(compactedSmall)).isEqualTo(small);

        // 2. Large synthetic class bytecode (10KB repeated pattern): deflated mode
        byte[] large = new byte[10240];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i % 64);
        }
        byte[] compactedLarge = storage.compact(large);
        assertThat(compactedLarge[0]).isEqualTo((byte) 0x01);
        // Bytecode compression ratio should reduce size by >70%
        assertThat(compactedLarge.length).isLessThan(large.length / 3);

        byte[] decompressedLarge = storage.decompress(compactedLarge);
        assertThat(decompressedLarge).isEqualTo(large);

        ZeroCopyByteBuffer zeroCopyView = storage.retrieveAsZeroCopy(compactedLarge);
        assertThat(zeroCopyView.length()).isEqualTo(large.length);
        assertThat(zeroCopyView.toByteArrayCopy()).isEqualTo(large);
    }

    @Test
    @DisplayName("InMemoryVirtualFileSystemMount readResourceZeroCopy returns valid zero-copy buffer")
    void testVfsZeroCopyRead() throws Exception {
        try (InMemoryVirtualFileSystemMount mount = new InMemoryVirtualFileSystemMount("/test.jar")) {
            byte[] bytes = new byte[]{ 0x10, 0x20, 0x30, 0x40 };
            mount.putResource("test.class", bytes);

            Optional<ZeroCopyByteBuffer> result = mount.readResourceZeroCopy("test.class");
            assertThat(result).isPresent();
            assertThat(result.get().length()).isEqualTo(4);
            assertThat(result.get().get(0)).isEqualTo((byte) 0x10);
            assertThat(result.get().toByteArrayCopy()).isEqualTo(bytes);
        }
    }
}
