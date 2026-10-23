package dev.vessel.bootstrap.mojang;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the latest official Minecraft release version (`latest.release`) and per-version
 * metadata URLs from Mojang's Piston Meta API
 * ({@code https://piston-meta.mojang.com/mc/game/version_manifest_v2.json}), with transparent
 * local disk caching (per-version {@code cache/meta/<version>.json}) and offline fallback.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class MojangVersionManifestResolver {

    public static final String DEFAULT_MANIFEST_URL =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String CACHED_MANIFEST_FILE = "version_manifest_v2.json";
    public static final String FALLBACK_LATEST_RELEASE = "1.21.4";

    private static final Pattern LATEST_RELEASE_PATTERN = Pattern.compile(
            "\"latest\"\\s*:\\s*\\{[^}]*\"release\"\\s*:\\s*\"([^\"]+)\"",
            Pattern.DOTALL);

    /**
     * Extracts the {@code latest.release} version identifier from a Mojang version manifest JSON
     * payload.
     *
     * @param manifestJson the raw {@code version_manifest_v2.json} content
     * @return the latest release version string (e.g., {@code "1.21.4"})
     */
    public String resolveLatestReleaseVersion(String manifestJson) {
        Objects.requireNonNull(manifestJson, "manifestJson must not be null");
        Matcher matcher = LATEST_RELEASE_PATTERN.matcher(manifestJson);
        if (matcher.find()) {
            String resolved = matcher.group(1).trim();
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        return FALLBACK_LATEST_RELEASE;
    }

    /**
     * Fetches the latest Mojang version manifest from {@code manifestUrl} (or loads the cached copy
     * in {@code cacheDir} if offline) and persists it to {@code cacheDir/version_manifest_v2.json}.
     *
     * @param cacheDir    the local cache directory
     * @param manifestUrl the Mojang version manifest endpoint URL
     * @return the manifest JSON string
     */
    public String fetchOrLoadCachedVersionManifest(Path cacheDir, String manifestUrl) {
        Objects.requireNonNull(cacheDir, "cacheDir must not be null");
        Objects.requireNonNull(manifestUrl, "manifestUrl must not be null");
        Path cachedFile = cacheDir.resolve(CACHED_MANIFEST_FILE);

        try {
            Files.createDirectories(cacheDir);
            if (manifestUrl.startsWith("http://") || manifestUrl.startsWith("https://")) {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(manifestUrl))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                    Files.writeString(cachedFile, response.body(), StandardCharsets.UTF_8);
                    return response.body();
                }
            }
        } catch (Exception ignored) {
            // Fall through to cached manifest or embedded fallback manifest when offline
        }

        try {
            if (Files.exists(cachedFile)) {
                String cached = Files.readString(cachedFile, StandardCharsets.UTF_8);
                if (!cached.isBlank()) {
                    return cached;
                }
            }
            String fallbackJson = buildEmbeddedFallbackManifest();
            Files.writeString(cachedFile, fallbackJson, StandardCharsets.UTF_8);
            return fallbackJson;
        } catch (IOException e) {
            return buildEmbeddedFallbackManifest();
        }
    }

    /**
     * Fetches or loads the cached per-version detail metadata JSON for {@code targetVersion},
     * storing it in {@code metaCacheDir/<targetVersion>.json} so multiple Minecraft versions never
     * overwrite one another's cached metadata.
     *
     * @param metaCacheDir   the metadata cache directory (e.g. {@code cache/meta})
     * @param targetVersion  the target Minecraft version ID (e.g. {@code "1.16.5"}, {@code "1.21.11"}, {@code "26.3"})
     * @param versionMetaUrl the version metadata URL from {@code version_manifest_v2.json}
     * @return the version detail JSON string, or an empty string if unavailable
     */
    public String fetchOrLoadVersionDetailJson(
            Path metaCacheDir,
            String targetVersion,
            String versionMetaUrl) {
        Objects.requireNonNull(metaCacheDir, "metaCacheDir must not be null");
        Objects.requireNonNull(targetVersion, "targetVersion must not be null");
        Objects.requireNonNull(versionMetaUrl, "versionMetaUrl must not be null");

        String safeFileName = targetVersion.trim().replaceAll("[^a-zA-Z0-9._-]", "_") + ".json";
        Path cachedFile = metaCacheDir.resolve(safeFileName);
        try {
            Files.createDirectories(metaCacheDir);
            if (Files.isRegularFile(cachedFile)) {
                String existing = Files.readString(cachedFile, StandardCharsets.UTF_8);
                if (!existing.isBlank() && existing.contains("\"downloads\"")) {
                    return existing;
                }
            }
            if (versionMetaUrl.startsWith("http://") || versionMetaUrl.startsWith("https://")) {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(8))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(versionMetaUrl))
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                    Files.writeString(cachedFile, response.body(), StandardCharsets.UTF_8);
                    return response.body();
                }
            }
        } catch (Exception ignored) {
            // Fall back to cached file if present
        }
        try {
            if (Files.isRegularFile(cachedFile)) {
                return Files.readString(cachedFile, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // Ignore read error
        }
        return "";
    }

    /**
     * Extracts the version-specific metadata JSON URL for {@code targetVersion} from the Mojang
     * version manifest JSON.
     *
     * @param manifestJson  the raw {@code version_manifest_v2.json} content
     * @param targetVersion the target Minecraft version ID (e.g., {@code "1.16.5"}, {@code "1.21.11"}, {@code "26.3"})
     * @return the version metadata URL, or an empty string if not present
     */
    public String extractVersionMetadataUrl(String manifestJson, String targetVersion) {
        Objects.requireNonNull(manifestJson, "manifestJson must not be null");
        Objects.requireNonNull(targetVersion, "targetVersion must not be null");
        String quotedId = "\"id\"";
        int searchIndex = 0;
        while (true) {
            int idPos = manifestJson.indexOf(quotedId, searchIndex);
            if (idPos < 0) {
                break;
            }
            int objStart = manifestJson.lastIndexOf('{', idPos);
            int objEnd = manifestJson.indexOf('}', idPos);
            if (objStart >= 0 && objEnd > idPos) {
                String entryBlock = manifestJson.substring(objStart, objEnd + 1);
                String idVal = extractJsonStringField(entryBlock, "id");
                if (targetVersion.trim().equals(idVal)) {
                    String urlVal = extractJsonStringField(entryBlock, "url");
                    if (!urlVal.isEmpty()) {
                        return urlVal;
                    }
                }
            }
            searchIndex = idPos + quotedId.length();
        }
        return "";
    }

    private static String extractJsonStringField(String jsonBlock, String fieldName) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(jsonBlock);
        return m.find() ? m.group(1).trim() : "";
    }

    private static String buildEmbeddedFallbackManifest() {
        return """
                {
                  "latest": {
                    "release": "1.21.4",
                    "snapshot": "25w09b"
                  },
                  "versions": [
                    {
                      "id": "26.3",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/61fff1316bd50312624a858bbb5eb18fa45208ed/26.3.json"
                    },
                    {
                      "id": "1.21.11",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/240f9560a4a6bc07faf42a7032a96b225f76e658/1.21.11.json"
                    },
                    {
                      "id": "1.21.4",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/1.21.4/1.21.4.json"
                    },
                    {
                      "id": "1.21.1",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/1.21.1/1.21.1.json"
                    },
                    {
                      "id": "1.20.1",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/38d412af3b247e485c7f7e50ac25febc4e79ab1d/1.20.1.json"
                    },
                    {
                      "id": "1.16.5",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/fba9f7833e858a1257d810d21a9e3c967f9077/1.16.5.json"
                    },
                    {
                      "id": "1.12.2",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/832d95b9f40699d4961394dcf6cf549e65f15dc5/1.12.2.json"
                    }
                  ]
                }
                """.stripIndent();
    }
}
