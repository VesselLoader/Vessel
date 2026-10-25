package dev.vessel.kernel.asset.stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive integration and unit tests for HTTP Range Streaming,
 * Lossless Pack Compression, and Dual-Generation Asset Builders.
 * Verifies extinguishing of AST-NET, AST-OPT, and AST-MOD.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class HttpRangeStreamAndCompressionTest {

    private HttpRangeChunkStreamServer server;
    private HttpClient httpClient;
    private byte[] testPayloadBytes;
    private String testSha1;

    @BeforeEach
    public void setUp() throws IOException {
        // Prepare 128 KB structured test payload
        testPayloadBytes = new byte[131072];
        for (int i = 0; i < testPayloadBytes.length; i++) {
            testPayloadBytes[i] = (byte) (i % 251); // pseudo-random deterministic bytes
        }
        testSha1 = ZstdLosslessPackCompressor.computeSha1Hex(testPayloadBytes);

        // Configure server with dynamic ephemeral port (0) for parallel-safe execution
        HttpRangeServerConfig config = new HttpRangeServerConfig(
                "127.0.0.1",
                0,
                8192,
                "/",
                64
        );
        InMemoryStreamableAssetRepository repository = new InMemoryStreamableAssetRepository();
        server = new HttpRangeChunkStreamServer(config, repository);

        StreamableAssetResource resource = new StreamableAssetResource(
                "/packs/vessel-pack.zip",
                "application/zip",
                testPayloadBytes,
                testSha1,
                System.currentTimeMillis()
        );
        server.registerAsset(resource);
        server.start();

        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @AfterEach
    public void tearDown() {
        if (server != null && server.isRunning()) {
            server.stop(1);
        }
    }

    @Test
    @DisplayName("Should serve full resource payload with 200 OK when no Range header is sent")
    public void testFullPayloadDownload() throws IOException, InterruptedException {
        URI targetUri = URI.create(server.getBaseUrl() + "/packs/vessel-pack.zip");
        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Accept-Ranges")).contains("bytes");
        assertThat(response.headers().firstValue("Content-Type")).contains("application/zip");
        assertThat(response.headers().firstValue("Content-Length")).contains(String.valueOf(testPayloadBytes.length));
        assertThat(response.headers().firstValue("ETag")).contains("\"" + testSha1 + "\"");
        assertThat(response.body()).isEqualTo(testPayloadBytes);
    }

    @Test
    @DisplayName("Should serve partial byte chunk with 206 Partial Content when Range header is provided")
    public void testPartialByteRangeRequest() throws IOException, InterruptedException {
        URI targetUri = URI.create(server.getBaseUrl() + "/packs/vessel-pack.zip");
        // Request first 1024 bytes (0 to 1023 inclusive)
        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .header("Range", "bytes=0-1023")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(206);
        assertThat(response.headers().firstValue("Accept-Ranges")).contains("bytes");
        assertThat(response.headers().firstValue("Content-Range")).contains("bytes 0-1023/" + testPayloadBytes.length);
        assertThat(response.headers().firstValue("Content-Length")).contains("1024");

        byte[] expectedSlice = Arrays.copyOfRange(testPayloadBytes, 0, 1024);
        assertThat(response.body()).isEqualTo(expectedSlice);
    }

    @Test
    @DisplayName("Should serve open-ended range from offset to EOF")
    public void testOpenEndedRangeRequest() throws IOException, InterruptedException {
        URI targetUri = URI.create(server.getBaseUrl() + "/packs/vessel-pack.zip");
        int offset = 100000;
        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .header("Range", "bytes=" + offset + "-")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        int expectedLength = testPayloadBytes.length - offset;
        assertThat(response.statusCode()).isEqualTo(206);
        assertThat(response.headers().firstValue("Content-Range"))
                .contains("bytes " + offset + "-" + (testPayloadBytes.length - 1) + "/" + testPayloadBytes.length);
        assertThat(response.headers().firstValue("Content-Length")).contains(String.valueOf(expectedLength));

        byte[] expectedSlice = Arrays.copyOfRange(testPayloadBytes, offset, testPayloadBytes.length);
        assertThat(response.body()).isEqualTo(expectedSlice);
    }

    @Test
    @DisplayName("Should serve suffix byte range (last N bytes)")
    public void testSuffixByteRangeRequest() throws IOException, InterruptedException {
        URI targetUri = URI.create(server.getBaseUrl() + "/packs/vessel-pack.zip");
        int suffixCount = 512;
        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .header("Range", "bytes=-" + suffixCount)
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        int expectedStart = testPayloadBytes.length - suffixCount;
        assertThat(response.statusCode()).isEqualTo(206);
        assertThat(response.headers().firstValue("Content-Range"))
                .contains("bytes " + expectedStart + "-" + (testPayloadBytes.length - 1) + "/" + testPayloadBytes.length);
        assertThat(response.headers().firstValue("Content-Length")).contains(String.valueOf(suffixCount));

        byte[] expectedSlice = Arrays.copyOfRange(testPayloadBytes, expectedStart, testPayloadBytes.length);
        assertThat(response.body()).isEqualTo(expectedSlice);
    }

    @Test
    @DisplayName("Should return 416 Range Not Satisfiable when range is beyond file size")
    public void testOutOfRangeRequest() throws IOException, InterruptedException {
        URI targetUri = URI.create(server.getBaseUrl() + "/packs/vessel-pack.zip");
        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .header("Range", "bytes=9999999-10000000")
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(response.statusCode()).isEqualTo(416);
        assertThat(response.headers().firstValue("Content-Range"))
                .contains("bytes */" + testPayloadBytes.length);
    }

    @Test
    @DisplayName("Should handle HEAD requests without returning body")
    public void testHeadRequest() throws IOException, InterruptedException {
        URI targetUri = URI.create(server.getBaseUrl() + "/packs/vessel-pack.zip");
        HttpRequest request = HttpRequest.newBuilder(targetUri)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Accept-Ranges")).contains("bytes");
        assertThat(response.headers().firstValue("Content-Length")).contains(String.valueOf(testPayloadBytes.length));
    }

    @Test
    @DisplayName("Should compress and decompress data losslessly using Deflate Ultra and Zstandard Level 19")
    public void testCompressionRoundTripLossless() throws IOException {
        ZstdLosslessPackCompressor compressor = new ZstdLosslessPackCompressor();

        // 1. Deflate Optimal
        CompressedAssetPayload deflatePayload = compressor.compressData(testPayloadBytes, PackCompressionAlgorithm.DEFLATE_OPTIMAL);
        assertThat(deflatePayload.compressedSizeBytes()).isGreaterThan(0);
        byte[] restoredDeflate = compressor.decompressData(deflatePayload);
        assertThat(restoredDeflate).isEqualTo(testPayloadBytes);

        // 2. Zstandard Level 19
        CompressedAssetPayload zstdPayload = compressor.compressData(testPayloadBytes, PackCompressionAlgorithm.ZSTD_LEVEL_19);
        assertThat(zstdPayload.compressedSizeBytes()).isGreaterThan(0);
        byte[] restoredZstd = compressor.decompressData(zstdPayload);
        assertThat(restoredZstd).isEqualTo(testPayloadBytes);
    }

    @Test
    @DisplayName("Should compress multiple virtual files into a valid client-compliant ZIP archive")
    public void testVirtualPackZipGeneration() throws IOException {
        ZstdLosslessPackCompressor compressor = new ZstdLosslessPackCompressor();

        Map<String, byte[]> virtualFiles = Map.of(
                "pack.mcmeta", "{\"pack\":{\"pack_format\":34,\"description\":\"Test Pack\"}}".getBytes(StandardCharsets.UTF_8),
                "assets/minecraft/models/item/diamond_sword.json", "{\"parent\":\"item/handheld\"}".getBytes(StandardCharsets.UTF_8)
        );

        byte[] zipBytes = compressor.compressResourcePackZip(virtualFiles);
        assertThat(zipBytes).isNotEmpty();

        // Verify ZIP integrity by reading back entries
        int entryCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                assertThat(virtualFiles).containsKey(entry.getName());
                entryCount++;
                zis.closeEntry();
            }
        }
        assertThat(entryCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should generate dual pack for 1.20.1-1.20.4 (overrides) and 1.20.5+ (item_model) from canonical model")
    public void testClientAssetDualGenerationPackBuilder() throws IOException, InterruptedException {
        ClientAssetDualGenerationPackBuilder builder = new ClientAssetDualGenerationPackBuilder();

        ItemModelSourceDefinition frostBlade = new ItemModelSourceDefinition(
                "minecraft:diamond_sword",
                10001,
                "vessel:item/frost_blade",
                "minecraft:item/handheld",
                Map.of("layer0", "vessel:item/frost_blade")
        );

        ItemModelSourceDefinition flameBlade = new ItemModelSourceDefinition(
                "minecraft:diamond_sword",
                10002,
                "vessel:item/flame_blade",
                "minecraft:item/handheld",
                Map.of("layer0", "vessel:item/flame_blade")
        );

        builder.addModelDefinition(frostBlade);
        builder.addModelDefinition(flameBlade);

        DualGenerationPackArtifact artifact = builder.compileDualPack("VESSEL Dual Test");

        // 1. Verify Legacy Overrides (1.20.1 - 1.20.4)
        Map<String, String> legacyFiles = artifact.legacyFiles();
        assertThat(legacyFiles).containsKey("assets/minecraft/models/item/diamond_sword.json");
        String legacyBaseJson = legacyFiles.get("assets/minecraft/models/item/diamond_sword.json");
        assertThat(legacyBaseJson).contains("\"overrides\"");
        assertThat(legacyBaseJson).contains("\"custom_model_data\": 10001");
        assertThat(legacyBaseJson).contains("\"custom_model_data\": 10002");
        assertThat(legacyFiles).containsKey("assets/vessel/models/item/frost_blade.json");
        assertThat(legacyFiles).containsKey("assets/vessel/models/item/flame_blade.json");

        // 2. Verify Modern Item Models (1.20.5+)
        Map<String, String> modernFiles = artifact.modernFiles();
        assertThat(modernFiles).containsKey("assets/minecraft/items/diamond_sword.json");
        String modernItemJson = modernFiles.get("assets/minecraft/items/diamond_sword.json");
        assertThat(modernItemJson).contains("\"minecraft:range_dispatch\"");
        assertThat(modernItemJson).contains("\"property\": \"minecraft:custom_model_data\"");
        assertThat(modernItemJson).contains("\"threshold\": 10001");
        assertThat(modernItemJson).contains("\"threshold\": 10002");
        assertThat(modernItemJson).contains("\"fallback\"");

        // 3. Verify Combined & pack.mcmeta
        Map<String, String> combined = artifact.combinedFiles();
        assertThat(combined).containsKey("pack.mcmeta");
        String mcmeta = combined.get("pack.mcmeta");
        assertThat(mcmeta).contains("\"supported_formats\"");

        // 4. Test direct compile and mount to HTTP Stream Server
        ZstdLosslessPackCompressor compressor = new ZstdLosslessPackCompressor();
        StreamableAssetResource mounted = builder.compileAndMount(
                "VESSEL Mounted Pack",
                "/live/resourcepack.zip",
                server,
                compressor
        );

        assertThat(mounted.contentLength()).isGreaterThan(0);

        // Fetch first 50 bytes of mounted pack over HTTP Range
        URI mountedUri = URI.create(server.getBaseUrl() + "/live/resourcepack.zip");
        HttpRequest rangeReq = HttpRequest.newBuilder(mountedUri)
                .header("Range", "bytes=0-49")
                .GET()
                .build();

        HttpResponse<byte[]> rangeResp = httpClient.send(rangeReq, HttpResponse.BodyHandlers.ofByteArray());
        assertThat(rangeResp.statusCode()).isEqualTo(206);
        assertThat(rangeResp.body()).hasSize(50);
        // Standard ZIP header starts with PK (0x50, 0x4B)
        assertThat(rangeResp.body()[0]).isEqualTo((byte) 0x50);
        assertThat(rangeResp.body()[1]).isEqualTo((byte) 0x4B);
    }
}
