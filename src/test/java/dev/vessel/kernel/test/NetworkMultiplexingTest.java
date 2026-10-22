package dev.vessel.kernel.test;

import dev.vessel.kernel.network.NetworkPacketBatcher;
import dev.vessel.kernel.network.PacketCompressionEngine;
import dev.vessel.kernel.network.PacketProtocolFamily;
import dev.vessel.kernel.network.ZeroCopyPacketFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for network packet multiplexing, zero-copy frames, and compression.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public class NetworkMultiplexingTest {

    @Test
    @DisplayName("ZeroCopyPacketFrame binds packet id, protocol family, and immutable payload")
    void testZeroCopyPacketFrame() {
        byte[] payload = "PACKET_DATA_CHUNK".getBytes(StandardCharsets.UTF_8);
        ZeroCopyPacketFrame frame = ZeroCopyPacketFrame.of(0x24, PacketProtocolFamily.JAVA_NATIVE, payload);

        assertThat(frame.packetId()).isEqualTo(0x24);
        assertThat(frame.family()).isEqualTo(PacketProtocolFamily.JAVA_NATIVE);
        assertThat(frame.payloadLength()).isEqualTo(payload.length);
        assertThat(frame.payload().toByteArrayCopy()).isEqualTo(payload);
    }

    @Test
    @DisplayName("PacketCompressionEngine compresses large packets and restores original bytes")
    void testPacketCompression() {
        PacketCompressionEngine engine = new PacketCompressionEngine(64);

        // 1. Below threshold: should not compress
        assertThat(engine.shouldCompress(32)).isFalse();

        // 2. Above threshold: compress and decompress
        byte[] largeData = new byte[1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 32);
        }

        assertThat(engine.shouldCompress(largeData.length)).isTrue();
        byte[] compressed = engine.compress(largeData);
        assertThat(compressed.length).isLessThan(largeData.length / 2);

        byte[] decompressed = engine.decompress(compressed, largeData.length);
        assertThat(decompressed).isEqualTo(largeData);
    }

    @Test
    @DisplayName("NetworkPacketBatcher coalesces multiple outgoing frames into a single buffer")
    void testPacketBatcher() {
        NetworkPacketBatcher batcher = new NetworkPacketBatcher();
        assertThat(batcher.hasPendingPackets()).isFalse();

        ZeroCopyPacketFrame frame1 = ZeroCopyPacketFrame.of(0x01, PacketProtocolFamily.JAVA_NATIVE, new byte[]{1, 2, 3});
        ZeroCopyPacketFrame frame2 = ZeroCopyPacketFrame.of(0x02, PacketProtocolFamily.JAVA_NATIVE, new byte[]{4, 5});

        batcher.queuePacket(frame1);
        batcher.queuePacket(frame2);

        assertThat(batcher.queuedCount()).isEqualTo(2);
        assertThat(batcher.hasPendingPackets()).isTrue();

        byte[] batchedPayload = batcher.flushToBatch();
        // Each frame has 6 bytes header (2 bytes packetId + 4 bytes length) + payload
        // Frame 1: 6 + 3 = 9 bytes
        // Frame 2: 6 + 2 = 8 bytes
        // Total batched = 17 bytes
        assertThat(batchedPayload.length).isEqualTo(17);
        assertThat(batcher.queuedCount()).isEqualTo(0);
        assertThat(batcher.hasPendingPackets()).isFalse();
    }
}
