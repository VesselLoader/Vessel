package dev.vessel.kernel.test;

import com.sun.net.httpserver.HttpServer;
import dev.vessel.bootstrap.target.VesselResourcePackHttpHandler;
import dev.vessel.kernel.resourcepack.http.VesselByteRangeSpec;
import dev.vessel.kernel.resourcepack.http.VesselFileChannelChunkStreamer;
import dev.vessel.kernel.resourcepack.http.VesselHttpRangeHeaderParser;
import dev.vessel.kernel.resourcepack.http.VesselRangeNotSatisfiableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for HTTP Range streaming, chunk streamer, and {@link VesselResourcePackHttpHandler}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class VesselResourcePackHttpStreamingRangeTest {

    @Test
    @DisplayName("VesselHttpRangeHeaderParser parses closed, open-ended, and suffix ranges")
    void testRangeHeaderParserSuccess() {
        VesselHttpRangeHeaderParser parser = new VesselHttpRangeHeaderParser();
        long totalSize = 1000L;

        // Null / blank -> null
        assertThat(parser.parse(null, totalSize)).isNull();
        assertThat(parser.parse("", totalSize)).isNull();
        assertThat(parser.parse("   ", totalSize)).isNull();
        assertThat(parser.parse("non-bytes-range", totalSize)).isNull();

        // Closed range: bytes=0-499
        VesselByteRangeSpec closed = parser.parse("bytes=0-499", totalSize);
        assertThat(closed).isNotNull();
        assertThat(closed.start()).isEqualTo(0L);
        assertThat(closed.end()).isEqualTo(499L);
        assertThat(closed.contentLength()).isEqualTo(500L);
        assertThat(closed.toContentRangeHeader()).isEqualTo("bytes 0-499/1000");

        // Open-ended range: bytes=500-
        VesselByteRangeSpec openEnded = parser.parse("bytes=500-", totalSize);
        assertThat(openEnded).isNotNull();
        assertThat(openEnded.start()).isEqualTo(500L);
        assertThat(openEnded.end()).isEqualTo(999L);
        assertThat(openEnded.contentLength()).isEqualTo(500L);
        assertThat(openEnded.toContentRangeHeader()).isEqualTo("bytes 500-999/1000");

        // Suffix range: bytes=-250 (last 250 bytes)
        VesselByteRangeSpec suffix = parser.parse("bytes=-250", totalSize);
        assertThat(suffix).isNotNull();
        assertThat(suffix.start()).isEqualTo(750L);
        assertThat(suffix.end()).isEqualTo(999L);
        assertThat(suffix.contentLength()).isEqualTo(250L);
        assertThat(suffix.toContentRangeHeader()).isEqualTo("bytes 750-999/1000");

        // Overflow end truncated per RFC 7233: bytes=800-2000
        VesselByteRangeSpec truncated = parser.parse("bytes=800-2000", totalSize);
        assertThat(truncated).isNotNull();
        assertThat(truncated.start()).isEqualTo(800L);
        assertThat(truncated.end()).isEqualTo(999L);
        assertThat(truncated.contentLength()).isEqualTo(200L);
    }

    @Test
    @DisplayName("VesselHttpRangeHeaderParser rejects unsatisfiable or malformed byte ranges")
    void testRangeHeaderParserUnsatisfiable() {
        VesselHttpRangeHeaderParser parser = new VesselHttpRangeHeaderParser();

        // Empty file cannot satisfy any range
        assertThatThrownBy(() -> parser.parse("bytes=0-10", 0L))
                .isInstanceOf(VesselRangeNotSatisfiableException.class);

        // Start >= totalSize
        assertThatThrownBy(() -> parser.parse("bytes=1000-1050", 1000L))
                .isInstanceOf(VesselRangeNotSatisfiableException.class);

        // Start > End
        assertThatThrownBy(() -> parser.parse("bytes=500-200", 1000L))
                .isInstanceOf(VesselRangeNotSatisfiableException.class);

        // Suffix length <= 0
        assertThatThrownBy(() -> parser.parse("bytes=-0", 1000L))
                .isInstanceOf(VesselRangeNotSatisfiableException.class);

        // Malformed string
        assertThatThrownBy(() -> parser.parse("bytes=nodash", 1000L))
                .isInstanceOf(VesselRangeNotSatisfiableException.class);
        assertThatThrownBy(() -> parser.parse("bytes=abc-def", 1000L))
                .isInstanceOf(VesselRangeNotSatisfiableException.class);
    }

    @Test
    @DisplayName("VesselFileChannelChunkStreamer streams full and sliced ranges accurately")
    void testChunkStreamer(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("sample.bin");
        byte[] sourceData = new byte[200_000];
        for (int i = 0; i < sourceData.length; i++) {
            sourceData[i] = (byte) (i & 0xFF);
        }
        Files.write(testFile, sourceData);

        VesselFileChannelChunkStreamer streamer = new VesselFileChannelChunkStreamer(64 * 1024);

        // Test full stream
        try (FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long streamed = streamer.streamAll(channel, baos);

            assertThat(streamed).isEqualTo(sourceData.length);
            assertThat(baos.toByteArray()).containsExactly(sourceData);
        }

        // Test slice stream: start = 50,000, length = 75,000
        try (FileChannel channel = FileChannel.open(testFile, StandardOpenOption.READ)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long streamed = streamer.streamRange(channel, 50_000L, 75_000L, baos);

            assertThat(streamed).isEqualTo(75_000L);
            byte[] expectedSlice = Arrays.copyOfRange(sourceData, 50_000, 125_000);
            assertThat(baos.toByteArray()).containsExactly(expectedSlice);
        }
    }

    @Test
    @DisplayName("VesselResourcePackHttpHandler serves 200 OK, 206 Partial Content, and 416 Unsatisfiable via live HTTP")
    void testLiveHttpHandler(@TempDir Path tempDir) throws IOException {
        Path packFile = tempDir.resolve("test-pack.zip");
        byte[] payload = "VESSEL_TEST_RESOURCE_PACK_PAYLOAD_CHUNK_STREAMING_VERIFICATION_1234567890".getBytes(StandardCharsets.UTF_8);
        Files.write(packFile, payload);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/pack.zip", new VesselResourcePackHttpHandler(packFile));
        server.start();

        int port = server.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port + "/pack.zip";

        try {
            // 1. Full GET (200 OK)
            HttpURLConnection conn200 = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            conn200.setRequestMethod("GET");
            assertThat(conn200.getResponseCode()).isEqualTo(200);
            assertThat(conn200.getHeaderField("Content-Type")).isEqualTo("application/zip");
            assertThat(conn200.getHeaderField("Accept-Ranges")).isEqualTo("bytes");
            assertThat(conn200.getContentLengthLong()).isEqualTo(payload.length);
            byte[] body200 = conn200.getInputStream().readAllBytes();
            assertThat(body200).containsExactly(payload);

            // 2. HEAD request (200 OK, zero body)
            HttpURLConnection connHead = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            connHead.setRequestMethod("HEAD");
            assertThat(connHead.getResponseCode()).isEqualTo(200);
            assertThat(connHead.getContentLengthLong()).isEqualTo(payload.length);
            assertThat(connHead.getInputStream().readAllBytes()).isEmpty();

            // 3. Partial Content (206 Partial Content) Range: bytes=10-25
            HttpURLConnection conn206 = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            conn206.setRequestMethod("GET");
            conn206.setRequestProperty("Range", "bytes=10-25");
            assertThat(conn206.getResponseCode()).isEqualTo(206);
            assertThat(conn206.getHeaderField("Content-Range")).isEqualTo("bytes 10-25/" + payload.length);
            assertThat(conn206.getContentLengthLong()).isEqualTo(16);
            byte[] body206 = conn206.getInputStream().readAllBytes();
            byte[] expectedSlice = Arrays.copyOfRange(payload, 10, 26);
            assertThat(body206).containsExactly(expectedSlice);

            // 4. Unsatisfiable Range (416 Range Not Satisfiable) Range: bytes=9999-19999
            HttpURLConnection conn416 = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            conn416.setRequestMethod("GET");
            conn416.setRequestProperty("Range", "bytes=9999-19999");
            assertThat(conn416.getResponseCode()).isEqualTo(416);
            assertThat(conn416.getHeaderField("Content-Range")).isEqualTo("bytes */" + payload.length);

            // 5. Method Not Allowed (405)
            HttpURLConnection conn405 = (HttpURLConnection) URI.create(baseUrl).toURL().openConnection();
            conn405.setRequestMethod("POST");
            assertThat(conn405.getResponseCode()).isEqualTo(405);
        } finally {
            server.stop(0);
        }
    }
}
