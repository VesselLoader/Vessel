package dev.vessel.kernel.asset.stream;

/**
 * Enumeration of Minecraft Resource Pack formats supported by the dual-generation engine.
 * Extinguishes AST-MOD.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum TargetPackFormat {

    /**
     * Minecraft 1.20.1 (Format 15) using CustomModelData overrides.
     */
    FORMAT_15_LEGACY(15, "1.20.1 CustomModelData Overrides"),

    /**
     * Minecraft 1.20.4 (Format 22) using CustomModelData overrides.
     */
    FORMAT_22_LEGACY(22, "1.20.4 CustomModelData Overrides"),

    /**
     * Minecraft 1.20.5 (Format 32) using modern minecraft:item_model definitions.
     */
    FORMAT_32_MODERN(32, "1.20.5 Modern Item Models"),

    /**
     * Minecraft 1.21+ (Format 34) modern item components.
     */
    FORMAT_34_MODERN(34, "1.21+ Modern Item Models"),

    /**
     * Universal dual generation targeting both legacy and modern formats simultaneously.
     */
    UNIVERSAL_DUAL(34, "Universal Dual-Generation Pack (1.20.1 - 1.21+)");

    private final int formatNumber;
    private final String description;

    TargetPackFormat(int formatNumber, String description) {
        this.formatNumber = formatNumber;
        this.description = description;
    }

    public int getFormatNumber() {
        return formatNumber;
    }

    public String getDescription() {
        return description;
    }
}
