package dev.vessel.kernel.asset.stream;

import java.util.Objects;

/**
 * Immutable configuration parameters for the internal HTTP Range stream server.
 * Extinguishes AST-NET by avoiding hardcoded socket parameters.
 *
 * @param host IP address or hostname to bind
 * @param port TCP port to bind (0 indicates dynamic ephemeral port)
 * @param chunkBufferSize buffer size in bytes for streaming chunked responses
 * @param contextPath base URI context prefix
 * @param backlog maximum number of queued incoming connections
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record HttpRangeServerConfig(
        String host,
        int port,
        int chunkBufferSize,
        String contextPath,
        int backlog
) {

    public static final String DEFAULT_HOST = "0.0.0.0";
    public static final int DEFAULT_PORT = 8080;
    public static final int DEFAULT_CHUNK_BUFFER_SIZE = 65536; // 64 KB
    public static final String DEFAULT_CONTEXT_PATH = "/";
    public static final int DEFAULT_BACKLOG = 128;

    public HttpRangeServerConfig {
        Objects.requireNonNull(host, "host cannot be null");
        Objects.requireNonNull(contextPath, "contextPath cannot be null");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port out of range [0-65535]: " + port);
        }
        if (chunkBufferSize <= 0) {
            throw new IllegalArgumentException("Chunk buffer size must be positive: " + chunkBufferSize);
        }
        if (backlog < 0) {
            throw new IllegalArgumentException("Backlog cannot be negative: " + backlog);
        }
    }

    /**
     * Creates a default configuration instance.
     *
     * @return default configuration
     */
    public static HttpRangeServerConfig createDefault() {
        return new HttpRangeServerConfig(
                DEFAULT_HOST,
                DEFAULT_PORT,
                DEFAULT_CHUNK_BUFFER_SIZE,
                DEFAULT_CONTEXT_PATH,
                DEFAULT_BACKLOG
        );
    }
}
