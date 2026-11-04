package dev.vessel.kernel.test;

import dev.vessel.bootstrap.ecosystem.BuiltInGeyserBedrockBridgeService;
import dev.vessel.bootstrap.target.VesselResourcePackDeliveryService;
import dev.vessel.bootstrap.target.VesselServerConfig;
import dev.vessel.kernel.network.NetworkPacketBatcher;
import dev.vessel.kernel.network.PacketProtocolFamily;
import dev.vessel.kernel.network.ZeroCopyPacketFrame;
import dev.vessel.kernel.network.bridge.BedrockRakNetPortBinder;
import dev.vessel.kernel.network.protocol.ZeroCopyNettyByteBufSlicer;
import dev.vessel.kernel.vfs.ZeroCopyByteBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification test suite for Phase 5 (Network, Bedrock Protocol Bridge & Asset Streaming).
 * Tests zero-allocation bitwise batching, dynamic port resolution, and zero-copy slicing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class NetworkAndDynamicPortDeliveryTest {

    @Test
    @DisplayName("NetworkPacketBatcher batches multiple frames with zero-allocation bitwise copy")
    void testZeroAllocationNetworkPacketBatcher() {
        NetworkPacketBatcher batcher = new NetworkPacketBatcher();

        byte[] payload1 = "Hello".getBytes(StandardCharsets.UTF_8);
        byte[] payload2 = "World123".getBytes(StandardCharsets.UTF_8);

        ZeroCopyPacketFrame frame1 = ZeroCopyPacketFrame.of(42, PacketProtocolFamily.JAVA_NATIVE, payload1);
        ZeroCopyPacketFrame frame2 = ZeroCopyPacketFrame.of(99, PacketProtocolFamily.JAVA_NATIVE, payload2);

        batcher.queuePacket(frame1);
        batcher.queuePacket(frame2);

        assertThat(batcher.queuedCount()).isEqualTo(2);

        byte[] batch = batcher.flushToBatch();
        assertThat(batcher.queuedCount()).isEqualTo(0);

        // Expected length: (6 + 5) + (6 + 8) = 25 bytes
        int expectedLen = (6 + payload1.length) + (6 + payload2.length);
        assertThat(batch.length).isEqualTo(expectedLen);

        ByteBuffer buf = ByteBuffer.wrap(batch);

        // Verify frame 1
        assertThat(buf.getShort()).isEqualTo((short) 42);
        assertThat(buf.getInt()).isEqualTo(payload1.length);
        byte[] read1 = new byte[payload1.length];
        buf.get(read1);
        assertThat(new String(read1, StandardCharsets.UTF_8)).isEqualTo("Hello");

        // Verify frame 2
        assertThat(buf.getShort()).isEqualTo((short) 99);
        assertThat(buf.getInt()).isEqualTo(payload2.length);
        byte[] read2 = new byte[payload2.length];
        buf.get(read2);
        assertThat(new String(read2, StandardCharsets.UTF_8)).isEqualTo("World123");
    }

    @Test
    @DisplayName("ZeroCopyByteBuffer copyTo transfers bytes accurately")
    void testZeroCopyByteBufferCopyTo() {
        byte[] src = "MinecraftPacketData".getBytes(StandardCharsets.UTF_8);
        ZeroCopyByteBuffer buffer = ZeroCopyByteBuffer.wrap(src, 9, 10); // "PacketData"

        byte[] dest = new byte[20];
        buffer.copyTo(dest, 5);

        byte[] extracted = new byte[10];
        System.arraycopy(dest, 5, extracted, 0, 10);
        assertThat(new String(extracted, StandardCharsets.UTF_8)).isEqualTo("PacketData");
    }

    @Test
    @DisplayName("Dynamic Port resolution in VesselResourcePackDeliveryService and Geyser Bridge")
    void testDynamicPortResolution() {
        VesselResourcePackDeliveryService deliveryService = new VesselResourcePackDeliveryService();
        int activeHttpPort = deliveryService.getActiveHttpPort();
        assertThat(activeHttpPort).isBetween(1, 65535);

        deliveryService.setActiveHttpPort(28888);
        assertThat(deliveryService.getActiveHttpPort()).isEqualTo(28888);

        BuiltInGeyserBedrockBridgeService geyserBridge = new BuiltInGeyserBedrockBridgeService();
        assertThat(geyserBridge.getActiveBedrockUdpPort()).isBetween(1, 65535);

        BedrockRakNetPortBinder binder = new BedrockRakNetPortBinder();
        boolean bound = binder.bindOrYieldToExternalGeyser(false);
        assertThat(bound).isTrue();
        assertThat(binder.getBoundUdpPort()).isBetween(1, 65535);

        boolean yielded = binder.bindOrYieldToExternalGeyser(true);
        assertThat(yielded).isFalse();
        assertThat(binder.getBoundUdpPort()).isEqualTo(-1);
    }
}
