package dev.vessel.bootstrap.wave3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Sample legacy-style MinecraftForge {@code @Mod} fixture with a zero-argument constructor that
 * resolves its active namespace and mod event bus through {@link ModLoadingContext#get()} and
 * {@link FMLJavaModLoadingContext#get()}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mod("legacy_forge_ctx_mod")
public final class SampleLegacyForgeContextModFixture {

    public static final List<String> RECORDED_EVENTS = new CopyOnWriteArrayList<>();

    public SampleLegacyForgeContextModFixture() {
        String activeNs = ModLoadingContext.get().getActiveNamespace();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        if (modBus != null) {
            RECORDED_EVENTS.add("CTOR_NS:" + activeNs);
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
        RECORDED_EVENTS.add("SETUP:" + event.getModId());
    }
}
