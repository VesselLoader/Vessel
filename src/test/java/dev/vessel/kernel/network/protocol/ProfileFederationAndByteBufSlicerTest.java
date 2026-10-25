package dev.vessel.kernel.network.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.network.profile.FederatedGameProfile;
import dev.vessel.kernel.network.profile.ProfileProperty;
import dev.vessel.kernel.network.profile.UniversalProfileFederationBridge;
import dev.vessel.kernel.vfs.ZeroCopyByteBuffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UniversalProfileFederationBridge} and {@link ZeroCopyNettyByteBufSlicer}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class ProfileFederationAndByteBufSlicerTest {

    @Test
    @DisplayName("UniversalProfileFederationBridge resolves Floodgate Bedrock UUIDs without network blocking")
    void testProfileFederationBridgeBedrockResolution() {
        UniversalProfileFederationBridge bridge = new UniversalProfileFederationBridge();

        // Floodgate UUID has 0L as most significant bits
        UUID floodgateUuid = new UUID(0L, 123456789L);
        UUID javaUuid = UUID.randomUUID();

        assertTrue(bridge.isFloodgateBedrockUuid(floodgateUuid));
        assertFalse(bridge.isFloodgateBedrockUuid(javaUuid));

        // Unknown Java UUID returns null
        assertNull(bridge.resolveProfile(javaUuid, "JavaPlayer"));

        // Floodgate UUID synthesizes profile instantaneously
        FederatedGameProfile bedrockProfile = bridge.resolveProfile(floodgateUuid, "BedrockTester");
        assertNotNull(bedrockProfile);
        assertEquals(floodgateUuid, bedrockProfile.id());
        assertEquals(".BedrockTester", bedrockProfile.name());
        assertTrue(bedrockProfile.isBedrock());
        assertEquals(1L, bridge.getSyntheticGenerations());

        // Second lookup hits the cache
        FederatedGameProfile cachedProfile = bridge.resolveProfile(floodgateUuid, "BedrockTester");
        assertEquals(bedrockProfile, cachedProfile);
        assertEquals(1L, bridge.getCacheHits());

        // Resolve by name
        FederatedGameProfile byName = bridge.resolveByName(".bedrocktester");
        assertNotNull(byName);
        assertEquals(floodgateUuid, byName.id());

        // Invalidate profile
        FederatedGameProfile evicted = bridge.invalidate(floodgateUuid);
        assertNotNull(evicted);
        assertNull(bridge.resolveByName(".BedrockTester"));
        assertEquals(0, bridge.getCachedProfileCount());

        // Cache clear
        bridge.cacheProfile(new FederatedGameProfile(javaUuid, "CustomUser", List.of(new ProfileProperty("skin", "data")), false));
        assertEquals(1, bridge.getCachedProfileCount());
        bridge.clearCache();
        assertEquals(0, bridge.getCachedProfileCount());
    }

    @Test
    @DisplayName("ZeroCopyNettyByteBufSlicer provides zero-allocation buffer slicing and telemetry")
    void testZeroCopyByteBufSlicing() {
        ZeroCopyNettyByteBufSlicer slicer = new ZeroCopyNettyByteBufSlicer();

        // Standard NIO ByteBuffer slicing
        ByteBuffer buffer = ByteBuffer.allocate(32);
        for (int i = 0; i < 32; i++) {
            buffer.put((byte) i);
        }

        ByteBuffer slice1 = slicer.slice(buffer, 8, 16);
        assertEquals(16, slice1.remaining());
        assertEquals(8, slice1.get(0));
        assertEquals(23, slice1.get(15));

        ByteBuffer retainedSlice = slicer.retainedSlice(buffer, 0, 4);
        assertEquals(4, retainedSlice.remaining());
        assertEquals(0, retainedSlice.get(0));

        // ZeroCopyByteBuffer slicing
        byte[] rawBytes = new byte[]{10, 20, 30, 40, 50};
        ZeroCopyByteBuffer zeroCopyBuf = ZeroCopyByteBuffer.wrap(rawBytes);
        ZeroCopyByteBuffer slicedZeroCopy = slicer.slice(zeroCopyBuf, 1, 3);
        assertEquals(3, slicedZeroCopy.length());
        assertEquals(20, slicedZeroCopy.get(0));
        assertEquals(40, slicedZeroCopy.get(2));

        // Bounds validation
        assertThrows(IndexOutOfBoundsException.class, () -> slicer.slice(buffer, -1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> slicer.slice(buffer, 0, 33));
        assertThrows(IndexOutOfBoundsException.class, () -> slicer.slice(zeroCopyBuf, 2, 4));

        // Telemetry metrics
        ByteBufSliceMetrics metrics = slicer.getMetrics();
        assertEquals(3L, metrics.sliceOperationsTotal());
        assertEquals(16L + 4L + 3L, metrics.totalBytesSliced());
        assertEquals(3L, metrics.heapAllocationsSaved());

        slicer.resetMetrics();
        assertEquals(0L, slicer.getMetrics().sliceOperationsTotal());
    }
}
