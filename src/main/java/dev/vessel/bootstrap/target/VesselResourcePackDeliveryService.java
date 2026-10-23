package dev.vessel.bootstrap.target;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Manages the built-in HTTP server daemon that streams the compiled Universal Client Presentation
 * Resource Pack (texture packs, custom block/item/entity models) to connecting Vanilla and Bedrock
 * clients, and automatically updates {@code server.properties} with the pack URL and SHA-1 hash.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class VesselResourcePackDeliveryService {

    public static final String PACK_FILE_NAME = "vessel-pack.zip";
    public static final String PACK_ENDPOINT_PATH = "/vessel-pack.zip";

    private final VesselUniversalResourcePackCompiler packCompiler =
            new VesselUniversalResourcePackCompiler();
    private final VesselServerPropertiesPackConfigurator propertiesConfigurator =
            new VesselServerPropertiesPackConfigurator();
    private volatile HttpServer activeHttpServer;
    private volatile int activeHttpPort = 25566;
    private volatile String activePackSha1 = "";

    /**
     * Initializes the Universal Presentation Resource Pack ZIP, binds the light HTTP server
     * daemon on {@code serverPort + 1}, and injects the download URL into {@code server.properties}.
     *
     * @param serverRoot the server root directory
     * @param serverPort the Minecraft game server port (e.g., {@code 25565})
     * @return the local HTTP download URL of the pack (e.g., {@code "http://127.0.0.1:25566/vessel-pack.zip"})
     */
    public String startDeliveryServerAndConfigureProperties(Path serverRoot, int serverPort) {
        return startDeliveryServerAndConfigureProperties(serverRoot, serverPort, "1.21.4");
    }

    /**
     * Initializes the Universal Presentation Resource Pack ZIP with version-specific pack_format,
     * binds the light HTTP server daemon on {@code serverPort + 1}, and injects era-aware properties into {@code server.properties}.
     *
     * @param serverRoot the server root directory
     * @param serverPort the Minecraft game server port (e.g., {@code 25565})
     * @param mcVersion  active Minecraft version (e.g., {@code "1.12.2"}, {@code "1.20.1"}, {@code "26.3"})
     * @return the local HTTP download URL of the pack (e.g., {@code "http://127.0.0.1:25566/vessel-pack.zip"})
     */
    public String startDeliveryServerAndConfigureProperties(Path serverRoot, int serverPort, String mcVersion) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        this.activeHttpPort = (serverPort > 0 && serverPort < 65535) ? serverPort + 1 : 25566;

        Path ecosystemDir = serverRoot.resolve("cache").resolve("ecosystem");
        Path packZip = ecosystemDir.resolve(PACK_FILE_NAME);

        try {
            Files.createDirectories(ecosystemDir);
            this.activePackSha1 = packCompiler.compileResourcePack(serverRoot, packZip, mcVersion);

            if (activeHttpServer == null) {
                HttpServer server = HttpServer.create(new InetSocketAddress(activeHttpPort), 0);
                server.createContext(PACK_ENDPOINT_PATH, new VesselResourcePackHttpHandler(packZip));
                server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
                server.start();
                this.activeHttpServer = server;
            }

            String packUrl = "http://127.0.0.1:" + activeHttpPort + PACK_ENDPOINT_PATH;
            propertiesConfigurator.configureServerProperties(serverRoot, packUrl, activePackSha1, mcVersion);
            return packUrl;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the 40-character SHA-1 hex hash of the currently served resource pack.
     *
     * @return SHA-1 hash string
     */
    public String getActivePackSha1() {
        return activePackSha1;
    }

    /**
     * Returns the active HTTP port serving the resource pack.
     *
     * @return HTTP port number
     */
    public int getActiveHttpPort() {
        return activeHttpPort;
    }

    /**
     * Stops the HTTP delivery server if running.
     */
    public void stopDeliveryServer() {
        HttpServer server = this.activeHttpServer;
        if (server != null) {
            server.stop(0);
            this.activeHttpServer = null;
        }
    }
}

