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
 * @version 1.0.6
 */
public final class VesselResourcePackDeliveryService {

    public static final String PACK_FILE_NAME = "vessel-pack.zip";
    public static final String PACK_ENDPOINT_PATH = "/vessel-pack.zip";

    private final VesselUniversalResourcePackCompiler packCompiler =
            new VesselUniversalResourcePackCompiler();
    private final VesselServerPropertiesPackConfigurator propertiesConfigurator =
            new VesselServerPropertiesPackConfigurator();
    private final VesselResourcePackHostResolver hostResolver =
            new VesselResourcePackHostResolver();
    private final dev.vessel.kernel.presentation.PresentationResourceChecksumVerifier checksumVerifier =
            new dev.vessel.kernel.presentation.PresentationResourceChecksumVerifier();
    private final dev.vessel.kernel.presentation.ClientResourcePackAcceptanceVerifier acceptanceVerifier =
            new dev.vessel.kernel.presentation.ClientResourcePackAcceptanceVerifier();
    private final dev.vessel.kernel.network.bridge.IntegratedPackHttpServer integratedPackServer =
            new dev.vessel.kernel.network.bridge.IntegratedPackHttpServer();
    private volatile HttpServer activeHttpServer;
    private volatile int activeHttpPort = resolveInitialHttpPort();
    private volatile String activePackSha1 = "";

    /**
     * Initializes the Universal Presentation Resource Pack ZIP, binds the light HTTP server
     * daemon on {@code serverPort + 1}, and injects the download URL into {@code server.properties}.
     *
     * @param serverRoot the server root directory
     * @param serverPort the Minecraft game server port (e.g., {@code 25565})
     * @return the HTTP download URL of the pack
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
     * @return the HTTP download URL of the pack
     */
    public String startDeliveryServerAndConfigureProperties(Path serverRoot, int serverPort, String mcVersion) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        this.activeHttpPort = resolveConfiguredHttpPort(serverPort);

        Path ecosystemDir = serverRoot.resolve("cache").resolve("ecosystem");
        Path packZip = ecosystemDir.resolve(PACK_FILE_NAME);

        try {
            Files.createDirectories(ecosystemDir);
            this.activePackSha1 = packCompiler.compileResourcePack(serverRoot, packZip, mcVersion);
            checksumVerifier.verifyChecksum(packZip, activePackSha1);

            if (activeHttpServer == null) {
                HttpServer server = HttpServer.create(new InetSocketAddress(activeHttpPort), 0);
                server.createContext(PACK_ENDPOINT_PATH, new VesselResourcePackHttpHandler(packZip));
                server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
                server.start();
                this.activeHttpServer = server;
            }

            String packUrl = hostResolver.resolveDeliveryUrl(serverRoot, activeHttpPort, PACK_ENDPOINT_PATH);
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
     * Dynamically sets or overrides the active HTTP delivery port.
     *
     * @param activeHttpPort valid TCP port between 1 and 65535
     */
    public void setActiveHttpPort(int activeHttpPort) {
        if (activeHttpPort > 0 && activeHttpPort <= 65535) {
            this.activeHttpPort = activeHttpPort;
        }
    }

    private static int resolveInitialHttpPort() {
        int configured = VesselServerConfig.getActiveConfig().getInt("client-presentation.http-port", 0);
        if (configured > 0 && configured <= 65535) {
            return configured;
        }
        int serverPort = VesselServerConfig.getActiveConfig().getInt("network-and-security.server-port", 25565);
        return (serverPort > 0 && serverPort < 65535) ? serverPort + 1 : 25566;
    }

    private int resolveConfiguredHttpPort(int serverPort) {
        int configuredPort = VesselServerConfig.getActiveConfig().getInt("client-presentation.http-port", 0);
        if (configuredPort > 0 && configuredPort <= 65535) {
            return configuredPort;
        }
        int effectiveServerPort = serverPort > 0
                ? serverPort
                : VesselServerConfig.getActiveConfig().getInt("network-and-security.server-port", 25565);
        return (effectiveServerPort > 0 && effectiveServerPort < 65535) ? effectiveServerPort + 1 : 25566;
    }

    public dev.vessel.kernel.presentation.ClientResourcePackAcceptanceVerifier getAcceptanceVerifier() {
        return acceptanceVerifier;
    }

    public dev.vessel.kernel.presentation.PresentationResourceChecksumVerifier getChecksumVerifier() {
        return checksumVerifier;
    }

    public dev.vessel.kernel.network.bridge.IntegratedPackHttpServer getIntegratedPackServer() {
        return integratedPackServer;
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

