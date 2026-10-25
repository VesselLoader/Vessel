package dev.vessel.bootstrap.updater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Sub-Stage 8.2: {@link VesselBuildVersionCode} comparison,
 * {@link UserDataPreservationSnapshotGuard} non-destructive user data protection,
 * {@link StagedKernelPatchManager} live update staging and restart application, and
 * {@link HourlyGitHubAutoUpdateDaemon} zero-lag hourly polling.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
class HourlyAutoUpdateAndStagedPatchTest {

    @Test
    void testVersionCodeParsingFormattingAndOlderEqualNewerComparison() {
        VesselBuildVersionCode comparator = new VesselBuildVersionCode();
        String vOld = comparator.formatVersionCode(
                "2.3.0", 1046, "1.21.4", "2.6.1-b756", "1.10.119");
        String vNew = comparator.formatVersionCode(
                "2.3.1", 1050, "26.1.2", "2.7.0-b810", "1.10.125");

        assertEquals(1046, comparator.extractRevisionNumber(vOld));
        assertEquals("1.21.4", comparator.extractBundledMinecraftVersion(vOld));
        assertEquals("2.6.1-b756", comparator.extractBundledGeyserVersion(vOld));
        assertEquals("1.10.119", comparator.extractBundledSparkVersion(vOld));

        assertEquals(-1, comparator.compareVersionCodes(vOld, vNew));
        assertEquals(0, comparator.compareVersionCodes(vNew, vNew));
        assertEquals(1, comparator.compareVersionCodes(vNew, vOld));
    }

    @Test
    void testLiveUpdateStagingRestartPatchApplicationAndZeroUserDataLoss(@TempDir Path tempDir)
            throws Exception {
        // 1. Populate user-owned directories and files (mods/, plugins/, config/, vessel.yml, world/)
        Path modsDir = tempDir.resolve("mods");
        Path pluginsDir = tempDir.resolve("plugins");
        Path configDir = tempDir.resolve("config");
        Path worldDir = tempDir.resolve("world");
        Files.createDirectories(modsDir);
        Files.createDirectories(pluginsDir);
        Files.createDirectories(configDir);
        Files.createDirectories(worldDir);

        Files.writeString(modsDir.resolve("custom-user-mod.txt"), "USER_MOD_DATA_V1", StandardCharsets.UTF_8);
        Files.writeString(pluginsDir.resolve("custom-user-plugin.txt"), "USER_PLUGIN_DATA_V1", StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve("user-settings.yml"), "setting: true\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("vessel.yml"), "custom-admin-config: 42\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("eula.txt"), "eula=true\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("server.properties"), "server-port=25565\n", StandardCharsets.UTF_8);
        Files.writeString(worldDir.resolve("level.dat"), "WORLD_SAVE_BINARY_STATE", StandardCharsets.UTF_8);

        UserDataPreservationSnapshotGuard dataGuard = new UserDataPreservationSnapshotGuard();
        assertTrue(dataGuard.isProtectedUserPath(tempDir, modsDir.resolve("custom-user-mod.txt")));
        assertTrue(dataGuard.isProtectedUserPath(tempDir, tempDir.resolve("vessel.yml")));
        assertFalse(dataGuard.isProtectedUserPath(tempDir, tempDir.resolve("cache/updates/staged")));
        assertThrows(
                SecurityException.class,
                () -> dataGuard.enforceNonDestructivePatchTarget(tempDir, tempDir.resolve("vessel.yml")));

        String initialUserDataDigest = dataGuard.computeUserDataIntegrityDigest(tempDir);

        // 2. Start hourly auto-update daemon & trigger a live check against a newer GitHub release
        HourlyGitHubAutoUpdateDaemon daemon = new HourlyGitHubAutoUpdateDaemon();
        daemon.startHourlyBackgroundUpdater(
                tempDir, HourlyGitHubAutoUpdateDaemon.DEFAULT_GITHUB_PAGES_MANIFEST_URL);
        assertTrue(daemon.isHourlySchedulerActive());

        String remoteManifestJson = """
                {
                  "versionCode": "VESSEL-2.4.0-rev1060+mc26.1.2-geyser2.7.0-b810-spark1.10.125",
                  "channel": "release"
                }
                """.stripIndent();
        String checkStatus = daemon.checkAndStageUpdateOnce(tempDir, remoteManifestJson);
        assertEquals(HourlyGitHubAutoUpdateDaemon.STATUS_UPDATE_STAGED, checkStatus);
        daemon.stopBackgroundUpdater();
        assertFalse(daemon.isHourlySchedulerActive());

        StagedKernelPatchManager patchManager = new StagedKernelPatchManager();
        assertTrue(patchManager.isRestartRequiredToApplyPatches(tempDir));

        // Verify ecosystem-versions.json and CREDITS.md were refreshed with latest Geyser/Spark/MC
        String ecosystemJson = Files.readString(
                tempDir.resolve("cache/ecosystem/ecosystem-versions.json"),
                StandardCharsets.UTF_8);
        assertTrue(ecosystemJson.contains("26.1.2"));
        assertTrue(ecosystemJson.contains("2.7.0-b810"));
        assertTrue(ecosystemJson.contains("1.10.125"));

        // Verify 100% of user data remained untouched during live staging
        assertEquals(initialUserDataDigest, dataGuard.computeUserDataIntegrityDigest(tempDir));

        // 3. Restart server -> staged patches are automatically applied on startup
        VesselServerLauncher restartedLauncher = new VesselServerLauncher(tempDir);
        int exitCode = restartedLauncher.executeBootSequence(new String[]{"--nogui"});
        assertEquals(0, exitCode);
        assertTrue(restartedLauncher.wasStagedPatchAppliedOnBoot());
        assertEquals(
                "VESSEL-2.4.0-rev1060+mc26.1.2-geyser2.7.0-b810-spark1.10.125",
                restartedLauncher.getActiveVesselVersionCode());
        assertEquals("26.1.2", restartedLauncher.getResolvedActiveMcVersion());
        assertFalse(patchManager.isRestartRequiredToApplyPatches(tempDir));

        // Verify subsequent check reports UP_TO_DATE_LATEST and user data is still 100% intact
        assertEquals(
                HourlyGitHubAutoUpdateDaemon.STATUS_UP_TO_DATE,
                daemon.checkAndStageUpdateOnce(tempDir, remoteManifestJson));
        assertEquals(initialUserDataDigest, dataGuard.computeUserDataIntegrityDigest(tempDir));
    }
}
