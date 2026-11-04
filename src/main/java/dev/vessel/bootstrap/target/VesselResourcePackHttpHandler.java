package dev.vessel.bootstrap.target;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dev.vessel.kernel.resourcepack.http.VesselByteRangeSpec;
import dev.vessel.kernel.resourcepack.http.VesselFileChannelChunkStreamer;
import dev.vessel.kernel.resourcepack.http.VesselHttpRangeHeaderParser;
import dev.vessel.kernel.resourcepack.http.VesselRangeNotSatisfiableException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * High-performance HTTP handler that streams the compiled VESSEL Universal Presentation Resource Pack ZIP
 * to connecting Minecraft Java and Bedrock clients using Zero-Copy 64 KB chunk streaming and full HTTP byte-range resumes.
 * <p>
 * Eliminates out-of-memory crashes on large packs by streaming directly from disk channels in 64 KB blocks
 * without buffering the full archive into the JVM heap. Supports {@code Range: bytes=start-end} with HTTP 206
 * Partial Content to allow clients on unstable networks to resume interrupted downloads seamlessly.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselResourcePackHttpHandler implements HttpHandler {

    private final Path packZipFile;
    private final VesselHttpRangeHeaderParser rangeParser = new VesselHttpRangeHeaderParser();
    private final VesselFileChannelChunkStreamer chunkStreamer = new VesselFileChannelChunkStreamer();

    /**
     * Constructs a new HTTP streaming handler for the target pack archive.
     *
     * @param packZipFile path to the resource pack ZIP file
     */
    public VesselResourcePackHttpHandler(Path packZipFile) {
        this.packZipFile = Objects.requireNonNull(packZipFile, "packZipFile must not be null");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        if (!Files.isRegularFile(packZipFile)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        try (FileChannel fileChannel = FileChannel.open(packZipFile, StandardOpenOption.READ)) {
            long totalSize = fileChannel.size();
            String rangeHeader = exchange.getRequestHeaders().getFirst("Range");

            VesselByteRangeSpec rangeSpec = null;
            if (rangeHeader != null && !rangeHeader.isBlank()) {
                try {
                    rangeSpec = rangeParser.parse(rangeHeader, totalSize);
                } catch (VesselRangeNotSatisfiableException e) {
                    exchange.getResponseHeaders().set("Content-Range", VesselByteRangeSpec.toUnsatisfiableHeader(totalSize));
                    exchange.sendResponseHeaders(416, -1);
                    return;
                }
            }

            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.getResponseHeaders().set("Accept-Ranges", "bytes");

            boolean isHead = "HEAD".equalsIgnoreCase(method);

            if (rangeSpec != null) {
                // HTTP 206 Partial Content (Client Resuming or Slicing)
                exchange.getResponseHeaders().set("Content-Range", rangeSpec.toContentRangeHeader());
                if (isHead) {
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(rangeSpec.contentLength()));
                    exchange.sendResponseHeaders(206, -1);
                } else {
                    exchange.sendResponseHeaders(206, rangeSpec.contentLength());
                    try (OutputStream os = exchange.getResponseBody()) {
                        chunkStreamer.streamRange(fileChannel, rangeSpec.start(), rangeSpec.contentLength(), os);
                    }
                }
            } else {
                // HTTP 200 OK (Full Archive Transfer)
                if (isHead) {
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(totalSize));
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, totalSize);
                    try (OutputStream os = exchange.getResponseBody()) {
                        chunkStreamer.streamAll(fileChannel, os);
                    }
                }
            }
        }
    }
}
