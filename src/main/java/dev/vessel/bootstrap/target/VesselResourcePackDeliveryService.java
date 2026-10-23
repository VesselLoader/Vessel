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
 * @version 1.0.1
 */
public final class VesselResourcePackDeliveryService {

    public static final String PACK_FILE_NAME = "vessel-pack.zip";
    public static final String PACK_ENDPOINT_PATH = "/vessel-pack.zip";

    private final VesselUniversalResourcePackCompiler packCompiler =
            new VesselUniversalResourcePackCompiler();
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
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        this.activeHttpPort = (serverPort > 0 && serverPort < 65535) ? serverPort + 1 : 25566;

        Path ecosystemDir = serverRoot.resolve("cache").resolve("ecosystem");
        Path packZip = ecosystemDir.resolve(PACK_FILE_NAME);

        try {
            Files.createDirectories(ecosystemDir);
            this.activePackSha1 = packCompiler.compileResourcePack(serverRoot, packZip);

            if (activeHttpServer == null) {
                HttpServer server = HttpServer.create(new InetSocketAddress(activeHttpPort), 0);
                server.createContext(PACK_ENDPOINT_PATH, new VesselResourcePackHttpHandler(packZip));
                server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
                server.start();
                this.activeHttpServer = server;
            }

            String packUrl = "http://127.0.0.1:" + activeHttpPort + PACK_ENDPOINT_PATH;
            updateServerPropertiesWithPackUrl(serverRoot, packUrl, activePackSha1);
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

    private static void updateServerPropertiesWithPackUrl(
            Path serverRoot,
            String packUrl,
            String packSha1) {
        Path propsPath = serverRoot.resolve("server.properties");
        if (!Files.isRegularFile(propsPath)) {
            return;
        }
        try {
            String content = Files.readString(propsPath, StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder(content.length() + 256);
            boolean hasPack = false;
            boolean hasSha1 = false;
            boolean hasPrompt = false;
            boolean hasPackId = false;

            for (String rawLine : content.split("\\R")) {
                String line = rawLine.trim();
                if (line.startsWith("resource-pack=")) {
                    sb.append("resource-pack=").append(packUrl).append('\n');
                    hasPack = true;
                } else if (line.startsWith("resource-pack-sha1=")) {
                    sb.append("resource-pack-sha1=").append(packSha1).append('\n');
                    hasSha1 = true;
                } else if (line.startsWith("resource-pack-id=")) {
                    sb.append("resource-pack-id=fcce4567-99de-3e01-949b-9799d6489542\n");
                    hasPackId = true;
                } else if (line.startsWith("resource-pack-prompt=")) {
                    sb.append("resource-pack-prompt={\"text\":\"VESSEL Sovereign Universal Mod Presentation Pack\"}\n");
                    hasPrompt = true;
                } else {
                    sb.append(rawLine).append('\n');
                }
            }
            if (!hasPack) {
                sb.append("resource-pack=").append(packUrl).append('\n');
            }
            if (!hasSha1) {
                sb.append("resource-pack-sha1=").append(packSha1).append('\n');
            }
            if (!hasPackId) {
                sb.append("resource-pack-id=fcce4567-99de-3e01-949b-9799d6489542\n");
            }
            if (!hasPrompt) {
                sb.append("resource-pack-prompt={\"text\":\"VESSEL Sovereign Universal Mod Presentation Pack\"}\n");
            }
            Files.writeString(propsPath, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
