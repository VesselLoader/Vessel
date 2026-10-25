package dev.vessel.kernel.test;

import dev.vessel.kernel.display.model.CompactModelMeshDefinition;
import dev.vessel.kernel.network.bedrock.BedrockBatchPacketCompressor;
import dev.vessel.kernel.network.bedrock.BedrockRuntimeEntityIdIndex;
import dev.vessel.kernel.resourcepack.bedrock.BedrockGeometryModelBridge;
import dev.vessel.kernel.resourcepack.bedrock.BedrockManifestGenerator;
import dev.vessel.kernel.resourcepack.bedrock.BedrockTextureMetadata;
import dev.vessel.kernel.resourcepack.bedrock.JavaToBedrockTextureConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for Bedrock Resource Pack compilation (Phase 14) and
 * Bedrock Network Packet Batching & Entity Routing (Phase 21).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class BedrockResourcePackAndNetworkTest {

    // Helper: generate minimal mock PNG byte array with specified width and height
    private static byte[] createMockPng(int width, int height) {
        byte[] data = new byte[33];
        // PNG Signature (8 bytes)
        data[0] = (byte) 0x89;
        data[1] = 0x50; // P
        data[2] = 0x4E; // N
        data[3] = 0x47; // G
        data[4] = 0x0D;
        data[5] = 0x0A;
        data[6] = 0x1A;
        data[7] = 0x0A;

        // IHDR chunk length (13)
        data[8] = 0; data[9] = 0; data[10] = 0; data[11] = 13;
        // IHDR chunk type
        data[12] = 'I'; data[13] = 'H'; data[14] = 'D'; data[15] = 'R';

        // Width (big-endian)
        data[16] = (byte) ((width >>> 24) & 0xFF);
        data[17] = (byte) ((width >>> 16) & 0xFF);
        data[18] = (byte) ((width >>> 8) & 0xFF);
        data[19] = (byte) (width & 0xFF);

        // Height (big-endian)
        data[20] = (byte) ((height >>> 24) & 0xFF);
        data[21] = (byte) ((height >>> 16) & 0xFF);
        data[22] = (byte) ((height >>> 8) & 0xFF);
        data[23] = (byte) (height & 0xFF);

        // Bit depth & color type
        data[24] = 8;
        data[25] = 6; // RGBA
        return data;
    }

    @Test
    @DisplayName("JavaToBedrockTextureConverter converts Java paths to standard Bedrock paths")
    void testTexturePathConversion() {
        JavaToBedrockTextureConverter converter = new JavaToBedrockTextureConverter();

        // Vanilla paths: block -> blocks, item -> items
        assertThat(converter.convertPath("assets/minecraft/textures/block/stone.png"))
                .isEqualTo("textures/blocks/stone.png");
        assertThat(converter.convertPath("assets/minecraft/textures/item/diamond_sword.png"))
                .isEqualTo("textures/items/diamond_sword.png");
        assertThat(converter.convertPath("assets/minecraft/textures/entity/zombie/zombie.png"))
                .isEqualTo("textures/entity/zombie/zombie.png");

        // Custom mod namespace paths
        assertThat(converter.convertPath("assets/techmod/textures/block/cable.png"))
                .isEqualTo("textures/techmod/blocks/cable.png");
        assertThat(converter.convertPath("assets/techmod/textures/item/wrench.png"))
                .isEqualTo("textures/techmod/items/wrench.png");

        // Windows separator normalization
        assertThat(converter.convertPath("assets\\techmod\\textures\\block\\pipe.png"))
                .isEqualTo("textures/techmod/blocks/pipe.png");

        // Invalid paths
        assertThatThrownBy(() -> converter.convertPath("invalid/path/test.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> converter.convertPath("assets/minecraft/models/test.json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("JavaToBedrockTextureConverter validates PNG and parses .mcmeta animations")
    void testTextureConversionWithAnimation() {
        JavaToBedrockTextureConverter converter = new JavaToBedrockTextureConverter();

        // 16x64 PNG -> 4 vertical frames of 16x16
        byte[] pngData = createMockPng(16, 64);
        assertThat(converter.isValidPng(pngData)).isTrue();
        int[] dims = converter.extractPngDimensions(pngData);
        assertThat(dims).containsExactly(16, 64);

        String mcmetaJson = "{\"animation\": {\"frametime\": 3, \"interpolate\": true}}";
        BedrockTextureMetadata meta = converter.convertTexture(
                "assets/minecraft/textures/block/water_flow.png",
                pngData,
                mcmetaJson
        );

        assertThat(meta.animated()).isTrue();
        assertThat(meta.frameCount()).isEqualTo(4);
        assertThat(meta.ticksPerFrame()).isEqualTo(3);
        assertThat(meta.interpolate()).isTrue();
        assertThat(meta.bedrockPath()).isEqualTo("textures/blocks/water_flow.png");

        // Flipbook textures JSON generation
        String flipbookJson = converter.generateFlipbookTexturesJson(List.of(meta));
        assertThat(flipbookJson).contains("\"flipbook_texture\": \"textures/blocks/water_flow\"");
        assertThat(flipbookJson).contains("\"ticks_per_frame\": 3");
        assertThat(flipbookJson).contains("\"blend_frames\": true");
        assertThat(flipbookJson).contains("\"frames\": [0, 1, 2, 3]");
    }

    @Test
    @DisplayName("BedrockManifestGenerator generates deterministic UUID v5 and compliant manifest.json")
    void testBedrockManifestGenerator() {
        BedrockManifestGenerator generator = new BedrockManifestGenerator();

        UUID uuid1 = BedrockManifestGenerator.generateDeterministicUuid(
                BedrockManifestGenerator.VESSEL_BEDROCK_NAMESPACE,
                "vessel.test.module"
        );
        UUID uuid2 = BedrockManifestGenerator.generateDeterministicUuid(
                BedrockManifestGenerator.VESSEL_BEDROCK_NAMESPACE,
                "vessel.test.module"
        );
        UUID uuidDifferent = BedrockManifestGenerator.generateDeterministicUuid(
                BedrockManifestGenerator.VESSEL_BEDROCK_NAMESPACE,
                "vessel.other.module"
        );

        // Deterministic check
        assertThat(uuid1).isEqualTo(uuid2);
        assertThat(uuid1).isNotEqualTo(uuidDifferent);
        assertThat(uuid1.version()).isEqualTo(5);

        // Version parsing
        assertThat(BedrockManifestGenerator.parseVersionArray("1.2.3")).containsExactly(1, 2, 3);
        assertThat(BedrockManifestGenerator.parseVersionArray("2.4")).containsExactly(2, 4, 0);

        // Manifest JSON generation
        String manifest = generator.generateManifest(
                "vessel-core-pack",
                "VESSEL Graphics Pack",
                "Unified Resource and Addon Pack for Bedrock Clients",
                new int[]{1, 2, 0},
                new int[]{1, 20, 0}
        );

        assertThat(manifest).contains("\"format_version\": 2");
        assertThat(manifest).contains("\"name\": \"VESSEL Graphics Pack\"");
        assertThat(manifest).contains("\"type\": \"resources\"");
        assertThat(manifest).contains("\"version\": [1, 2, 0]");
        assertThat(manifest).contains("\"min_engine_version\": [1, 20, 0]");
    }

    @Test
    @DisplayName("BedrockGeometryModelBridge converts CompactModelMeshDefinition to geometry.json")
    void testGeometryModelBridge() {
        BedrockGeometryModelBridge bridge = new BedrockGeometryModelBridge();

        float[] vertices = new float[]{
                -8.0f, 0.0f, -8.0f,
                8.0f, 16.0f, 8.0f
        };
        float[] uvs = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
        int[] indices = new int[]{0, 1};

        CompactModelMeshDefinition mesh = new CompactModelMeshDefinition("mymod:custom_block", vertices, uvs, indices);

        String geometryJson = bridge.convertMeshToGeometryJson(mesh, 64, 64, BedrockGeometryModelBridge.SCHEMA_1_16_0);

        assertThat(geometryJson).contains("\"format_version\": \"1.16.0\"");
        assertThat(geometryJson).contains("\"identifier\": \"geometry.mymod.custom_block\"");
        assertThat(geometryJson).contains("\"texture_width\": 64");
        assertThat(geometryJson).contains("\"texture_height\": 64");
        assertThat(geometryJson).contains("\"origin\": [-8.00, 0.00, -8.00]");
        assertThat(geometryJson).contains("\"size\": [16.00, 16.00, 16.00]");
        assertThat(geometryJson).contains("\"pivot\": [0.0, 0.0, 0.0]");
    }

    @Test
    @DisplayName("BedrockRuntimeEntityIdIndex provides zero-allocation bidirectional lookups and rehashes accurately")
    void testRuntimeEntityIdIndex() {
        BedrockRuntimeEntityIdIndex index = new BedrockRuntimeEntityIdIndex(32);

        assertThat(index.isEmpty()).isTrue();
        assertThat(index.size()).isEqualTo(0);

        index.put(101, 1000101L);
        index.put(102, 1000102L);
        index.put(103, 1000103L);

        assertThat(index.size()).isEqualTo(3);
        assertThat(index.getBedrockId(101)).isEqualTo(1000101L);
        assertThat(index.getJavaId(1000101L)).isEqualTo(101);
        assertThat(index.containsJavaId(102)).isTrue();
        assertThat(index.containsBedrockId(1000103L)).isTrue();

        // Absent lookups
        assertThat(index.getBedrockId(999)).isEqualTo(BedrockRuntimeEntityIdIndex.BEDROCK_NOT_FOUND);
        assertThat(index.getJavaId(999999L)).isEqualTo(BedrockRuntimeEntityIdIndex.JAVA_NOT_FOUND);

        // Removal tests
        assertThat(index.removeByJavaId(102)).isTrue();
        assertThat(index.size()).isEqualTo(2);
        assertThat(index.containsJavaId(102)).isFalse();
        assertThat(index.containsBedrockId(1000102L)).isFalse();

        assertThat(index.removeByBedrockId(1000103L)).isTrue();
        assertThat(index.size()).isEqualTo(1);

        // Stress rehash test with 500 entities
        index.clear();
        assertThat(index.isEmpty()).isTrue();

        for (int i = 1; i <= 500; i++) {
            index.put(i, 2_000_000L + i);
        }
        assertThat(index.size()).isEqualTo(500);

        for (int i = 1; i <= 500; i++) {
            assertThat(index.getBedrockId(i)).isEqualTo(2_000_000L + i);
            assertThat(index.getJavaId(2_000_000L + i)).isEqualTo(i);
        }
    }

    @Test
    @DisplayName("BedrockRuntimeEntityIdIndex handles concurrent reads and writes safely")
    void testConcurrentEntityIdIndex() throws InterruptedException {
        BedrockRuntimeEntityIdIndex index = new BedrockRuntimeEntityIdIndex(128);
        int threadCount = 4;
        int operationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadOffset = t * operationsPerThread;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        int javaId = threadOffset + i;
                        long bedrockId = (threadOffset + i) * 100L;
                        index.put(javaId, bedrockId);

                        long retrievedBedrock = index.getBedrockId(javaId);
                        int retrievedJava = index.getJavaId(bedrockId);
                        if (retrievedBedrock != bedrockId || retrievedJava != javaId) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(failures.get()).isEqualTo(0);
        assertThat(index.size()).isEqualTo(threadCount * operationsPerThread);
    }

    @Test
    @DisplayName("BedrockBatchPacketCompressor frames, compresses, and uncompresses packet batches")
    void testBatchPacketCompressor() {
        BedrockBatchPacketCompressor compressor = new BedrockBatchPacketCompressor();

        byte[] packet1 = "MOVE:x=10.5,y=64.0,z=-20.0".getBytes(StandardCharsets.UTF_8);
        byte[] packet2 = "BLOCK_UPDATE:pos=[10,64,-20],state=minecraft:stone".getBytes(StandardCharsets.UTF_8);
        byte[] packet3 = "CHAT_MESSAGE:user=Shiroz,msg=HelloBedrock".getBytes(StandardCharsets.UTF_8);

        byte[][] packets = new byte[][]{packet1, packet2, packet3};

        // Assemble and compress
        byte[] compressedBatch = compressor.assembleAndCompressBatch(packets);
        assertThat(compressedBatch).isNotEmpty();

        // Decompress and unpack
        List<byte[]> unpacked = compressor.decompressAndUnpackBatch(compressedBatch);
        assertThat(unpacked).hasSize(3);
        assertThat(unpacked.get(0)).containsExactly(packet1);
        assertThat(unpacked.get(1)).containsExactly(packet2);
        assertThat(unpacked.get(2)).containsExactly(packet3);

        // Compression levels check
        byte[] compressedLevel1 = compressor.assembleAndCompressBatch(packets, 1);
        byte[] compressedLevel9 = compressor.assembleAndCompressBatch(packets, 9);
        assertThat(compressedLevel1).isNotEmpty();
        assertThat(compressedLevel9).isNotEmpty();

        // Verify level 1 decompress
        List<byte[]> unpackedL1 = compressor.decompressAndUnpackBatch(compressedLevel1);
        assertThat(unpackedL1).hasSize(3);

        // Security / Safety limit check
        assertThatThrownBy(() -> compressor.decompressRawBatch(compressedBatch, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeded maximum limit");

        // Corrupted payload check
        assertThatThrownBy(() -> compressor.decompressRawBatch(new byte[]{1, 2, 3, 4, 5}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("BedrockRuntimeEntityIdIndex preserves size on mapping updates and handles tombstones")
    void testEntityIdIndexUpdateAndSizeLeak() {
        BedrockRuntimeEntityIdIndex index = new BedrockRuntimeEntityIdIndex(32);
        index.put(1, 100L);
        assertThat(index.size()).isEqualTo(1);

        // Update value for existing key
        index.put(1, 200L);
        assertThat(index.size()).isEqualTo(1);
        assertThat(index.getBedrockId(1)).isEqualTo(200L);
        assertThat(index.getJavaId(100L)).isEqualTo(BedrockRuntimeEntityIdIndex.JAVA_NOT_FOUND);
        assertThat(index.getJavaId(200L)).isEqualTo(1);

        // Reassign bedrockId to a different javaId
        index.put(2, 200L);
        assertThat(index.size()).isEqualTo(1);
        assertThat(index.getJavaId(200L)).isEqualTo(2);
        assertThat(index.getBedrockId(1)).isEqualTo(BedrockRuntimeEntityIdIndex.BEDROCK_NOT_FOUND);

        // Merge two existing mappings: (10, 1000) and (20, 2000)
        index.put(10, 1000L);
        index.put(20, 2000L);
        assertThat(index.size()).isEqualTo(3);
        index.put(10, 2000L); // javaId 10 now gets bedrockId 2000
        assertThat(index.size()).isEqualTo(2);
        assertThat(index.getBedrockId(10)).isEqualTo(2000L);
        assertThat(index.getJavaId(1000L)).isEqualTo(BedrockRuntimeEntityIdIndex.JAVA_NOT_FOUND);
        assertThat(index.containsJavaId(20)).isFalse();
    }

    @Test
    @DisplayName("BedrockBatchPacketCompressor rejects truncated or incomplete compressed streams")
    void testBatchPacketCompressorTruncatedData() {
        BedrockBatchPacketCompressor compressor = new BedrockBatchPacketCompressor();
        byte[] payload = "SAMPLE_LARGE_PACKET_PAYLOAD_FOR_COMPRESSION_VALIDATION_1234567890".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compressRawBatch(payload);

        // Truncate compressed payload
        byte[] truncated = java.util.Arrays.copyOf(compressed, compressed.length - 8);
        assertThatThrownBy(() -> compressor.decompressRawBatch(truncated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Truncated or incomplete");
    }

    @Test
    @DisplayName("BedrockBatchPacketCompressor wraps and unwraps RakNet 0xFE batch packets")
    void testBatchPacketCompressorRakNetFraming() {
        BedrockBatchPacketCompressor compressor = new BedrockBatchPacketCompressor();
        byte[] packet1 = "ENTITY_MOVE:id=42,x=100,y=64,z=200".getBytes(StandardCharsets.UTF_8);
        byte[][] packets = new byte[][]{packet1};

        byte[] compressed = compressor.assembleAndCompressBatch(packets);
        byte[] rakNetPacket = compressor.wrapBatchPacket(compressed);

        assertThat(compressor.isBatchPacket(rakNetPacket)).isTrue();
        assertThat(compressor.isBatchPacket(compressed)).isFalse();
        assertThat(rakNetPacket[0]).isEqualTo(BedrockBatchPacketCompressor.BEDROCK_BATCH_PACKET_ID);

        List<byte[]> unwrapped = compressor.unwrapAndDecompressBatchPacket(rakNetPacket);
        assertThat(unwrapped).hasSize(1);
        assertThat(unwrapped.get(0)).containsExactly(packet1);

        // Invalid packet header check
        byte[] invalidHeaderPacket = new byte[]{(byte) 0x80, 1, 2, 3};
        assertThatThrownBy(() -> compressor.unwrapAndDecompressBatchPacket(invalidHeaderPacket))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Bedrock batch packet header");
    }

    @Test
    @DisplayName("JavaToBedrockTextureConverter validates IHDR chunk and parses custom mcmeta frame sequences")
    void testTextureConverterCustomFramesAndIhdr() {
        JavaToBedrockTextureConverter converter = new JavaToBedrockTextureConverter();

        // Malformed PNG: magic valid, but IHDR chunk corrupted/missing
        byte[] badPng = createMockPng(16, 16);
        badPng[12] = 'J'; badPng[13] = 'U'; badPng[14] = 'N'; badPng[15] = 'K';
        assertThatThrownBy(() -> converter.extractPngDimensions(badPng))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing mandatory IHDR");

        // Particle path conversion
        assertThat(converter.convertPath("assets/minecraft/textures/particle/flame.png"))
                .isEqualTo("textures/particles/flame.png");

        // Animation with custom frame sequence and frame objects
        byte[] validPng = createMockPng(16, 64);
        String mcmetaWithCustomFrames = "{\"animation\": {\"frametime\": 2, \"interpolate\": true, \"frames\": [0, 1, {\"index\": 2, \"time\": 4}, 1]}}";
        BedrockTextureMetadata meta = converter.convertTexture(
                "assets/minecraft/textures/block/fire.png",
                validPng,
                mcmetaWithCustomFrames
        );

        assertThat(meta.animated()).isTrue();
        assertThat(meta.customFrames()).containsExactly(0, 1, 2, 1);
        String flipbookJson = converter.generateFlipbookTexturesJson(List.of(meta));
        assertThat(flipbookJson).contains("\"frames\": [0, 1, 2, 1]");
    }

    @Test
    @DisplayName("BedrockGeometryModelBridge validates modelId and BedrockManifestGenerator escapes control chars")
    void testBridgeAndManifestValidation() {
        BedrockGeometryModelBridge bridge = new BedrockGeometryModelBridge();

        assertThatThrownBy(() -> bridge.sanitizeIdentifier(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bridge.sanitizeIdentifier("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bridge.sanitizeIdentifier(":::"))
                .isInstanceOf(IllegalArgumentException.class);

        BedrockManifestGenerator manifestGen = new BedrockManifestGenerator();
        String manifest = manifestGen.generateManifest(
                "ctrl-pack",
                "Pack\tName",
                "Line1\nLine2\u0001WithControl"
        );
        assertThat(manifest).contains("Line1\\nLine2\\u0001WithControl");
    }
}
