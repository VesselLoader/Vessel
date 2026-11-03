package net.minecraftforge.fml;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge mod loading context ({@code net.minecraftforge.fml.ModLoadingContext}) exposing the
 * currently initializing mod namespace and language-specific extension context.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ModLoadingContext {

    private static final ModLoadingContext INSTANCE = new ModLoadingContext();

    private volatile String activeNamespace = "minecraft";

    private ModLoadingContext() {
    }

    /**
     * Returns the active {@code ModLoadingContext} singleton.
     *
     * @return non-null mod loading context
     */
    public static ModLoadingContext get() {
        return INSTANCE;
    }

    /**
     * Returns the namespace (modId) of the mod currently being constructed.
     *
     * @return non-null active mod namespace
     */
    public String getActiveNamespace() {
        return activeNamespace;
    }

    /**
     * Sets the active mod namespace prior to constructing a Forge mod entrypoint.
     *
     * @param namespace mod identifier; defaults to {@code "minecraft"} if {@code null} or blank
     */
    public void setActiveNamespace(String namespace) {
        this.activeNamespace = (namespace == null || namespace.isBlank())
                ? "minecraft"
                : namespace.trim();
    }

    /**
     * Returns the active {@link FMLJavaModLoadingContext} language extension.
     *
     * @return non-null Java FML mod loading context
     */
    public FMLJavaModLoadingContext extension() {
        return FMLJavaModLoadingContext.get();
    }
}
