package dev.vessel.bootstrap.target;

import dev.vessel.kernel.identity.SourceLoaderKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;

/**
 * Routes module and world data directories into per-loader isolated paths under
 * Architectural Decisions 6 and 26.
 *
 * <ul>
 *   <li><b>Decision 6:</b> Isolates module state into {@code data/<loader>/<id>/},
 *       {@code data/worlds/<world>/<loader>/<id>/}, and scoped categories ({@code cache},
 *       {@code generated}, {@code logs}, {@code temp}, {@code libraries}, {@code backups},
 *       {@code reports/crashes}).</li>
 *   <li><b>Decision 26:</b> Implements copy-on-first-creation from
 *       {@code vessel-config/defaults/<loader>/} into the active module config directory.</li>
 * </ul>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-29)
 */
public final class PerLoaderDataDirectoryRouter {

    /**
     * Resolves the isolated data directory for a module: {@code data/<loader>/<id>/}.
     *
     * @param serverRoot server root path
     * @param loader     source loader kind
     * @param moduleId   normalized module identifier
     * @return resolved path {@code <serverRoot>/data/<loader>/<id>}
     */
    public Path resolveModuleDataDirectory(Path serverRoot, SourceLoaderKind loader, String moduleId) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(moduleId, "moduleId cannot be null");

        return serverRoot
                .resolve("data")
                .resolve(loader.getId().toLowerCase(Locale.ROOT))
                .resolve(sanitizeSegment(moduleId));
    }

    /**
     * Resolves the world-scoped isolated data directory for a module:
     * {@code data/worlds/<world>/<loader>/<id>/}.
     *
     * @param serverRoot server root path
     * @param worldId    world identifier
     * @param loader     source loader kind
     * @param moduleId   normalized module identifier
     * @return resolved path {@code <serverRoot>/data/worlds/<world>/<loader>/<id>}
     */
    public Path resolveWorldModuleDataDirectory(
            Path serverRoot,
            String worldId,
            SourceLoaderKind loader,
            String moduleId
    ) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        Objects.requireNonNull(worldId, "worldId cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(moduleId, "moduleId cannot be null");

        return serverRoot
                .resolve("data")
                .resolve("worlds")
                .resolve(sanitizeSegment(worldId))
                .resolve(loader.getId().toLowerCase(Locale.ROOT))
                .resolve(sanitizeSegment(moduleId));
    }

    /**
     * Resolves a scoped auxiliary directory ({@code cache}, {@code generated}, {@code logs},
     * {@code temp}, {@code libraries}, {@code backups}, or {@code reports/crashes}) for the
     * specified loader and module.
     *
     * @param serverRoot   server root path
     * @param rootCategory top-level category (e.g. {@code "cache"}, {@code "reports/crashes"})
     * @param loader       source loader kind
     * @param moduleId     normalized module identifier
     * @return resolved path {@code <serverRoot>/<rootCategory>/<loader>/<id>}
     */
    public Path resolveScopedDirectory(
            Path serverRoot,
            String rootCategory,
            SourceLoaderKind loader,
            String moduleId
    ) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        Objects.requireNonNull(rootCategory, "rootCategory cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(moduleId, "moduleId cannot be null");

        Path base = serverRoot;
        for (String part : rootCategory.replace('\\', '/').split("/")) {
            if (!part.isBlank()) {
                base = base.resolve(part.trim());
            }
        }
        return base
                .resolve(loader.getId().toLowerCase(Locale.ROOT))
                .resolve(sanitizeSegment(moduleId));
    }

    /**
     * Copies a default configuration template from {@code vessel-config/defaults/<loader>/<configFileName>}
     * into {@code data/<loader>/config/<configFileName>} only if the target file does not yet exist
     * (Decision 26 copy-on-first-creation).
     *
     * @param serverRoot     server root path
     * @param loader         source loader kind
     * @param configFileName configuration file name (e.g. {@code "mekanism-common.toml"})
     * @return {@code true} if the default template was copied; {@code false} if target already existed or template was absent
     */
    public boolean copyDefaultConfigOnFirstCreation(
            Path serverRoot,
            SourceLoaderKind loader,
            String configFileName
    ) {
        Objects.requireNonNull(serverRoot, "serverRoot cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(configFileName, "configFileName cannot be null");

        String loaderKey = loader.getId().toLowerCase(Locale.ROOT);
        Path defaultSource = serverRoot
                .resolve("vessel-config")
                .resolve("defaults")
                .resolve(loaderKey)
                .resolve(configFileName);

        Path targetDest = serverRoot
                .resolve("data")
                .resolve(loaderKey)
                .resolve("config")
                .resolve(configFileName);

        if (Files.exists(targetDest) || !Files.exists(defaultSource)) {
            return false;
        }

        try {
            Files.createDirectories(targetDest.getParent());
            Files.copy(defaultSource, targetDest, StandardCopyOption.COPY_ATTRIBUTES);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy default config from " + defaultSource + " to " + targetDest, e);
        }
    }

    private static String sanitizeSegment(String segment) {
        String cleaned = segment.trim().replace("..", "").replace('\\', '_').replace('/', '_');
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Path segment cannot be empty");
        }
        return cleaned;
    }
}
