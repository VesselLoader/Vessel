package dev.vessel.bootstrap.ecosystem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatically checks, downloads/updates, and integrates the latest official releases of
 * <b>GeyserMC</b>, <b>Floodgate</b>, and <b>spark</b> via the Modrinth v2 API and GeyserMC
 * Download v2 API alongside the latest Minecraft release, while generating full open-source
 * credits via {@link ThirdPartyCreditsAndAttributionRegistry}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class BuiltInGeyserAndSparkProvisioner {

    public static final String GEYSER_LATEST_API_URL =
            "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest";
    public static final String GEYSER_STANDALONE_DOWNLOAD_URL =
            "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/standalone";
    public static final String GEYSER_MODRINTH_API_URL =
            "https://api.modrinth.com/v2/project/geyser/version";
    public static final String FLOODGATE_MODRINTH_API_URL =
            "https://api.modrinth.com/v2/project/floodgate/version";
    public static final String FLOODGATE_LATEST_API_URL =
            "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest";
    public static final String SPARK_LATEST_API_URL =
            "https://api.modrinth.com/v2/project/spark/version";

    public static final String DEFAULT_LATEST_GEYSER_VERSION = "2.6.1-b756";
    public static final String DEFAULT_LATEST_FLOODGATE_VERSION = "2.2.7-b69";
    public static final String DEFAULT_LATEST_SPARK_VERSION = "1.10.119";
    public static final String DEFAULT_LATEST_MINECRAFT_VERSION = "1.21.4";

    private static final Pattern GEYSER_VERSION_PATTERN =
            Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern GEYSER_BUILD_PATTERN =
            Pattern.compile("\"build\"\\s*:\\s*(\\d+)");
    private static final Pattern VERSION_NUMBER_PATTERN =
            Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MODRINTH_CDN_JAR_URL_PATTERN =
            Pattern.compile("\"(https://cdn\\.modrinth\\.com/[^\"]+\\.jar)\"");

    private final ThirdPartyCreditsAndAttributionRegistry creditsRegistry =
            new ThirdPartyCreditsAndAttributionRegistry();

    private volatile String activeGeyserVersion = DEFAULT_LATEST_GEYSER_VERSION;
    private volatile String activeFloodgateVersion = DEFAULT_LATEST_FLOODGATE_VERSION;
    private volatile String activeSparkVersion = DEFAULT_LATEST_SPARK_VERSION;
    private volatile String activeMinecraftVersion = DEFAULT_LATEST_MINECRAFT_VERSION;

    /**
     * Parses the latest Geyser version and build number from a GeyserMC API v2 or Modrinth API v2
     * JSON response or falls back to {@link #DEFAULT_LATEST_GEYSER_VERSION}.
     *
     * @param geyserJson raw JSON string from GeyserMC or Modrinth API
     * @return resolved Geyser version string (e.g., {@code "2.12.0-b1269"})
     */
    public String parseGeyserVersionFromJson(String geyserJson) {
        if (geyserJson == null || geyserJson.isBlank()) {
            return DEFAULT_LATEST_GEYSER_VERSION;
        }
        Matcher modrinthMatcher = VERSION_NUMBER_PATTERN.matcher(geyserJson);
        if (modrinthMatcher.find()) {
            String ver = modrinthMatcher.group(1).trim();
            if (!ver.isEmpty()) {
                return ver;
            }
        }
        Matcher verMatcher = GEYSER_VERSION_PATTERN.matcher(geyserJson);
        Matcher buildMatcher = GEYSER_BUILD_PATTERN.matcher(geyserJson);
        if (verMatcher.find()) {
            String ver = verMatcher.group(1).trim();
            if (buildMatcher.find()) {
                return ver + "-b" + buildMatcher.group(1).trim();
            }
            return ver;
        }
        return DEFAULT_LATEST_GEYSER_VERSION;
    }

    /**
     * Parses the latest Floodgate release version from a Modrinth or GeyserMC API JSON response or
     * falls back to {@link #DEFAULT_LATEST_FLOODGATE_VERSION}.
     *
     * @param floodgateJson raw JSON string from Modrinth or GeyserMC API
     * @return resolved Floodgate version string (e.g., {@code "2.2.7-b69"})
     */
    public String parseFloodgateVersionFromJson(String floodgateJson) {
        if (floodgateJson == null || floodgateJson.isBlank()) {
            return DEFAULT_LATEST_FLOODGATE_VERSION;
        }
        Matcher modrinthMatcher = VERSION_NUMBER_PATTERN.matcher(floodgateJson);
        if (modrinthMatcher.find()) {
            String ver = modrinthMatcher.group(1).trim();
            if (!ver.isEmpty()) {
                return ver;
            }
        }
        Matcher verMatcher = GEYSER_VERSION_PATTERN.matcher(floodgateJson);
        Matcher buildMatcher = GEYSER_BUILD_PATTERN.matcher(floodgateJson);
        if (verMatcher.find()) {
            String ver = verMatcher.group(1).trim();
            if (buildMatcher.find()) {
                return ver + "-b" + buildMatcher.group(1).trim();
            }
            return ver;
        }
        return DEFAULT_LATEST_FLOODGATE_VERSION;
    }

    /**
     * Parses the latest Spark release version from a Modrinth/Spark API JSON response or falls
     * back to {@link #DEFAULT_LATEST_SPARK_VERSION}.
     *
     * @param sparkJson raw JSON string from Spark/Modrinth API
     * @return resolved Spark version string (e.g., {@code "1.10.187"})
     */
    public String parseSparkVersionFromJson(String sparkJson) {
        if (sparkJson == null || sparkJson.isBlank()) {
            return DEFAULT_LATEST_SPARK_VERSION;
        }
        Matcher matcher = VERSION_NUMBER_PATTERN.matcher(sparkJson);
        if (matcher.find()) {
            String ver = matcher.group(1).trim();
            if (!ver.isEmpty()) {
                return ver;
            }
        }
        return DEFAULT_LATEST_SPARK_VERSION;
    }

    /**
     * Extracts the first Modrinth CDN {@code .jar} download URL from a Modrinth version JSON array.
     *
     * @param modrinthJson raw JSON response from {@code https://api.modrinth.com/v2/project/.../version}
     * @return extracted CDN {@code .jar} download URL, or {@code ""} if none is present
     */
    public String extractModrinthJarDownloadUrl(String modrinthJson) {
        if (modrinthJson == null || modrinthJson.isBlank()) {
            return "";
        }
        Matcher matcher = MODRINTH_CDN_JAR_URL_PATTERN.matcher(modrinthJson);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * Synchronizes and provisions the latest Geyser, Floodgate, and Spark integrations inside
     * {@code serverRoot/cache/ecosystem/}, updates {@code ecosystem-versions.json}, and writes
     * {@code CREDITS.md} with full attribution.
     *
     * @param serverRoot          server root directory
     * @param resolvedMcVersion   active Minecraft release version (defaults to {@code latest.release})
     * @param geyserApiEndpoint   Geyser API endpoint (or {@code "offline://"} for local/cached mode)
     * @param sparkApiEndpoint    Spark API endpoint (or {@code "offline://"} for local/cached mode)
     * @return path to the written {@code ecosystem-versions.json} state file
     */
    public Path provisionAndSyncLatestEcosystem(
            Path serverRoot,
            String resolvedMcVersion,
            String geyserApiEndpoint,
            String sparkApiEndpoint) {
        return provisionAndSyncLatestEcosystem(
                serverRoot,
                resolvedMcVersion,
                geyserApiEndpoint,
                isOnlineHttpUrl(geyserApiEndpoint) ? FLOODGATE_MODRINTH_API_URL : "offline://floodgate",
                sparkApiEndpoint);
    }

    /**
     * Synchronizes and provisions the latest Geyser, Floodgate, and Spark integrations inside
     * {@code serverRoot/cache/ecosystem/}, downloading official release JARs via Modrinth &amp;
     * GeyserMC APIs when online, updating {@code ecosystem-versions.json}, and writing
     * {@code CREDITS.md} with full attribution.
     *
     * @param serverRoot           server root directory
     * @param resolvedMcVersion    active Minecraft release version
     * @param geyserApiEndpoint    Geyser API endpoint
     * @param floodgateApiEndpoint Floodgate API endpoint
     * @param sparkApiEndpoint     Spark API endpoint
     * @return path to the written {@code ecosystem-versions.json} state file
     */
    public Path provisionAndSyncLatestEcosystem(
            Path serverRoot,
            String resolvedMcVersion,
            String geyserApiEndpoint,
            String floodgateApiEndpoint,
            String sparkApiEndpoint) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        this.activeMinecraftVersion =
                (resolvedMcVersion != null && !resolvedMcVersion.isBlank())
                        ? resolvedMcVersion.trim()
                        : DEFAULT_LATEST_MINECRAFT_VERSION;

        String geyserJson = fetchJsonIfOnline(geyserApiEndpoint);
        String floodgateJson = fetchJsonIfOnline(floodgateApiEndpoint);
        String sparkJson = fetchJsonIfOnline(sparkApiEndpoint);
        this.activeGeyserVersion = parseGeyserVersionFromJson(geyserJson);
        this.activeFloodgateVersion = parseFloodgateVersionFromJson(floodgateJson);
        this.activeSparkVersion = parseSparkVersionFromJson(sparkJson);

        Path ecosystemDir = serverRoot.resolve("cache").resolve("ecosystem");
        try {
            Files.createDirectories(ecosystemDir);
            Path geyserJar = ecosystemDir.resolve("geyser-vessel-integrated.jar");
            Path geyserStandaloneJar = ecosystemDir.resolve("Geyser-Standalone.jar");
            Path floodgateJar = ecosystemDir.resolve("floodgate-vessel-integrated.jar");
            Path sparkJar = ecosystemDir.resolve("spark-vessel-integrated.jar");
            Path stateFile = ecosystemDir.resolve("ecosystem-versions.json");

            String existingState = Files.isRegularFile(stateFile)
                    ? Files.readString(stateFile, StandardCharsets.UTF_8)
                    : "";

            boolean onlineMode = isOnlineHttpUrl(geyserApiEndpoint) || isOnlineHttpUrl(sparkApiEndpoint);
            if (onlineMode) {
                // 1. Download Geyser (Modrinth + Standalone Bedrock UDP 19132 Bridge) if version changed or missing
                if (!existingState.contains("\"geyserLatestVersion\": \"" + this.activeGeyserVersion + "\"")
                        || !Files.isRegularFile(geyserJar)
                        || Files.size(geyserJar) < 4096L) {
                    String geyserDownloadUrl = extractModrinthJarDownloadUrl(geyserJson);
                    if (geyserDownloadUrl.isBlank()) {
                        String modrinthGeyserJson = fetchJsonIfOnline(GEYSER_MODRINTH_API_URL);
                        geyserDownloadUrl = extractModrinthJarDownloadUrl(modrinthGeyserJson);
                    }
                    if (!geyserDownloadUrl.isBlank()) {
                        downloadBinaryFileIfOnline(geyserDownloadUrl, geyserJar);
                    }
                }
                if (!Files.isRegularFile(geyserStandaloneJar) || Files.size(geyserStandaloneJar) < 4096L) {
                    downloadBinaryFileIfOnline(GEYSER_STANDALONE_DOWNLOAD_URL, geyserStandaloneJar);
                }

                // 2. Download Floodgate via Modrinth API
                if (!existingState.contains("\"floodgateLatestVersion\": \"" + this.activeFloodgateVersion + "\"")
                        || !Files.isRegularFile(floodgateJar)
                        || Files.size(floodgateJar) < 4096L) {
                    String floodgateDownloadUrl = extractModrinthJarDownloadUrl(floodgateJson);
                    if (!floodgateDownloadUrl.isBlank()) {
                        downloadBinaryFileIfOnline(floodgateDownloadUrl, floodgateJar);
                    }
                }

                // 3. Download spark via Modrinth API
                if (!existingState.contains("\"sparkLatestVersion\": \"" + this.activeSparkVersion + "\"")
                        || !Files.isRegularFile(sparkJar)
                        || Files.size(sparkJar) < 4096L) {
                    String sparkDownloadUrl = extractModrinthJarDownloadUrl(sparkJson);
                    if (!sparkDownloadUrl.isBlank()) {
                        downloadBinaryFileIfOnline(sparkDownloadUrl, sparkJar);
                    }
                }
            }

            if (!Files.isRegularFile(geyserJar)) {
                writeIntegratedCompanionJarIfNeeded(
                        geyserJar,
                        "geyser-vessel",
                        this.activeGeyserVersion,
                        ThirdPartyCreditsAndAttributionRegistry.GEYSER_AUTHORS);
            }
            if (!Files.isRegularFile(floodgateJar)) {
                writeIntegratedCompanionJarIfNeeded(
                        floodgateJar,
                        "floodgate-vessel",
                        this.activeFloodgateVersion,
                        ThirdPartyCreditsAndAttributionRegistry.FLOODGATE_AUTHORS);
            }
            if (!Files.isRegularFile(sparkJar)) {
                writeIntegratedCompanionJarIfNeeded(
                        sparkJar,
                        "spark-vessel",
                        this.activeSparkVersion,
                        ThirdPartyCreditsAndAttributionRegistry.SPARK_AUTHORS);
            }

            creditsRegistry.writeCreditsFileToServerRoot(
                    serverRoot,
                    this.activeGeyserVersion,
                    this.activeFloodgateVersion,
                    this.activeSparkVersion,
                    this.activeMinecraftVersion);

            String stateJson = """
                    {
                      "minecraftDefaultLatestRelease": "%s",
                      "geyserLatestVersion": "%s",
                      "floodgateLatestVersion": "%s",
                      "sparkLatestVersion": "%s",
                      "creditsFile": "CREDITS.md"
                    }
                    """.stripIndent().formatted(
                    this.activeMinecraftVersion,
                    this.activeGeyserVersion,
                    this.activeFloodgateVersion,
                    this.activeSparkVersion);
            Files.writeString(stateFile, stateJson, StandardCharsets.UTF_8);
            return stateFile;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to provision Geyser, Floodgate, and Spark ecosystem", e);
        }
    }

    /**
     * Returns the currently active/provisioned Geyser version.
     *
     * @return Geyser version string
     */
    public String getActiveGeyserVersion() {
        return activeGeyserVersion;
    }

    /**
     * Returns the currently active/provisioned Floodgate version.
     *
     * @return Floodgate version string
     */
    public String getActiveFloodgateVersion() {
        return activeFloodgateVersion;
    }

    /**
     * Returns the currently active/provisioned Spark version.
     *
     * @return Spark version string
     */
    public String getActiveSparkVersion() {
        return activeSparkVersion;
    }

    /**
     * Returns the currently active default Minecraft release version.
     *
     * @return Minecraft release version string
     */
    public String getActiveMinecraftVersion() {
        return activeMinecraftVersion;
    }

    private static boolean isOnlineHttpUrl(String endpoint) {
        return endpoint != null
                && (endpoint.startsWith("http://") || endpoint.startsWith("https://"));
    }

    private static String fetchJsonIfOnline(String endpoint) {
        if (!isOnlineHttpUrl(endpoint)) {
            return "";
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "VESSEL-Server-Loader/2.3 (https://github.com/VesselLoader/Vessel)")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200 && response.body() != null) {
                return response.body();
            }
        } catch (Exception ignored) {
            // Fall back to embedded latest version constants when offline
        }
        return "";
    }

    private static boolean downloadBinaryFileIfOnline(String downloadUrl, Path targetPath) {
        if (!isOnlineHttpUrl(downloadUrl)) {
            return false;
        }
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .timeout(Duration.ofSeconds(45))
                    .header("User-Agent", "VESSEL-Server-Loader/2.3 (https://github.com/VesselLoader/Vessel)")
                    .GET()
                    .build();
            Path tempFile = targetPath.resolveSibling(targetPath.getFileName() + ".part");
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200 && response.body() != null) {
                try (InputStream in = response.body()) {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                }
                if (Files.size(tempFile) > 1024L) {
                    Files.move(
                            tempFile,
                            targetPath,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                    return true;
                }
                Files.deleteIfExists(tempFile);
            }
        } catch (Exception ignored) {
            // Retain existing cached JAR or fallback companion stub on transient error
        }
        return false;
    }

    private static void writeIntegratedCompanionJarIfNeeded(
            Path targetJar,
            String moduleId,
            String version,
            String authors) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(baos)) {
            JarEntry entry = new JarEntry("fabric.mod.json");
            jos.putNextEntry(entry);
            String descriptor = "{\"id\":\"" + moduleId
                    + "\",\"version\":\"" + version
                    + "\",\"authors\":[\"" + authors + "\"]}";
            jos.write(descriptor.getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        Files.write(targetJar, baos.toByteArray());
    }
}
