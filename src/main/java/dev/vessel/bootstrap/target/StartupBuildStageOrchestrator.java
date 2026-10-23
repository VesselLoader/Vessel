package dev.vessel.bootstrap.target;

import dev.vessel.kernel.identity.SourceLoaderKind;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Orchestrates Step 11 (Startup Build Stage) under Architectural Decisions 1, 2, 3, 19 and Lesson L7.
 *
 * <ul>
 *   <li><b>Decision 1 &amp; 19:</b> Eagerly compiles all client render bundles at Step 11 before
 *       loading worlds or binding the network port.</li>
 *   <li><b>Decision 2:</b> On first run without {@code --vessel-version}, outputs the 45-second
 *       English warning banner, writes {@code vessel-first-run-ack.txt}, and signals clean exit {@code 0}.</li>
 *   <li><b>Decision 3 &amp; Lesson L7:</b> Writes the {@code .input} SHA-256 sidecar file last;
 *       purges any interrupted partial build directory lacking a valid {@code .input} sidecar while
 *       preserving completed bundles.</li>
 * </ul>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-29)
 */
public final class StartupBuildStageOrchestrator {

    public static final String FIRST_RUN_SENTINEL_FILE = "vessel-first-run-ack.txt";
    public static final String COMPLETED_SIDECAR_FILE = ".input";

    private final Map<String, byte[]> compiledRenderBundles = new ConcurrentHashMap<>();

    /**
     * Checks whether the server is permitted to proceed past the first-run version-scoping check
     * (Decision 2).
     *
     * <p>If {@code vessel-first-run-ack.txt} does not exist and {@code cliVersions} is empty,
     * writes the sentinel file and returns {@code false} so the launcher exits with status {@code 0}
     * after displaying the 45-second warning banner.</p>
     *
     * @param serverRoot  server root directory
     * @param cliVersions list of versions passed via {@code --vessel-version}
     * @return {@code true} if boot should continue; {@code false} if first-run exit {@code 0} is scheduled
     */
    public boolean checkFirstRunSentinelOrScheduleExit(Path serverRoot, List<String> cliVersions) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        Objects.requireNonNull(cliVersions, "cliVersions cannot be null");

        Path sentinelPath = serverRoot.resolve(FIRST_RUN_SENTINEL_FILE);
        if (Files.exists(sentinelPath)) {
            return true;
        }

        try {
            Files.createDirectories(serverRoot);
            if (!cliVersions.isEmpty()) {
                Files.writeString(
                        sentinelPath,
                        "ACK_WITH_CLI_VERSIONS=" + String.join(",", cliVersions) + "\n",
                        StandardCharsets.UTF_8
                );
                return true;
            }

            Files.writeString(
                    sentinelPath,
                    formatFirstRun45SecondWarning() + "\n",
                    StandardCharsets.UTF_8
            );
            return false;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read or write first-run sentinel at " + sentinelPath, e);
        }
    }

    /**
     * Formats the mandatory 45-second English first-run notice explaining how to scope versions
     * using {@code --vessel-version} or {@code vessel-config/versions.yml} (Decision 2).
     *
     * @return formatted English warning message
     */
    public String formatFirstRun45SecondWarning() {
        return """
                ================================================================================
                [VESSEL FIRST-RUN NOTICE — 45-SECOND OPERATOR ADVISORY]
                No --vessel-version flag was provided on this initial launch.
                If you continue on the next startup without scoping versions via:
                  --vessel-version="26.2","26.1","1.21.1"
                or editing vessel-config/versions.yml, VESSEL will eagerly pre-build render
                bundles for ALL supported (Minecraft Version x Source Loader) combinations
                during Step 11 (Startup Build Stage) before opening the server port.
                Sentinel written to vessel-first-run-ack.txt. Exiting cleanly (code 0).
                ================================================================================
                """.stripIndent();
    }

    /**
     * Eagerly compiles and caches a render bundle for the given {@code (minecraftVersion, loader)}
     * combination during Step 11 (Decision 1).
     *
     * @param minecraftVersion target Minecraft version string
     * @param loader           source loader ecosystem kind
     * @param payloadBytes     raw asset/model payload bytes
     */
    public void buildRenderBundleForCombination(
            String minecraftVersion,
            SourceLoaderKind loader,
            byte[] payloadBytes
    ) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(payloadBytes, "payloadBytes cannot be null");

        String key = minecraftVersion.trim() + "::" + loader.getId();
        compiledRenderBundles.put(key, payloadBytes.clone());
    }

    /**
     * Writes the terminal {@code .input} SHA-256 sidecar file inside a bundle build directory
     * as the final atomic completion marker (Lesson L7).
     *
     * @param bundleDir directory containing the compiled bundle artifacts
     * @param sha256Hex lowercase SHA-256 hex digest of the bundle inputs
     */
    public void writeCompletedSidecarHash(Path bundleDir, String sha256Hex) {
        Objects.requireNonNull(bundleDir, "bundleDir cannot be null");
        Objects.requireNonNull(sha256Hex, "sha256Hex cannot be null");
        if (sha256Hex.isBlank()) {
            throw new IllegalArgumentException("sha256Hex cannot be blank");
        }

        try {
            Files.createDirectories(bundleDir);
            Path sidecarFile = bundleDir.resolve(COMPLETED_SIDECAR_FILE);
            Files.writeString(sidecarFile, sha256Hex.trim() + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write sidecar hash at " + bundleDir, e);
        }
    }

    /**
     * Scans the build cache root directory and deletes any interrupted partial build subdirectory
     * that is missing a non-empty {@code .input} sidecar file, while preserving all completed builds
     * (Decision 3 &amp; Lesson L7).
     *
     * @param buildCacheRoot root directory of cached bundle builds
     * @return number of interrupted partial build directories purged
     */
    public int purgeInterruptedPartialBuilds(Path buildCacheRoot) {
        Objects.requireNonNull(buildCacheRoot, "buildCacheRoot cannot be null");
        if (!Files.exists(buildCacheRoot) || !Files.isDirectory(buildCacheRoot)) {
            return 0;
        }

        int purgedCount = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildCacheRoot)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    continue;
                }
                Path sidecar = entry.resolve(COMPLETED_SIDECAR_FILE);
                boolean validCompletedBuild = false;
                if (Files.exists(sidecar) && Files.isRegularFile(sidecar)) {
                    String content = Files.readString(sidecar, StandardCharsets.UTF_8).trim();
                    validCompletedBuild = !content.isEmpty();
                }

                if (!validCompletedBuild) {
                    deleteDirectoryRecursively(entry);
                    purgedCount++;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to purge interrupted partial builds in " + buildCacheRoot, e);
        }
        return purgedCount;
    }

    /**
     * Computes a SHA-256 hex digest for the given input payload bytes.
     *
     * @param inputBytes raw payload bytes
     * @return lowercase 64-character SHA-256 hex string
     */
    public String computeInputSha256Hex(byte[] inputBytes) {
        Objects.requireNonNull(inputBytes, "inputBytes cannot be null");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(inputBytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public int getCompiledRenderBundleCount() {
        return compiledRenderBundles.size();
    }

    private static void deleteDirectoryRecursively(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                Files.deleteIfExists(p);
            }
        }
    }
}
