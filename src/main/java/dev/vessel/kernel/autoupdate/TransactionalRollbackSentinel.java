package dev.vessel.kernel.autoupdate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Transactional rollback sentinel managing pre-upgrade state snapshots and triggering automated,
 * zero-data-loss rollback restoration if a newly deployed server binary throws unrecoverable exceptions
 * during bootstrap or startup health checks.
 *
 * <p>Preserves all user worlds, configs, and plugins, rolling back only the active binary slot and
 * version metadata to the proven stable snapshot.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class TransactionalRollbackSentinel {

    public static final String SENTINEL_PROPERTIES_FILE = "migration-sentinel.properties";
    public static final String SNAPSHOTS_BASE_DIR = "cache/snapshots";
    public static final String BACKUP_JAR_NAME = "backup-binary.jar";

    public static final String KEY_STATE = "sentinel.state";
    public static final String KEY_SNAPSHOT_ID = "sentinel.snapshot_id";
    public static final String KEY_PREVIOUS_SLOT = "sentinel.previous_slot";
    public static final String KEY_PREVIOUS_VERSION = "sentinel.previous_version";
    public static final String KEY_TARGET_VERSION = "sentinel.target_version";
    public static final String KEY_TIMESTAMP_MILLIS = "sentinel.timestamp_millis";
    public static final String KEY_BACKUP_BINARY_PATH = "sentinel.backup_binary_path";
    public static final String KEY_FAILURE_REASON = "sentinel.failure_reason";
    public static final String KEY_RESTORED_AT_MILLIS = "sentinel.restored_at_millis";

    private final WindowsFileLockSafeSwapper swapper = new WindowsFileLockSafeSwapper();

    /**
     * Reads the current migration state from {@code migration-sentinel.properties}.
     *
     * @param serverRoot root directory of the server
     * @return current {@link SentinelMigrationState}, defaulting to {@link SentinelMigrationState#STABLE}
     */
    public SentinelMigrationState readSentinelState(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path propFile = serverRoot.resolve(SENTINEL_PROPERTIES_FILE);
        if (!Files.isRegularFile(propFile)) {
            return SentinelMigrationState.STABLE;
        }
        Properties props = loadPropertiesSilently(propFile);
        String rawState = props.getProperty(KEY_STATE, SentinelMigrationState.STABLE.name());
        return SentinelMigrationState.fromString(rawState);
    }

    /**
     * Captures a rollback snapshot of the current binary and arms the sentinel into
     * {@link SentinelMigrationState#UPGRADING_PENDING_HEALTHCHECK} prior to deploying an update.
     *
     * @param serverRoot     root directory of the server
     * @param currentSlot    the active slot prior to update
     * @param currentVersion active version identifier
     * @param targetVersion  version about to be installed
     * @return created {@link RollbackSnapshot}
     * @throws SentinelStorageException if snapshot directories or backups cannot be created
     */
    public RollbackSnapshot armRollbackSentinel(
            Path serverRoot,
            ServerBinarySlot currentSlot,
            String currentVersion,
            String targetVersion) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(currentSlot, "currentSlot must not be null");
        Objects.requireNonNull(currentVersion, "currentVersion must not be null");
        Objects.requireNonNull(targetVersion, "targetVersion must not be null");

        long now = System.currentTimeMillis();
        String snapshotId = "snapshot-" + now + "-" + currentSlot.name();
        Path snapshotDir = serverRoot.resolve(SNAPSHOTS_BASE_DIR).resolve(snapshotId);

        try {
            Files.createDirectories(snapshotDir);

            // Backup existing binary if it exists
            Path currentBinary = swapper.resolveSlotBinaryPath(serverRoot, currentSlot);
            Path backupBinary = snapshotDir.resolve(BACKUP_JAR_NAME);
            if (Files.isRegularFile(currentBinary)) {
                Files.copy(currentBinary, backupBinary, StandardCopyOption.REPLACE_EXISTING);
            } else {
                // Create a placeholder backup file to maintain reference
                Files.write(backupBinary, new byte[0]);
            }

            Properties props = new Properties();
            props.setProperty(KEY_STATE, SentinelMigrationState.UPGRADING_PENDING_HEALTHCHECK.name());
            props.setProperty(KEY_SNAPSHOT_ID, snapshotId);
            props.setProperty(KEY_PREVIOUS_SLOT, currentSlot.name());
            props.setProperty(KEY_PREVIOUS_VERSION, currentVersion);
            props.setProperty(KEY_TARGET_VERSION, targetVersion);
            props.setProperty(KEY_TIMESTAMP_MILLIS, String.valueOf(now));
            props.setProperty(KEY_BACKUP_BINARY_PATH, serverRoot.relativize(backupBinary).toString());

            storeProperties(serverRoot.resolve(SENTINEL_PROPERTIES_FILE), props, "VESSEL Rollback Sentinel Armed");

            Map<String, String> meta = new HashMap<>();
            meta.put(KEY_TARGET_VERSION, targetVersion);
            meta.put(KEY_TIMESTAMP_MILLIS, String.valueOf(now));

            return new RollbackSnapshot(snapshotId, now, currentSlot, currentVersion, backupBinary, meta);
        } catch (IOException e) {
            throw new SentinelStorageException("Failed to arm rollback sentinel snapshot at " + snapshotDir, e);
        }
    }

    /**
     * Confirms that the newly started server has successfully passed all initialization and health checks,
     * transitioning the sentinel state back to {@link SentinelMigrationState#STABLE}.
     *
     * @param serverRoot root directory of the server
     */
    public void confirmSuccessfulStartup(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path propFile = serverRoot.resolve(SENTINEL_PROPERTIES_FILE);
        if (!Files.isRegularFile(propFile)) {
            return;
        }

        Properties props = loadPropertiesSilently(propFile);
        String currentState = props.getProperty(KEY_STATE, SentinelMigrationState.STABLE.name());
        if (SentinelMigrationState.UPGRADING_PENDING_HEALTHCHECK.name().equalsIgnoreCase(currentState)) {
            props.setProperty(KEY_STATE, SentinelMigrationState.STABLE.name());
            props.setProperty(KEY_RESTORED_AT_MILLIS, String.valueOf(System.currentTimeMillis()));
            try {
                storeProperties(propFile, props, "VESSEL Upgrade Healthcheck Confirmed Stable");
            } catch (IOException e) {
                throw new SentinelStorageException("Failed to confirm stable startup state in sentinel properties", e);
            }
        }
    }

    /**
     * Inspects the sentinel status and executes an automated, atomic rollback if the server failed
     * during startup or threw {@code startupFailure}.
     *
     * @param serverRoot     root directory of the server
     * @param startupFailure caught exception during startup, or {@code null}
     * @return {@link RollbackExecutionResult} detailing the outcome
     */
    public RollbackExecutionResult detectAndExecuteAutomaticRollback(Path serverRoot, Throwable startupFailure) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");

        long startNanos = System.nanoTime();
        Path propFile = serverRoot.resolve(SENTINEL_PROPERTIES_FILE);
        if (!Files.isRegularFile(propFile)) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            return RollbackExecutionResult.failed("NONE", ServerBinarySlot.SLOT_A, "Sentinel properties file not found", elapsedMs);
        }

        Properties props = loadPropertiesSilently(propFile);
        String currentState = props.getProperty(KEY_STATE, SentinelMigrationState.STABLE.name());
        boolean needsRollback = SentinelMigrationState.UPGRADING_PENDING_HEALTHCHECK.name().equalsIgnoreCase(currentState)
                || startupFailure != null;

        if (!needsRollback) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            return RollbackExecutionResult.failed(
                    props.getProperty(KEY_SNAPSHOT_ID, "NONE"),
                    ServerBinarySlot.fromString(props.getProperty(KEY_PREVIOUS_SLOT, ServerBinarySlot.SLOT_A.name())),
                    "No rollback required; system state is " + currentState,
                    elapsedMs
            );
        }

        String snapshotId = props.getProperty(KEY_SNAPSHOT_ID, "snapshot-unknown");
        ServerBinarySlot previousSlot = ServerBinarySlot.fromString(
                props.getProperty(KEY_PREVIOUS_SLOT, ServerBinarySlot.SLOT_A.name())
        );
        String relativeBackup = props.getProperty(KEY_BACKUP_BINARY_PATH, "");
        Path backupBinary = relativeBackup.isBlank() ? null : serverRoot.resolve(relativeBackup);

        try {
            // Restore previous binary from backup if present
            if (backupBinary != null && Files.isRegularFile(backupBinary) && Files.size(backupBinary) > 0) {
                Path targetJar = swapper.resolveSlotBinaryPath(serverRoot, previousSlot);
                Files.copy(backupBinary, targetJar, StandardCopyOption.REPLACE_EXISTING);
            }

            // Restore active slot pointer back to previous slot
            swapper.commitActiveSlotPointer(serverRoot, previousSlot);

            // Record rollback status in sentinel properties
            props.setProperty(KEY_STATE, SentinelMigrationState.BOOTSTRAP_FAILED_ROLLED_BACK.name());
            String failureReason = startupFailure != null
                    ? startupFailure.getClass().getName() + ": " + startupFailure.getMessage()
                    : "Startup health check failure detected";
            props.setProperty(KEY_FAILURE_REASON, failureReason);
            props.setProperty(KEY_RESTORED_AT_MILLIS, String.valueOf(System.currentTimeMillis()));
            storeProperties(propFile, props, "VESSEL Automated Rollback Executed");

            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            return RollbackExecutionResult.successful(
                    snapshotId,
                    previousSlot,
                    "Successfully rolled back to slot [" + previousSlot + "] due to: " + failureReason,
                    elapsedMs
            );
        } catch (Exception e) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            return RollbackExecutionResult.failed(
                    snapshotId,
                    previousSlot,
                    "Rollback restoration failed: " + e.getMessage(),
                    elapsedMs
            );
        }
    }

    private Properties loadPropertiesSilently(Path propFile) {
        Properties properties = new Properties();
        if (Files.isRegularFile(propFile)) {
            try (InputStream is = Files.newInputStream(propFile)) {
                properties.load(is);
            } catch (IOException ignored) {
            }
        }
        return properties;
    }

    private void storeProperties(Path propFile, Properties properties, String comments) throws IOException {
        Path tempFile = propFile.resolveSibling(propFile.getFileName().toString() + ".tmp");
        try (OutputStream os = Files.newOutputStream(tempFile)) {
            properties.store(os, comments);
        }
        try {
            Files.move(tempFile, propFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(tempFile, propFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
