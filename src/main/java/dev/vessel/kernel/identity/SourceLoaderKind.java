package dev.vessel.kernel.identity;

import java.util.Objects;

/**
 * Enumeration representing the supported upstream ecosystem loader kinds.
 *
 * <p>Each loader ecosystem carries its own distinct lifecycle, metadata descriptor,
 * and capability semantics. VESSEL distinguishes all modules by their originating
 * loader kind to guarantee full isolation and prevent cross-loader name collisions.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-3)
 */
public enum SourceLoaderKind {

    /** Classical CraftBukkit plugin ecosystem. */
    BUKKIT("bukkit", "plugin.yml"),

    /** Spigot extended plugin ecosystem. */
    SPIGOT("spigot", "plugin.yml"),

    /** Modern Paper server plugin ecosystem (supports paper-plugin.yml). */
    PAPER("paper", "paper-plugin.yml"),

    /** Folia regionized multithreading server ecosystem. */
    FOLIA("folia", "paper-plugin.yml"),

    /** Fabric modular mod ecosystem. */
    FABRIC("fabric", "fabric.mod.json"),

    /** Quilt modular mod ecosystem (supports quilt.mod.json). */
    QUILT("quilt", "quilt.mod.json"),

    /** MinecraftForge mod ecosystem (supports mods.toml &amp; legacy mcmod.info). */
    FORGE("forge", "mods.toml"),

    /** NeoForge next-generation mod ecosystem (supports neoforge.mods.toml). */
    NEOFORGE("neoforge", "neoforge.mods.toml"),

    /** Sovereign native VESSEL microkernel module. */
    VESSEL_NATIVE("vessel", "vessel.module.json");

    private final String id;
    private final String defaultDescriptorFile;

    SourceLoaderKind(String id, String defaultDescriptorFile) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.defaultDescriptorFile = Objects.requireNonNull(defaultDescriptorFile, "defaultDescriptorFile must not be null");
    }

    /**
     * Returns the standardized identifier string for this loader kind.
     *
     * @return the loader identifier string
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the canonical metadata descriptor filename for this loader kind.
     *
     * @return the descriptor filename
     */
    public String getDefaultDescriptorFile() {
        return defaultDescriptorFile;
    }

    /**
     * Checks if this loader kind belongs to the Bukkit server family (Bukkit, Spigot, Paper, Folia).
     *
     * @return true if part of the Bukkit family
     */
    public boolean isBukkitFamily() {
        return this == BUKKIT || this == SPIGOT || this == PAPER || this == FOLIA;
    }

    /**
     * Checks if this loader kind belongs to the Fabric/Quilt family.
     *
     * @return true if part of the Fabric/Quilt family
     */
    public boolean isFabricFamily() {
        return this == FABRIC || this == QUILT;
    }

    /**
     * Checks if this loader kind belongs to the Forge/NeoForge family.
     *
     * @return true if part of the Forge/NeoForge family
     */
    public boolean isForgeFamily() {
        return this == FORGE || this == NEOFORGE;
    }

    /**
     * Resolves a loader kind by its canonical identifier string (case-insensitive).
     *
     * @param id the identifier string to parse
     * @return the matched SourceLoaderKind
     * @throws IllegalArgumentException if the identifier is unknown
     */
    public static SourceLoaderKind fromId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String normalized = id.trim().toLowerCase();
        for (SourceLoaderKind kind : values()) {
            if (kind.id.equals(normalized)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown SourceLoaderKind identifier: " + id);
    }
}
