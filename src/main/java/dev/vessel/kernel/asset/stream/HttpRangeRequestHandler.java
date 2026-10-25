package dev.vessel.kernel.asset.stream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Autonomous HTTP request handler for high-speed streaming of chunked asset payloads.
 * Implements RFC 7233 Range requests with zero heap duplication via direct slicing.
 * Extinguishes AST-NET.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class HttpRangeRequestHandler implements HttpHandler {

    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_OPTIONS = "OPTIONS";

    public static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
    public static final String HEADER_RANGE = "Range";
    public static final String HEADER_CONTENT_RANGE = "Content-Range";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_ALLOW = "Allow";

    public static final String BYTES_UNIT = "bytes";
    public static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";

    public static final int HTTP_OK = 200;
    public static final int HTTP_PARTIAL_CONTENT = 206;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_METHOD_NOT_ALLOWED = 405;
    public static final int HTTP_RANGE_NOT_SATISFIABLE = 416;

    private final StreamableAssetRepository repository;
    private final HttpRangeHeaderParser rangeParser;
    private final int chunkBufferSize;

    /**
     * Constructs the HTTP range handler with asset repository and configuration.
     *
     * @param repository asset repository
     * @param rangeParser range parser instance
     * @param chunkBufferSize chunk buffer size for response streaming
     */
    public HttpRangeRequestHandler(
            StreamableAssetRepository repository,
            HttpRangeHeaderParser rangeParser,
            int chunkBufferSize
    ) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.rangeParser = Objects.requireNonNull(rangeParser, "rangeParser cannot be null");
        this.chunkBufferSize = Math.max(1024, chunkBufferSize);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if (METHOD_OPTIONS.equalsIgnoreCase(method)) {
            handleOptions(exchange);
            return;
        }

        if (!METHOD_GET.equalsIgnoreCase(method) && !METHOD_HEAD.equalsIgnoreCase(method)) {
            sendErrorResponse(exchange, HTTP_METHOD_NOT_ALLOWED, "Method Not Allowed");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        Optional<StreamableAssetResource> optionalResource = repository.findAsset(requestPath);

        if (optionalResource.isEmpty()) {
            sendErrorResponse(exchange, HTTP_NOT_FOUND, "Asset Not Found: " + requestPath);
            return;
        }

        StreamableAssetResource resource = optionalResource.get();
        long totalLength = resource.contentLength();
        Headers requestHeaders = exchange.getRequestHeaders();
        String rangeHeader = requestHeaders.getFirst(HEADER_RANGE);

        boolean isHead = METHOD_HEAD.equalsIgnoreCase(method);

        if (rangeHeader != null && !rangeHeader.isBlank()) {
            handleRangeRequest(exchange, resource, rangeHeader, totalLength, isHead);
        } else {
            handleFullRequest(exchange, resource, totalLength, isHead);
        }
    }

    private void handleOptions(HttpExchange exchange) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set(HEADER_ALLOW, ALLOWED_METHODS);
        responseHeaders.set(HEADER_ACCEPT_RANGES, BYTES_UNIT);
        exchange.sendResponseHeaders(HTTP_OK, -1);
        exchange.close();
    }

    private void handleFullRequest(
            HttpExchange exchange,
            StreamableAssetResource resource,
            long totalLength,
            boolean isHead
    ) throws IOException {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set(HEADER_ACCEPT_RANGES, BYTES_UNIT);
        responseHeaders.set(HEADER_CONTENT_TYPE, resource.mimeType());
        responseHeaders.set(HEADER_CONTENT_LENGTH, String.valueOf(totalLength));
        responseHeaders.set(HEADER_ETAG, "\"" + resource.sha1Hex() + "\"");

        if (isHead) {
            exchange.sendResponseHeaders(HTTP_OK, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(HTTP_OK, totalLength);
        streamBytes(resource.data(), 0, (int) totalLength, exchange.getResponseBody());
        exchange.close();
    }

    private void handleRangeRequest(
            HttpExchange exchange,
            StreamableAssetResource resource,
            String rangeHeader,
            long totalLength,
            boolean isHead
    ) throws IOException {
        Optional<ByteRangeSpec> optionalRange = rangeParser.parseRange(rangeHeader, totalLength);

        if (optionalRange.isEmpty()) {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set(HEADER_CONTENT_RANGE, ByteRangeSpec.toUnsatisfiableRangeHeader(totalLength));
            responseHeaders.set(HEADER_ACCEPT_RANGES, BYTES_UNIT);
            exchange.sendResponseHeaders(HTTP_RANGE_NOT_SATISFIABLE, -1);
            exchange.close();
            return;
        }

        ByteRangeSpec range = optionalRange.get();
        long contentLength = range.contentLength();

        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set(HEADER_ACCEPT_RANGES, BYTES_UNIT);
        responseHeaders.set(HEADER_CONTENT_RANGE, range.toContentRangeHeader());
        responseHeaders.set(HEADER_CONTENT_TYPE, resource.mimeType());
        responseHeaders.set(HEADER_CONTENT_LENGTH, String.valueOf(contentLength));
        responseHeaders.set(HEADER_ETAG, "\"" + resource.sha1Hex() + "\"");

        if (isHead) {
            exchange.sendResponseHeaders(HTTP_PARTIAL_CONTENT, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(HTTP_PARTIAL_CONTENT, contentLength);
        streamBytes(resource.data(), (int) range.start(), (int) contentLength, exchange.getResponseBody());
        exchange.close();
    }

    private void streamBytes(byte[] source, int offset, int length, OutputStream out) throws IOException {
        int remaining = length;
        int currentOffset = offset;

        while (remaining > 0) {
            int toWrite = Math.min(remaining, chunkBufferSize);
            out.write(source, currentOffset, toWrite);
            currentOffset += toWrite;
            remaining -= toWrite;
        }
        out.flush();
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.set(HEADER_CONTENT_TYPE, "text/plain; charset=utf-8");
        responseHeaders.set(HEADER_CONTENT_LENGTH, String.valueOf(payload.length));
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
            out.flush();
        }
        exchange.close();
    }
}
