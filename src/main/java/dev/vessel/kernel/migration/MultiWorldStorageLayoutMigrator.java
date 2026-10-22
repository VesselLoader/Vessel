package dev.vessel.kernel.migration;

import dev.vessel.kernel.identity.SourceLoaderKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Multi-world storage directory layout migration and resolution engine.
 * <p>
 * Enforces VESSEL Architecture Decision 6 across disparate world storage layouts:
 * <ul>
 *   <li><b>Vanilla, Forge, Fabric, NeoForge</b>: Unified single world folder
 *       ({@code world/DIM-1/}, {@code world/DIM1/}, {@code world/dimensions/}).</li>
 *   <li><b>Spigot, Paper, Purpur, Folia</b>: Independent split dimension root directories
 *       ({@code world/}, {@code world_nether/}, {@code world_the_end/}).</li>
 * </ul>
 * Provides bidirectional layout detection, non-destructive migration planning,
 * and deterministic dimension path resolution.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MultiWorldStorageLayoutMigrator {

    /**
     * Detects world storage directory layout using a file existence predicate.
     *
     * @param fileExistsPredicate predicate checking path existence relative to server root
     * @param worldName          base world name (typically {@code "world"})
     * @return detected directory scheme
     */
    public WorldStorageDirectoryLayout detectLayout(Predicate<String> fileExistsPredicate, String worldName) {
        Objects.requireNonNull(fileExistsPredicate, "fileExistsPredicate cannot be null");
        Objects.requireNonNull(worldName, "worldName cannot be null");

        String netherSplit = worldName + "_nether";
        String endSplit = worldName + "_the_end";

        if (fileExistsPredicate.test(netherSplit) || fileExistsPredicate.test(endSplit)) {
            return WorldStorageDirectoryLayout.SPIGOT_SPLIT;
        }

        String unifiedNether = worldName + "/DIM-1";
        String unifiedEnd = worldName + "/DIM1";
        String modernNether = worldName + "/dimensions/minecraft/the_nether";

        if (fileExistsPredicate.test(unifiedNether) || fileExistsPredicate.test(unifiedEnd) || fileExistsPredicate.test(modernNether)) {
            return WorldStorageDirectoryLayout.VANILLA_UNIFIED;
        }

        if (fileExistsPredicate.test(worldName + "/level.dat")) {
            return WorldStorageDirectoryLayout.VANILLA_UNIFIED;
        }

        return WorldStorageDirectoryLayout.UNKNOWN;
    }

    /**
     * Resolves the canonical directory for a given dimension under the specified layout.
     *
     * @param layout    directory layout scheme
     * @param worldName base world name
     * @param dimension target dimension type
     * @return relative directory path
     */
    public String resolveDimensionDirectory(
            WorldStorageDirectoryLayout layout,
            String worldName,
            WorldDimensionType dimension
    ) {
        Objects.requireNonNull(layout, "layout cannot be null");
        Objects.requireNonNull(worldName, "worldName cannot be null");
        Objects.requireNonNull(dimension, "dimension cannot be null");

        if (dimension == WorldDimensionType.OVERWORLD) {
            return worldName;
        }

        if (layout == WorldStorageDirectoryLayout.SPIGOT_SPLIT) {
            return switch (dimension) {
                case THE_NETHER -> worldName + "_nether";
                case THE_END -> worldName + "_the_end";
                case CUSTOM -> worldName + "/dimensions";
                default -> worldName;
            };
        } else {
            // VANILLA_UNIFIED or UNKNOWN fallback
            return switch (dimension) {
                case THE_NETHER -> worldName + "/DIM-1";
                case THE_END -> worldName + "/DIM1";
                case CUSTOM -> worldName + "/dimensions";
                default -> worldName;
            };
        }
    }

    /**
     * Resolves the exact directory holding region (*.mca) chunk files for a dimension.
     *
     * @param layout    directory layout scheme
     * @param worldName base world name
     * @param dimension target dimension type
     * @return relative path to region directory
     */
    public String resolveDimensionRegionDirectory(
            WorldStorageDirectoryLayout layout,
            String worldName,
            WorldDimensionType dimension
    ) {
        String dimDir = resolveDimensionDirectory(layout, worldName, dimension);
        if (layout == WorldStorageDirectoryLayout.SPIGOT_SPLIT) {
            return switch (dimension) {
                case THE_NETHER -> dimDir + "/DIM-1/region";
                case THE_END -> dimDir + "/DIM1/region";
                default -> dimDir + "/region";
            };
        } else {
            return dimDir + "/region";
        }
    }

    /**
     * Generates an ordered list of atomic migration operations between two layout schemes.
     *
     * @param fromLayout source layout
     * @param toLayout   target layout
     * @param worldName  base world name
     * @return ordered migration steps
     */
    public List<String> planMigration(
            WorldStorageDirectoryLayout fromLayout,
            WorldStorageDirectoryLayout toLayout,
            String worldName
    ) {
        Objects.requireNonNull(fromLayout, "fromLayout cannot be null");
        Objects.requireNonNull(toLayout, "toLayout cannot be null");
        Objects.requireNonNull(worldName, "worldName cannot be null");

        if (fromLayout == toLayout || fromLayout == WorldStorageDirectoryLayout.UNKNOWN || toLayout == WorldStorageDirectoryLayout.UNKNOWN) {
            return Collections.emptyList();
        }

        List<String> plan = new ArrayList<>();

        if (fromLayout == WorldStorageDirectoryLayout.VANILLA_UNIFIED && toLayout == WorldStorageDirectoryLayout.SPIGOT_SPLIT) {
            plan.add("MKDIR " + worldName + "_nether");
            plan.add("MOVE " + worldName + "/DIM-1 -> " + worldName + "_nether/DIM-1");
            plan.add("COPY " + worldName + "/uid.dat -> " + worldName + "_nether/uid.dat");
            plan.add("MKDIR " + worldName + "_the_end");
            plan.add("MOVE " + worldName + "/DIM1 -> " + worldName + "_the_end/DIM1");
            plan.add("COPY " + worldName + "/uid.dat -> " + worldName + "_the_end/uid.dat");
        } else if (fromLayout == WorldStorageDirectoryLayout.SPIGOT_SPLIT && toLayout == WorldStorageDirectoryLayout.VANILLA_UNIFIED) {
            plan.add("MOVE " + worldName + "_nether/DIM-1 -> " + worldName + "/DIM-1");
            plan.add("DELETE_IF_EMPTY " + worldName + "_nether");
            plan.add("MOVE " + worldName + "_the_end/DIM1 -> " + worldName + "/DIM1");
            plan.add("DELETE_IF_EMPTY " + worldName + "_the_end");
        }

        return plan;
    }

    /**
     * Checks if a loader conventionally requires the Spigot split layout.
     *
     * @param loader source loader
     * @return true if Spigot split directory convention is expected
     */
    public boolean isSplitLayoutPreferred(SourceLoaderKind loader) {
        if (loader == null) {
            return false;
        }
        return switch (loader) {
            case BUKKIT, SPIGOT, PAPER, FOLIA -> true;
            case FABRIC, QUILT, FORGE, NEOFORGE, VESSEL_NATIVE -> false;
        };
    }
}
