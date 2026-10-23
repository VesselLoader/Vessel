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
 * @version 1.0.0
 */
public final class VesselResourcePackDeliveryService {

    public static final String PACK_FILE_NAME = "vessel-pack.zip";
    public static final String PACK_ENDPOINT_PATH = "/vessel-pack.zip";

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
            if (!Files.isRegularFile(packZip) || Files.size(packZip) < 256L) {
                compileDefaultUniversalResourcePack(packZip);
            }
            byte[] packBytes = Files.readAllBytes(packZip);
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            this.activePackSha1 = HexFormat.of().formatHex(sha1Digest.digest(packBytes));

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

    private static void compileDefaultUniversalResourcePack(Path packZip) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            // 1. pack.mcmeta
            JarEntry mcmetaEntry = new JarEntry("pack.mcmeta");
            jos.putNextEntry(mcmetaEntry);
            String mcmeta = """
                    {
                      "pack": {
                        "pack_format": 46,
                        "description": "VESSEL Sovereign Universal Mod Presentation Pack"
                      }
                    }
                    """;
            jos.write(mcmeta.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // 2. assets/vessel/textures/item/vessel_core.png (minimal 1x1 valid PNG)
            JarEntry textureEntry = new JarEntry("assets/vessel/textures/item/vessel_core.png");
            jos.putNextEntry(textureEntry);
            byte[] minPng = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                    (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                    0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
                    0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
                    0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
                    0x42, 0x60, (byte) 0x82
            };
            jos.write(minPng);
            jos.closeEntry();
        }
        Files.write(packZip, baos.toByteArray());
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

            for (String rawLine : content.split("\\R")) {
                String line = rawLine.trim();
                if (line.startsWith("resource-pack=")) {
                    sb.append("resource-pack=").append(packUrl).append('\n');
                    hasPack = true;
                } else if (line.startsWith("resource-pack-sha1=")) {
                    sb.append("resource-pack-sha1=").append(packSha1).append('\n');
                    hasSha1 = true;
                } else if (line.startsWith("resource-pack-prompt=")) {
                    sb.append("resource-pack-prompt=VESSEL Sovereign Universal Mod Presentation Pack\n");
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
            if (!hasPrompt) {
                sb.append("resource-pack-prompt=VESSEL Sovereign Universal Mod Presentation Pack\n");
            }
            Files.writeString(propsPath, sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
