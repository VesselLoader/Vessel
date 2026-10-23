package dev.vessel.bootstrap.updater;

import dev.vessel.bootstrap.ecosystem.BuiltInGeyserAndSparkProvisioner;
import dev.vessel.bootstrap.mojang.MojangVersionManifestResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Manages live staging of VESSEL updates and patches while the server is running, and applies
 * staged patches cleanly on the next server restart without touching any user data.
 *
 * <p>Every staged update also synchronizes the latest bundled Geyser release, latest Spark
 * release, and latest default Minecraft release version (`latest.release`).</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class StagedKernelPatchManager {

    public static final String ACTIVE_VERSION_FILE = "active-vessel-version.txt";
    public static final String PENDING_VERSION_FILE = "pending-version-code.txt";
    public static final String STAGED_PATCH_FILE = "vessel-staged-patch.bin";
    public static final String APPLIED_PATCH_FILE = "applied-vessel-patch.bin";

    private final VesselBuildVersionCode versionCodeParser = new VesselBuildVersionCode();
    private final UserDataPreservationSnapshotGuard dataGuard =
            new UserDataPreservationSnapshotGuard();
    private final BuiltInGeyserAndSparkProvisioner ecosystemProvisioner =
            new BuiltInGeyserAndSparkProvisioner();

    /**
     * Reads the currently active VESSEL version code for the given server root, initializing it to
     * {@link VesselBuildVersionCode#CURRENT_EMBEDDED_VERSION_CODE} if not yet persisted.
     *
     * @param serverRoot the server root directory
     * @return active canonical VESSEL version code string
     */
    public String readActiveVersionCode(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path updatesDir = serverRoot.resolve("cache").resolve("updates");
        Path activeFile = updatesDir.resolve(ACTIVE_VERSION_FILE);
        try {
            if (Files.isRegularFile(activeFile)) {
                String stored = Files.readString(activeFile, StandardCharsets.UTF_8).trim();
                if (!stored.isEmpty()) {
                    return stored;
                }
            }
            Files.createDirectories(updatesDir);
            dataGuard.enforceNonDestructivePatchTarget(serverRoot, activeFile);
            Files.writeString(
                    activeFile,
                    VesselBuildVersionCode.CURRENT_EMBEDDED_VERSION_CODE,
                    StandardCharsets.UTF_8);
            return VesselBuildVersionCode.CURRENT_EMBEDDED_VERSION_CODE;
        } catch (IOException e) {
            return VesselBuildVersionCode.CURRENT_EMBEDDED_VERSION_CODE;
        }
    }

    /**
     * Stages a newly downloaded VESSEL update and patch payload while the server is live without
     * interrupting gameplay or modifying user files, and updates the cached Geyser, Spark, and
     * Minecraft {@code latest.release} descriptors.
     *
     * @param serverRoot        the server root directory
     * @param remoteVersionCode the newer version code discovered on GitHub
     * @param patchPayload      the binary patch/bundle payload
     */
    public void stageUpdateWhileServerRunning(
            Path serverRoot,
            String remoteVersionCode,
            byte[] patchPayload) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(remoteVersionCode, "remoteVersionCode must not be null");
        Objects.requireNonNull(patchPayload, "patchPayload must not be null");

        Path stagedDir = serverRoot.resolve("cache").resolve("updates").resolve("staged");
        Path pendingVersionPath = stagedDir.resolve(PENDING_VERSION_FILE);
        Path stagedPatchPath = stagedDir.resolve(STAGED_PATCH_FILE);

        dataGuard.enforceNonDestructivePatchTarget(serverRoot, pendingVersionPath);
        dataGuard.enforceNonDestructivePatchTarget(serverRoot, stagedPatchPath);

        try {
            Files.createDirectories(stagedDir);
            Files.write(stagedPatchPath, patchPayload);
            Files.writeString(pendingVersionPath, remoteVersionCode.trim(), StandardCharsets.UTF_8);

            // Sync bundled latest Minecraft version, Geyser version, and Spark version
            String latestMc = versionCodeParser.extractBundledMinecraftVersion(remoteVersionCode);
            String latestGeyser = versionCodeParser.extractBundledGeyserVersion(remoteVersionCode);
            String latestSpark = versionCodeParser.extractBundledSparkVersion(remoteVersionCode);

            updateCachedMinecraftReleaseManifest(serverRoot.resolve("cache"), latestMc);
            ecosystemProvisioner.provisionAndSyncLatestEcosystem(
                    serverRoot,
                    latestMc,
                    "offline://" + latestGeyser,
                    "offline://" + latestSpark);

            Path ecosystemState = serverRoot.resolve("cache")
                    .resolve("ecosystem")
                    .resolve("ecosystem-versions.json");
            dataGuard.enforceNonDestructivePatchTarget(serverRoot, ecosystemState);
            String updatedEcosystemJson = """
                    {
                      "minecraftDefaultLatestRelease": "%s",
                      "geyserLatestVersion": "%s",
                      "sparkLatestVersion": "%s",
                      "creditsFile": "CREDITS.md"
                    }
                    """.stripIndent().formatted(latestMc, latestGeyser, latestSpark);
            Files.writeString(ecosystemState, updatedEcosystemJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to stage live VESSEL update", e);
        }
    }

    /**
     * Checks whether a staged update has been downloaded while the server was running and is
     * awaiting a server restart to apply its kernel patches.
     *
     * @param serverRoot the server root directory
     * @return {@code true} if staged patches are waiting for a server restart
     */
    public boolean isRestartRequiredToApplyPatches(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path pendingFile = serverRoot.resolve("cache")
                .resolve("updates")
                .resolve("staged")
                .resolve(PENDING_VERSION_FILE);
        return Files.isRegularFile(pendingFile);
    }

    /**
     * Applies any pending staged patches at the start of a server restart, promoting the staged
     * version code to the active version code and clearing the restart-required flag.
     *
     * @param serverRoot the server root directory
     * @return {@code true} if staged patches were found and applied during this startup
     */
    public boolean applyPendingStagedPatchesOnStartup(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path updatesDir = serverRoot.resolve("cache").resolve("updates");
        Path stagedDir = updatesDir.resolve("staged");
        Path pendingVersionPath = stagedDir.resolve(PENDING_VERSION_FILE);
        Path stagedPatchPath = stagedDir.resolve(STAGED_PATCH_FILE);
        if (!Files.isRegularFile(pendingVersionPath)) {
            return false;
        }

        Path activeVersionPath = updatesDir.resolve(ACTIVE_VERSION_FILE);
        Path appliedPatchPath = updatesDir.resolve(APPLIED_PATCH_FILE);
        dataGuard.enforceNonDestructivePatchTarget(serverRoot, activeVersionPath);
        dataGuard.enforceNonDestructivePatchTarget(serverRoot, appliedPatchPath);

        try {
            Files.createDirectories(updatesDir);
            String newVersionCode =
                    Files.readString(pendingVersionPath, StandardCharsets.UTF_8).trim();
            if (Files.isRegularFile(stagedPatchPath)) {
                Files.move(
                        stagedPatchPath,
                        appliedPatchPath,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(activeVersionPath, newVersionCode, StandardCharsets.UTF_8);
            Files.deleteIfExists(pendingVersionPath);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to apply staged VESSEL patches on startup", e);
        }
    }

    private void updateCachedMinecraftReleaseManifest(Path cacheDir, String latestMc)
            throws IOException {
        Files.createDirectories(cacheDir);
        Path manifestFile = cacheDir.resolve(MojangVersionManifestResolver.CACHED_MANIFEST_FILE);
        String updatedManifest = """
                {
                  "latest": {
                    "release": "%s",
                    "snapshot": "%s"
                  },
                  "versions": [
                    {
                      "id": "%s",
                      "type": "release",
                      "url": "https://piston-meta.mojang.com/v1/packages/%s/%s.json"
                    }
                  ]
                }
                """.stripIndent().formatted(latestMc, latestMc, latestMc, latestMc, latestMc);
        Files.writeString(manifestFile, updatedManifest, StandardCharsets.UTF_8);
    }
}
