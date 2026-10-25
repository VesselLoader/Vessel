package dev.vessel.kernel.migration;

import dev.vessel.kernel.identity.SourceLoaderKind;

import java.util.Objects;

/**
 * Migration engine enforcing VESSEL Architecture Decision 6:
 * Splits legacy shared {@code data/mods/<id>/} and {@code plugins/<id>/} storage
 * into per-source-loader partitioned directories:
 * <ul>
 *   <li>{@code data/fabric/<id>/}</li>
 *   <li>{@code data/quilt/<id>/}</li>
 *   <li>{@code data/forge/<id>/}</li>
 *   <li>{@code data/neoforge/<id>/}</li>
 *   <li>{@code data/plugins/<id>/}</li>
 *   <li>{@code data/vessel/<id>/}</li>
 * </ul>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class CrossPlatformDataMigrator {

    /**
     * Resolves the target partitioned VESSEL data directory for a given module.
     *
     * @param loader the source loader origin
     * @param moduleId normalized module identifier
     * @return the canonical target path relative to server root
     */
    public String resolveTargetDataDirectory(SourceLoaderKind loader, String moduleId) {
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(moduleId, "moduleId cannot be null");

        String normalizedId = moduleId.trim().toLowerCase();
        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException("moduleId cannot be empty");
        }

        return switch (loader) {
            case FABRIC -> "data/fabric/" + normalizedId + "/";
            case QUILT -> "data/quilt/" + normalizedId + "/";
            case FORGE -> "data/forge/" + normalizedId + "/";
            case NEOFORGE -> "data/neoforge/" + normalizedId + "/";
            case BUKKIT, SPIGOT, PAPER, FOLIA -> "data/plugins/" + normalizedId + "/";
            case VESSEL_NATIVE -> "data/vessel/" + normalizedId + "/";
        };
    }

    /**
     * Determines whether a given legacy path requires migration into VESSEL's partitioned layout.
     *
     * @param legacyPath the path to check
     * @return true if path is in legacy shared mods or root plugins location
     */
    public boolean requiresMigration(String legacyPath) {
        if (legacyPath == null) {
            return false;
        }
        String p = legacyPath.replace('\\', '/').trim();
        return p.startsWith("data/mods/")
                || (p.startsWith("plugins/") && !p.startsWith("data/plugins/"))
                || (p.startsWith("config/") && !p.startsWith("vessel-config/"));
    }
}
