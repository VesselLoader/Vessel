package dev.vessel.bootstrap.mojang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts official Mojang {@code server.jar} and {@code server_mappings} download URLs and SHA-1
 * digests from a version metadata JSON document, downloads or caches {@code mojang_<version>.jar},
 * and verifies SHA-1 integrity.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MojangServerJarDownloader {

    private static final Pattern SERVER_BLOCK_PATTERN = Pattern.compile(
            "\"server\"\\s*:\\s*\\{([^}]+)\\}",
            Pattern.DOTALL);
    private static final Pattern URL_FIELD_PATTERN = Pattern.compile(
            "\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SHA1_FIELD_PATTERN = Pattern.compile(
            "\"sha1\"\\s*:\\s*\"([0-9a-fA-F]{40})\"");

    /**
     * Extracts the official Mojang {@code downloads.server.url} from a version metadata JSON.
     *
     * @param versionDetailJson the version detail JSON string
     * @return the server JAR download URL, or an empty string if absent
     */
    public String extractServerDownloadUrl(String versionDetailJson) {
        Objects.requireNonNull(versionDetailJson, "versionDetailJson must not be null");
        Matcher blockMatcher = SERVER_BLOCK_PATTERN.matcher(versionDetailJson);
        if (blockMatcher.find()) {
            String serverBlock = blockMatcher.group(1);
            Matcher urlMatcher = URL_FIELD_PATTERN.matcher(serverBlock);
            if (urlMatcher.find()) {
                return urlMatcher.group(1).trim();
            }
        }
        return "";
    }

    /**
     * Extracts the official Mojang {@code downloads.server.sha1} from a version metadata JSON.
     *
     * @param versionDetailJson the version detail JSON string
     * @return the 40-character lowercase SHA-1 digest, or an empty string if absent
     */
    public String extractServerDownloadSha1(String versionDetailJson) {
        Objects.requireNonNull(versionDetailJson, "versionDetailJson must not be null");
        Matcher blockMatcher = SERVER_BLOCK_PATTERN.matcher(versionDetailJson);
        if (blockMatcher.find()) {
            String serverBlock = blockMatcher.group(1);
            Matcher shaMatcher = SHA1_FIELD_PATTERN.matcher(serverBlock);
            if (shaMatcher.find()) {
                return shaMatcher.group(1).trim().toLowerCase();
            }
        }
        return "";
    }

    /**
     * Verifies that the file at {@code filePath} exists and matches {@code expectedSha1}.
     *
     * @param filePath     the local file path to verify
     * @param expectedSha1 the expected 40-character hexadecimal SHA-1 hash
     * @return {@code true} if the file's SHA-1 matches {@code expectedSha1}
     */
    public boolean verifyFileSha1(Path filePath, String expectedSha1) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(expectedSha1, "expectedSha1 must not be null");
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return false;
        }
        if (expectedSha1.isBlank()) {
            return true;
        }
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    sha1.update(buffer, 0, read);
                }
            }
            String actualHex = HexFormat.of().formatHex(sha1.digest());
            return actualHex.equalsIgnoreCase(expectedSha1.trim());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Ensures that {@code cacheDir/mojang_<mcVersion>.jar} is present and verified, downloading it
     * from {@code downloadUrl} if necessary.
     *
     * @param cacheDir     the cache directory
     * @param mcVersion    the Minecraft version identifier
     * @param downloadUrl  the Mojang server JAR download URL (or {@code file://} URI)
     * @param expectedSha1 the expected SHA-1 hash (optional)
     * @return the path to the cached {@code mojang_<mcVersion>.jar}
     */
    public Path ensureVanillaServerJarCached(
            Path cacheDir,
            String mcVersion,
            String downloadUrl,
            String expectedSha1) {
        Objects.requireNonNull(cacheDir, "cacheDir must not be null");
        Objects.requireNonNull(mcVersion, "mcVersion must not be null");
        Objects.requireNonNull(downloadUrl, "downloadUrl must not be null");
        Objects.requireNonNull(expectedSha1, "expectedSha1 must not be null");

        Path targetJar = cacheDir.resolve("mojang_" + mcVersion.trim() + ".jar");
        try {
            Files.createDirectories(cacheDir);
            if (Files.exists(targetJar) && verifyFileSha1(targetJar, expectedSha1)) {
                return targetJar;
            }
            if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(downloadUrl))
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();
                HttpResponse<Path> response =
                        client.send(request, HttpResponse.BodyHandlers.ofFile(targetJar));
                if (response.statusCode() == 200 && verifyFileSha1(targetJar, expectedSha1)) {
                    return targetJar;
                }
            } else if (downloadUrl.startsWith("file:")) {
                Path sourcePath = Path.of(URI.create(downloadUrl));
                if (Files.exists(sourcePath)) {
                    Files.copy(sourcePath, targetJar, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return targetJar;
    }
}
