package dev.vessel.kernel.vstm.engine;

import java.util.Locale;
import java.util.Objects;

/**
 * Manages dialect bitmask flags for multi-ecosystem mapping sources in the VSTM core.
 *
 * <p>Supported Dialects:
 * <ul>
 *     <li>{@code MOJMAP} (Bit 0, 0x01): Mojang Official Obfuscation Mappings</li>
 *     <li>{@code INTERMEDIARY} (Bit 1, 0x02): Fabric Intermediary Mappings</li>
 *     <li>{@code YARN} (Bit 2, 0x04): Fabric Yarn Named Mappings</li>
 *     <li>{@code SPIGOT_NMS} (Bit 3, 0x08): CraftBukkit / Spigot Net.Minecraft.Server</li>
 *     <li>{@code SRG} (Bit 4, 0x10): Forge Searge / TSRG Mappings</li>
 *     <li>{@code NEOFORGE} (Bit 5, 0x20): NeoForge Parameter & Mapping Conventions</li>
 * </ul>
 * </p>
 *
 * <p>Prevents dialect mixups and misaligned transmutations (Catalog refs: UMG-0001 to UMG-0050).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmDialectMask {

    public static final int MOJMAP = 1 << 0;       // 1
    public static final int INTERMEDIARY = 1 << 1; // 2
    public static final int YARN = 1 << 2;         // 4
    public static final int SPIGOT_NMS = 1 << 3;   // 8
    public static final int SRG = 1 << 4;          // 16
    public static final int NEOFORGE = 1 << 5;     // 32

    /**
     * Checks if a specific dialect bit is set within a composite bitmask.
     *
     * @param bitmask the composite bitmask
     * @param dialect the dialect bit constant
     * @return true if dialect is active, false otherwise
     */
    public static boolean hasDialect(int bitmask, int dialect) {
        return (bitmask & dialect) != 0;
    }

    /**
     * Combines multiple dialect bits into a single composite bitmask.
     *
     * @param dialects variable list of dialect constants
     * @return the combined bitmask
     */
    public static int combine(int... dialects) {
        int mask = 0;
        if (dialects != null) {
            for (int dialect : dialects) {
                mask |= dialect;
            }
        }
        return mask;
    }

    /**
     * Resolves the canonical human-readable name of a single dialect.
     *
     * @param dialect the dialect constant
     * @return canonical dialect name string
     */
    public static String getName(int dialect) {
        return switch (dialect) {
            case MOJMAP -> "mojmap";
            case INTERMEDIARY -> "intermediary";
            case YARN -> "yarn";
            case SPIGOT_NMS -> "spigot_nms";
            case SRG -> "srg";
            case NEOFORGE -> "neoforge";
            default -> "unknown(" + dialect + ")";
        };
    }

    /**
     * Parses a dialect name string into its corresponding bitmask constant.
     *
     * @param name dialect name (case-insensitive)
     * @return the dialect bit constant
     * @throws IllegalArgumentException if the dialect name is unrecognized
     */
    public static int fromName(String name) {
        Objects.requireNonNull(name, "dialect name must not be null");
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mojmap", "mojang", "official" -> MOJMAP;
            case "intermediary", "fabric_intermediary" -> INTERMEDIARY;
            case "yarn", "fabric_yarn" -> YARN;
            case "spigot", "spigot_nms", "nms", "bukkit" -> SPIGOT_NMS;
            case "srg", "searge", "tsrg" -> SRG;
            case "neoforge", "neo" -> NEOFORGE;
            default -> throw new IllegalArgumentException("Unrecognized mapping dialect: " + name);
        };
    }

    private VstmDialectMask() {
        // Bitmask definitions
    }
}
