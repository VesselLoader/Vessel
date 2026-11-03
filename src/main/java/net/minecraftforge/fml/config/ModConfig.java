package net.minecraftforge.fml.config;

import java.util.Locale;
import java.util.Objects;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Forge mod configuration container ({@code net.minecraftforge.fml.config.ModConfig}) binding a
 * {@link ForgeConfigSpec} to a mod identifier, {@link ModConfigType}, and backing TOML file name.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ModConfig {

    private final ModConfigType type;
    private final ForgeConfigSpec spec;
    private final String modId;
    private final String fileName;

    /**
     * Constructs a new {@code ModConfig} using the default {@code "<modId>-<type>.toml"} file name.
     *
     * @param type  non-null configuration scope type
     * @param spec  non-null configuration specification
     * @param modId non-null owning mod identifier
     */
    public ModConfig(ModConfigType type, ForgeConfigSpec spec, String modId) {
        this(
                type,
                spec,
                modId,
                Objects.requireNonNull(modId, "modId must not be null").trim()
                        + "-"
                        + Objects.requireNonNull(type, "type must not be null").name().toLowerCase(Locale.ROOT)
                        + ".toml");
    }

    /**
     * Constructs a new {@code ModConfig} with an explicit backing TOML file name.
     *
     * @param type     non-null configuration scope type
     * @param spec     non-null configuration specification
     * @param modId    non-null owning mod identifier
     * @param fileName non-null configuration file name
     */
    public ModConfig(ModConfigType type, ForgeConfigSpec spec, String modId, String fileName) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.spec = Objects.requireNonNull(spec, "spec must not be null");
        this.modId = Objects.requireNonNull(modId, "modId must not be null").trim();
        this.fileName = Objects.requireNonNull(fileName, "fileName must not be null").trim();
    }

    /**
     * Returns the configuration scope type ({@link ModConfigType#COMMON}, {@link ModConfigType#CLIENT}, etc.).
     *
     * @return non-null configuration type
     */
    public ModConfigType getType() {
        return type;
    }

    /**
     * Returns the underlying {@link ForgeConfigSpec}.
     *
     * @return non-null configuration specification
     */
    public ForgeConfigSpec getSpec() {
        return spec;
    }

    /**
     * Returns the owning mod identifier.
     *
     * @return non-null mod identifier
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns the backing TOML configuration file name.
     *
     * @return non-null file name
     */
    public String getFileName() {
        return fileName;
    }
}
