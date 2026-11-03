package net.neoforged.fml;

import java.util.Objects;

/**
 * NeoForge FML mod loading context ({@code net.neoforged.fml.ModLoadingContext}) exposing the
 * currently initializing {@link ModContainer} during {@code @Mod} constructor execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ModLoadingContext {

    private static final ModLoadingContext INSTANCE = new ModLoadingContext();

    private volatile ModContainer activeContainer = new ModContainer("minecraft");

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
     * Returns the {@link ModContainer} of the mod currently being constructed.
     *
     * @return non-null active mod container
     */
    public ModContainer getActiveContainer() {
        return activeContainer;
    }

    /**
     * Updates the active {@link ModContainer} prior to constructing a NeoForge {@code @Mod} entrypoint.
     *
     * @param container non-null mod container
     */
    public void setActiveContainer(ModContainer container) {
        this.activeContainer = Objects.requireNonNull(container, "container must not be null");
    }

    /**
     * Returns the namespace (modId) of the active {@link ModContainer}.
     *
     * @return non-null active mod namespace
     */
    public String getActiveNamespace() {
        return activeContainer.getModId();
    }
}
