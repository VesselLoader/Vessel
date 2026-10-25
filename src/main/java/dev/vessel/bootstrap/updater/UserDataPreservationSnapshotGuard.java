package dev.vessel.bootstrap.updater;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Enforces strict non-destructive user data isolation during VESSEL live updates and staged patch
 * applications.
 *
 * <p>Guarantees that user-owned directories ({@code mods/}, {@code plugins/}, {@code config/},
 * {@code world/}, {@code world_nether/}, {@code world_the_end/}) and configuration files
 * ({@code vessel.yml}, {@code server.properties}, {@code eula.txt}, {@code ops.json},
 * {@code whitelist.json}) are never overwritten, truncated, or deleted by auto-update tasks.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class UserDataPreservationSnapshotGuard {

    private static final Set<String> PROTECTED_TOP_LEVEL_DIRECTORIES = Set.of(
            "mods",
            "plugins",
            "config",
            "world",
            "world_nether",
            "world_the_end",
            "datapacks"
    );

    private static final Set<String> PROTECTED_ROOT_FILES = Set.of(
            "vessel.yml",
            "server.properties",
            "eula.txt",
            "ops.json",
            "whitelist.json",
            "banned-players.json",
            "banned-ips.json"
    );

    /**
     * Checks whether {@code candidatePath} resolves inside a protected user directory or matches a
     * protected user configuration file under {@code serverRoot}.
     *
     * @param serverRoot    the server root directory
     * @param candidatePath the target path being inspected
     * @return {@code true} if the path belongs to protected user data
     */
    public boolean isProtectedUserPath(Path serverRoot, Path candidatePath) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Objects.requireNonNull(candidatePath, "candidatePath must not be null");

        Path normalizedRoot = serverRoot.toAbsolutePath().normalize();
        Path normalizedCandidate = candidatePath.toAbsolutePath().normalize();
        if (!normalizedCandidate.startsWith(normalizedRoot)) {
            return false;
        }
        Path relative = normalizedRoot.relativize(normalizedCandidate);
        if (relative.getNameCount() == 0) {
            return true;
        }
        String firstSegment = relative.getName(0).toString().toLowerCase(Locale.ROOT);
        if (relative.getNameCount() == 1 && PROTECTED_ROOT_FILES.contains(firstSegment)) {
            return true;
        }
        return PROTECTED_TOP_LEVEL_DIRECTORIES.contains(firstSegment);
    }

    /**
     * Verifies that an auto-updater or patch writer is not targeting protected user data.
     *
     * @param serverRoot      the server root directory
     * @param targetWritePath the file path about to be modified by the updater
     * @throws SecurityException if {@code targetWritePath} is a protected user file or directory
     */
    public void enforceNonDestructivePatchTarget(Path serverRoot, Path targetWritePath) {
        if (isProtectedUserPath(serverRoot, targetWritePath)) {
            throw new SecurityException(
                    "VESSEL UserDataPreservationSnapshotGuard blocked updater write to protected user path: "
                            + targetWritePath);
        }
    }

    /**
     * Computes a combined SHA-256 integrity digest across all user files inside {@code mods/},
     * {@code plugins/}, {@code config/}, and root configuration files ({@code vessel.yml},
     * {@code server.properties}, {@code eula.txt}) to prove zero data loss across updates.
     *
     * @param serverRoot the server root directory
     * @return hex-encoded SHA-256 digest of all user data
     */
    public String computeUserDataIntegrityDigest(Path serverRoot) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path normalizedRoot = serverRoot.toAbsolutePath().normalize();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<Path> userFiles = collectProtectedUserFiles(normalizedRoot);
            for (Path file : userFiles) {
                String relPath = normalizedRoot.relativize(file).toString().replace('\\', '/');
                md.update(relPath.getBytes(StandardCharsets.UTF_8));
                md.update(Files.readAllBytes(file));
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute user data integrity digest", e);
        }
    }

    private List<Path> collectProtectedUserFiles(Path normalizedRoot) throws IOException {
        List<Path> collected = new ArrayList<>();
        if (!Files.isDirectory(normalizedRoot)) {
            return collected;
        }
        try (Stream<Path> walk = Files.walk(normalizedRoot, 4)) {
            List<Path> all = walk.filter(Files::isRegularFile).toList();
            for (Path file : all) {
                if (isProtectedUserPath(normalizedRoot, file)) {
                    collected.add(file);
                }
            }
        }
        Collections.sort(collected);
        return collected;
    }
}
