package net.minecraftforge.fml.javafmlmod;

import java.util.Objects;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Forge Java FML mod loading context ({@code net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext})
 * exposing the active mod's {@link IEventBus} during {@code @Mod} constructor execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class FMLJavaModLoadingContext {

    private static final FMLJavaModLoadingContext INSTANCE = new FMLJavaModLoadingContext();

    private volatile IEventBus activeModEventBus = new IEventBus();

    private FMLJavaModLoadingContext() {
    }

    /**
     * Returns the active {@code FMLJavaModLoadingContext} singleton.
     *
     * @return non-null loading context
     */
    public static FMLJavaModLoadingContext get() {
        return INSTANCE;
    }

    /**
     * Returns the {@link IEventBus} associated with the mod currently being constructed.
     *
     * @return non-null mod event bus
     */
    public IEventBus getModEventBus() {
        return activeModEventBus;
    }

    /**
     * Updates the active mod {@link IEventBus} prior to invoking a Forge {@code @Mod} constructor.
     *
     * @param modEventBus non-null mod event bus
     */
    public void setActiveModEventBus(IEventBus modEventBus) {
        this.activeModEventBus = Objects.requireNonNull(modEventBus, "modEventBus must not be null");
    }
}
