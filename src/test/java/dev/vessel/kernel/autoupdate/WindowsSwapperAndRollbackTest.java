package dev.vessel.kernel.autoupdate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite validating Windows file-lock safe dual-slot binary swapping,
 * cumulative rollforward patch application, and transactional rollback sentinel execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
class WindowsSwapperAndRollbackTest {

    @Test
    @DisplayName("Dual-slot swapper correctly determines default slot and toggles cleanly between A and B")
    void testDualSlotSwapperToggle(@TempDir Path tempDir) throws IOException {
        WindowsFileLockSafeSwapper swapper = new WindowsFileLockSafeSwapper();

        // 1. Initial slot should default to SLOT_A when no pointer file exists
        ServerBinarySlot initialSlot = swapper.determineActiveSlot(tempDir);
        assertThat(initialSlot).isEqualTo(ServerBinarySlot.SLOT_A);

        ServerBinarySlot standbySlot = swapper.determineStandbySlot(tempDir);
        assertThat(standbySlot).isEqualTo(ServerBinarySlot.SLOT_B);

        // 2. Stage new binary payload into standby (SLOT_B)
        byte[] payloadB = "VESSEL_BINARY_SLOT_B_REV101".getBytes(StandardCharsets.UTF_8);
        String sha256 = swapper.computeSha256Hex(payloadB);

        SlotSwapResult result = swapper.stageAndSwapToStandbySlot(tempDir, payloadB, sha256);
        assertThat(result.success()).isTrue();
        assertThat(result.previousSlot()).isEqualTo(ServerBinarySlot.SLOT_A);
        assertThat(result.activeSlot()).isEqualTo(ServerBinarySlot.SLOT_B);
        assertThat(Files.readAllBytes(result.targetJarPath())).isEqualTo(payloadB);

        // 3. Pointer should now reflect SLOT_B
        assertThat(swapper.determineActiveSlot(tempDir)).isEqualTo(ServerBinarySlot.SLOT_B);
        assertThat(swapper.determineStandbySlot(tempDir)).isEqualTo(ServerBinarySlot.SLOT_A);

        // 4. Stage another binary into standby (now SLOT_A)
        byte[] payloadA = "VESSEL_BINARY_SLOT_A_REV102".getBytes(StandardCharsets.UTF_8);
        SlotSwapResult result2 = swapper.stageAndSwapToStandbySlot(tempDir, payloadA, null);
        assertThat(result2.success()).isTrue();
        assertThat(result2.previousSlot()).isEqualTo(ServerBinarySlot.SLOT_B);
        assertThat(result2.activeSlot()).isEqualTo(ServerBinarySlot.SLOT_A);
        assertThat(swapper.determineActiveSlot(tempDir)).isEqualTo(ServerBinarySlot.SLOT_A);
    }

    @Test
    @DisplayName("Dual-slot swapper rejects payload on SHA-256 checksum mismatch")
    void testDualSlotChecksumMismatch(@TempDir Path tempDir) throws IOException {
        WindowsFileLockSafeSwapper swapper = new WindowsFileLockSafeSwapper();
        byte[] payload = "CORRUPT_PAYLOAD".getBytes(StandardCharsets.UTF_8);
        String invalidDigest = "deadbeefcafebabe000000000000000000000000000000000000000000000000";

        SlotSwapResult result = swapper.stageAndSwapToStandbySlot(tempDir, payload, invalidDigest);
        assertThat(result.success()).isFalse();
        assertThat(result.statusMessage()).contains("SHA-256 digest mismatch");
    }

    @Test
    @DisplayName("Windows lock-safe replaceLockedFileWithRetry safely replaces files using shadow renaming")
    void testReplaceLockedFileWithRetry(@TempDir Path tempDir) throws IOException {
        WindowsFileLockSafeSwapper swapper = new WindowsFileLockSafeSwapper();

        Path targetFile = tempDir.resolve("server.jar");
        Path replacementFile = tempDir.resolve("replacement.jar");

        Files.writeString(targetFile, "ORIGINAL_TARGET_JAR");
        Files.writeString(replacementFile, "REPLACEMENT_JAR_CONTENT");

        swapper.replaceLockedFileWithRetry(targetFile, replacementFile, 3, 50L);

        assertThat(Files.readString(targetFile)).isEqualTo("REPLACEMENT_JAR_CONTENT");
    }

    @Test
    @DisplayName("CumulativePatchRollforwardApplier validates contiguous patch chains and rejects gaps")
    void testCumulativePatchValidation() {
        CumulativePatchRollforwardApplier applier = new CumulativePatchRollforwardApplier();

        PatchRevision patch1 = new PatchRevision(100, 101, "P1", Collections.emptyMap(), Collections.emptySet(), "hash1");
        PatchRevision patch2 = new PatchRevision(101, 102, "P2", Collections.emptyMap(), Collections.emptySet(), "hash2");
        PatchRevision patchGap = new PatchRevision(103, 104, "P_GAP", Collections.emptyMap(), Collections.emptySet(), "hash3");

        // Continuous chain: 100 -> 101 -> 102
        applier.validatePatchChain(100, List.of(patch1, patch2));

        // Broken starting revision
        assertThatThrownBy(() -> applier.validatePatchChain(99, List.of(patch1, patch2)))
                .isInstanceOf(PatchChainValidationException.class)
                .hasMessageContaining("Revision continuity broken");

        // Gap in chain
        assertThatThrownBy(() -> applier.validatePatchChain(100, List.of(patch1, patchGap)))
                .isInstanceOf(PatchChainValidationException.class)
                .hasMessageContaining("Revision continuity broken");

        // Empty chain
        assertThatThrownBy(() -> applier.validatePatchChain(100, Collections.emptyList()))
                .isInstanceOf(PatchChainValidationException.class)
                .hasMessageContaining("Patch sequence is empty");
    }

    @Test
    @DisplayName("CumulativePatchRollforwardApplier applies multi-revision rollforward with delta entry updates and deletions")
    void testCumulativePatchApplication(@TempDir Path tempDir) throws IOException {
        CumulativePatchRollforwardApplier applier = new CumulativePatchRollforwardApplier();

        Path baseJar = tempDir.resolve("base-server.jar");
        createSampleJar(baseJar, 100, Map.of(
                "dev/vessel/Core.class", "CORE_BYTECODE_V1".getBytes(StandardCharsets.UTF_8),
                "dev/vessel/Obsolete.class", "OBSOLETE_BYTECODE".getBytes(StandardCharsets.UTF_8),
                "assets/config.json", "{ \"enabled\": true }".getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(applier.readRevisionFromJar(baseJar)).isEqualTo(100);

        // Patch 1: rev100 -> rev101 (Modifies Core.class, deletes Obsolete.class)
        PatchRevision patch1 = new PatchRevision(
                100,
                101,
                "PATCH_100_TO_101",
                Map.of("dev/vessel/Core.class", "CORE_BYTECODE_V2".getBytes(StandardCharsets.UTF_8)),
                Set.of("dev/vessel/Obsolete.class"),
                "digest-1"
        );

        // Patch 2: rev101 -> rev102 (Adds NewFeature.class, modifies config.json)
        PatchRevision patch2 = new PatchRevision(
                101,
                102,
                "PATCH_101_TO_102",
                Map.of(
                        "dev/vessel/NewFeature.class", "NEW_FEATURE_BYTECODE".getBytes(StandardCharsets.UTF_8),
                        "assets/config.json", "{ \"enabled\": true, \"v\": 2 }".getBytes(StandardCharsets.UTF_8)
                ),
                Collections.emptySet(),
                "digest-2"
        );

        Path patchedJar = tempDir.resolve("patched-server.jar");
        RollforwardApplicationResult result = applier.applyCumulativeRollforward(baseJar, patchedJar, List.of(patch1, patch2));

        assertThat(result.success()).isTrue();
        assertThat(result.initialRevision()).isEqualTo(100);
        assertThat(result.finalRevision()).isEqualTo(102);
        assertThat(result.appliedPatchCount()).isEqualTo(2);

        // Verify resulting jar revision marker and contents
        assertThat(applier.readRevisionFromJar(patchedJar)).isEqualTo(102);

        try (JarFile jar = new JarFile(patchedJar.toFile())) {
            // Core.class should be V2
            JarEntry coreEntry = jar.getJarEntry("dev/vessel/Core.class");
            assertThat(coreEntry).isNotNull();
            try (InputStream is = jar.getInputStream(coreEntry)) {
                assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("CORE_BYTECODE_V2");
            }

            // Obsolete.class should be deleted
            assertThat(jar.getJarEntry("dev/vessel/Obsolete.class")).isNull();

            // NewFeature.class should exist
            JarEntry newFeatureEntry = jar.getJarEntry("dev/vessel/NewFeature.class");
            assertThat(newFeatureEntry).isNotNull();

            // config.json should have v2 content
            JarEntry configEntry = jar.getJarEntry("assets/config.json");
            assertThat(configEntry).isNotNull();
            try (InputStream is = jar.getInputStream(configEntry)) {
                assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("{ \"enabled\": true, \"v\": 2 }");
            }
        }
    }

    @Test
    @DisplayName("TransactionalRollbackSentinel arms snapshot and performs automatic rollback on startup exception")
    void testTransactionalRollbackSentinelExecution(@TempDir Path tempDir) throws IOException {
        TransactionalRollbackSentinel sentinel = new TransactionalRollbackSentinel();
        WindowsFileLockSafeSwapper swapper = new WindowsFileLockSafeSwapper();

        // Setup initial server state: running on SLOT_A with binary
        Path slotABinary = swapper.resolveSlotBinaryPath(tempDir, ServerBinarySlot.SLOT_A);
        Files.writeString(slotABinary, "STABLE_BINARY_SLOT_A_V1");
        swapper.commitActiveSlotPointer(tempDir, ServerBinarySlot.SLOT_A);

        assertThat(sentinel.readSentinelState(tempDir)).isEqualTo(SentinelMigrationState.STABLE);

        // 1. Arm Sentinel before deploying target version 2.0.0
        RollbackSnapshot snapshot = sentinel.armRollbackSentinel(
                tempDir,
                ServerBinarySlot.SLOT_A,
                "1.0.0",
                "2.0.0"
        );

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.snapshotSlot()).isEqualTo(ServerBinarySlot.SLOT_A);
        assertThat(Files.isRegularFile(snapshot.backupBinaryPath())).isTrue();
        assertThat(Files.readString(snapshot.backupBinaryPath())).isEqualTo("STABLE_BINARY_SLOT_A_V1");
        assertThat(sentinel.readSentinelState(tempDir)).isEqualTo(SentinelMigrationState.UPGRADING_PENDING_HEALTHCHECK);

        // 2. Simulate upgrade switching to SLOT_B
        Path slotBBinary = swapper.resolveSlotBinaryPath(tempDir, ServerBinarySlot.SLOT_B);
        Files.writeString(slotBBinary, "BUGGY_NEW_BINARY_SLOT_B_V2");
        swapper.commitActiveSlotPointer(tempDir, ServerBinarySlot.SLOT_B);
        assertThat(swapper.determineActiveSlot(tempDir)).isEqualTo(ServerBinarySlot.SLOT_B);

        // 3. Simulate startup crash throwing unrecoverable exception
        IllegalStateException startupCrash = new IllegalStateException("Simulated ClassNotFoundException on bootstrap");
        RollbackExecutionResult rollbackResult = sentinel.detectAndExecuteAutomaticRollback(tempDir, startupCrash);

        assertThat(rollbackResult.rolledBackSuccessfully()).isTrue();
        assertThat(rollbackResult.restoredSlot()).isEqualTo(ServerBinarySlot.SLOT_A);
        assertThat(sentinel.readSentinelState(tempDir)).isEqualTo(SentinelMigrationState.BOOTSTRAP_FAILED_ROLLED_BACK);

        // Active slot should be restored to SLOT_A
        assertThat(swapper.determineActiveSlot(tempDir)).isEqualTo(ServerBinarySlot.SLOT_A);
        assertThat(Files.readString(slotABinary)).isEqualTo("STABLE_BINARY_SLOT_A_V1");
    }

    @Test
    @DisplayName("TransactionalRollbackSentinel confirms successful startup transitioning to STABLE")
    void testConfirmSuccessfulStartup(@TempDir Path tempDir) throws IOException {
        TransactionalRollbackSentinel sentinel = new TransactionalRollbackSentinel();

        sentinel.armRollbackSentinel(tempDir, ServerBinarySlot.SLOT_A, "1.0.0", "1.0.1");
        assertThat(sentinel.readSentinelState(tempDir)).isEqualTo(SentinelMigrationState.UPGRADING_PENDING_HEALTHCHECK);

        sentinel.confirmSuccessfulStartup(tempDir);
        assertThat(sentinel.readSentinelState(tempDir)).isEqualTo(SentinelMigrationState.STABLE);
    }

    private void createSampleJar(Path targetJar, int revision, Map<String, byte[]> entries) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(targetJar))) {
            // Write revision entry
            JarEntry revEntry = new JarEntry(CumulativePatchRollforwardApplier.REVISION_METADATA_ENTRY);
            jos.putNextEntry(revEntry);
            jos.write(String.valueOf(revision).getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();

            // Write test entries
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                JarEntry jarEntry = new JarEntry(entry.getKey());
                jos.putNextEntry(jarEntry);
                jos.write(entry.getValue());
                jos.closeEntry();
            }
        }
    }
}
