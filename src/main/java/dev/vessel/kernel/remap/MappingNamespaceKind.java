package dev.vessel.kernel.remap;

/**
 * Enumeration of bytecode symbol mapping namespaces supported by the VESSEL remapping pipeline.
 *
 * <p>Determines whether a module compiled against a given namespace requires ahead-of-time or
 * load-time translation into the canonical runtime namespace ({@link #MOJMAP} / {@link #CANONICAL_UIR})
 * under Decision 32 and Decision 40.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-27)
 */
public enum MappingNamespaceKind {

    MOJMAP(false),
    INTERMEDIARY(true),
    YARN(true),
    SEARGE_SRG(true),
    SPIGOT_OBF(true),
    CANONICAL_UIR(false);

    private final boolean runtimeTranslationRequired;

    MappingNamespaceKind(boolean runtimeTranslationRequired) {
        this.runtimeTranslationRequired = runtimeTranslationRequired;
    }

    /**
     * Checks whether bytecode in this namespace requires runtime translation for the given
     * Minecraft protocol version.
     *
     * @param minecraftProtocolVersion target Minecraft protocol version (must be positive)
     * @return {@code true} if symbol translation to Mojmap/Canonical UIR is required
     */
    public boolean requiresRuntimeTranslation(int minecraftProtocolVersion) {
        if (minecraftProtocolVersion <= 0) {
            throw new IllegalArgumentException("minecraftProtocolVersion must be positive: " + minecraftProtocolVersion);
        }
        return this.runtimeTranslationRequired;
    }
}
