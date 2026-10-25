package dev.vessel.kernel.asset.stream;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Non-blocking internal HTTP Range chunk streaming server for high-speed parallel asset distribution.
 * Implements RFC 7233 partial content distribution utilizing Java 21 Virtual Threads.
 * Extinguishes AST-NET by avoiding full-pack blocking stalls and thread-pool exhaustion.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class HttpRangeChunkStreamServer {

    private final HttpRangeServerConfig config;
    private final StreamableAssetRepository repository;
    private final HttpRangeHeaderParser rangeParser;
    private final HttpServer httpServer;
    private final ExecutorService executorService;
    private volatile boolean isRunning;

    /**
     * Constructs the server using provided configuration and repository.
     *
     * @param config server network configuration
     * @param repository asset repository
     * @throws IOException if the server socket cannot be bound
     */
    public HttpRangeChunkStreamServer(HttpRangeServerConfig config, StreamableAssetRepository repository) throws IOException {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.rangeParser = new HttpRangeHeaderParser();

        InetSocketAddress socketAddress = new InetSocketAddress(config.host(), config.port());
        this.httpServer = HttpServer.create(socketAddress, config.backlog());

        // Java 21 LTS Virtual Threads for unlimited non-blocking lightweight concurrency
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.httpServer.setExecutor(this.executorService);

        HttpRangeRequestHandler handler = new HttpRangeRequestHandler(
                this.repository,
                this.rangeParser,
                this.config.chunkBufferSize()
        );

        this.httpServer.createContext(this.config.contextPath(), handler);
    }

    /**
     * Constructs the server with default configuration and in-memory repository.
     *
     * @throws IOException if server fails to initialize
     */
    public HttpRangeChunkStreamServer() throws IOException {
        this(HttpRangeServerConfig.createDefault(), new InMemoryStreamableAssetRepository());
    }

    /**
     * Starts the HTTP streaming server.
     */
    public synchronized void start() {
        if (!isRunning) {
            httpServer.start();
            isRunning = true;
        }
    }

    /**
     * Stops the HTTP streaming server and gracefully terminates virtual threads.
     *
     * @param delaySeconds maximum grace period in seconds
     */
    public synchronized void stop(int delaySeconds) {
        if (isRunning) {
            httpServer.stop(delaySeconds);
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(delaySeconds, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException interruptedException) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            isRunning = false;
        }
    }

    /**
     * Returns the actual bound TCP port (useful when configured with port 0).
     *
     * @return bound port number
     */
    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    /**
     * Returns the bound host address or IP string.
     *
     * @return host string
     */
    public String getHost() {
        return httpServer.getAddress().getHostString();
    }

    /**
     * Builds the base HTTP URL pointing to this streaming server instance.
     *
     * @return base URL string (e.g. "http://127.0.0.1:8080")
     */
    public String getBaseUrl() {
        String host = getHost();
        if ("0.0.0.0".equals(host)) {
            host = "127.0.0.1";
        }
        return "http://" + host + ":" + getPort();
    }

    /**
     * Registers a streamable asset resource into the server repository.
     *
     * @param resource asset resource
     */
    public void registerAsset(StreamableAssetResource resource) {
        repository.registerAsset(resource);
    }

    /**
     * Obtains the internal asset repository.
     *
     * @return streamable asset repository
     */
    public StreamableAssetRepository getRepository() {
        return repository;
    }

    /**
     * Checks if the server is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }
}
