package dev.vessel.kernel.migration;

/**
 * Canonical Minecraft dimension type classifier for multi-world storage resolution.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum WorldDimensionType {
    OVERWORLD("minecraft:overworld", ""),
    THE_NETHER("minecraft:the_nether", "DIM-1"),
    THE_END("minecraft:the_end", "DIM1"),
    CUSTOM("custom", "dimensions");

    private final String namespacedId;
    private final String legacyFolderName;

    WorldDimensionType(String namespacedId, String legacyFolderName) {
        this.namespacedId = namespacedId;
        this.legacyFolderName = legacyFolderName;
    }

    public String getNamespacedId() {
        return namespacedId;
    }

    public String getLegacyFolderName() {
        return legacyFolderName;
    }
}
