package dev.vessel.kernel.test;

import dev.vessel.kernel.resourcepack.PackChunkStreamer;
import dev.vessel.kernel.resourcepack.PackTargetPlatform;
import dev.vessel.kernel.resourcepack.VirtualResourcePack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for VirtualResourcePack and chunked streaming engine.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public class ResourcePackStreamingTest {

    @Test
    @DisplayName("VirtualResourcePack computes SHA-1 checksum automatically")
    void testVirtualResourcePackCreation() {
        byte[] dummyZip = "PK\003\004_DUMMY_ZIP_CONTENT_FOR_TESTING".getBytes(StandardCharsets.UTF_8);
        VirtualResourcePack pack = VirtualResourcePack.of("vessel-main", "VESSEL Server Assets", PackTargetPlatform.JAVA_EDITION, dummyZip);

        assertThat(pack.packId()).isEqualTo("vessel-main");
        assertThat(pack.name()).isEqualTo("VESSEL Server Assets");
        assertThat(pack.platform()).isEqualTo(PackTargetPlatform.JAVA_EDITION);
        assertThat(pack.sizeBytes()).isEqualTo(dummyZip.length);
        assertThat(pack.sha1Hex()).isNotNull().hasSize(40); // Standard 40-char SHA-1 hex
    }

    @Test
    @DisplayName("PackChunkStreamer slices archive into chunks for network streaming without full copies")
    void testPackChunkStreaming() {
        byte[] data = new byte[1000];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 128);
        }

        VirtualResourcePack pack = VirtualResourcePack.of("addon-bedrock", "Addon", PackTargetPlatform.BEDROCK_EDITION, data);
        PackChunkStreamer streamer = new PackChunkStreamer();

        int chunkSize = 300;
        // 1000 bytes with chunkSize 300 -> 4 chunks (300, 300, 300, 100)
        int chunkCount = streamer.calculateChunkCount(pack, chunkSize);
        assertThat(chunkCount).isEqualTo(4);

        byte[] c0 = streamer.getChunkSlice(pack, 0, chunkSize);
        assertThat(c0.length).isEqualTo(300);

        byte[] c1 = streamer.getChunkSlice(pack, 1, chunkSize);
        assertThat(c1.length).isEqualTo(300);

        byte[] c2 = streamer.getChunkSlice(pack, 2, chunkSize);
        assertThat(c2.length).isEqualTo(300);

        byte[] c3 = streamer.getChunkSlice(pack, 3, chunkSize);
        assertThat(c3.length).isEqualTo(100);

        // Verify SHA-1 integrity check
        assertThat(streamer.verifyIntegrity(pack, pack.sha1Hex())).isTrue();
        assertThat(streamer.verifyIntegrity(pack, "0000000000000000000000000000000000000000")).isFalse();

        // Index out of bounds
        assertThatThrownBy(() -> streamer.getChunkSlice(pack, 5, chunkSize))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
