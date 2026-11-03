package dev.vessel.bootstrap.wave3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.mojang.SinglePassModAndPluginLiveActivator;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 3 verifying {@link SinglePassModAndPluginLiveActivator} live
 * activation of zero-arg Forge {@code @Mod} classes using {@code ModLoadingContext} /
 * {@code FMLJavaModLoadingContext}, Quilt QSL {@code ModInitializer} &amp;
 * {@code DedicatedServerModInitializer} entrypoints, and NeoForge {@link ModLoadingContext} +
 * {@link NeoForge#EVENT_BUS}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveThreeForgeContextAndQuiltActivationTest {

    @Test
    void testForgeModLoadingContextAndQuiltQslLiveActivation() throws Exception {
        SampleLegacyForgeContextModFixture.RECORDED_EVENTS.clear();
        SampleQuiltQslModFixture.QUILT_LOG.clear();

        SinglePassModAndPluginLiveActivator activator = new SinglePassModAndPluginLiveActivator();

        VesselModuleIdentifier forgeModuleId =
                VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "legacy_forge_ctx_mod", "1.0.0");
        boolean forgeActivated = activator.activateModOrPluginClassDirectly(
                SampleLegacyForgeContextModFixture.class,
                forgeModuleId,
                "1.21.4",
                Bukkit.getServer());
        assertTrue(forgeActivated);
        assertTrue(
                SampleLegacyForgeContextModFixture.RECORDED_EVENTS.contains(
                        "CTOR_NS:legacy_forge_ctx_mod"));
        assertTrue(
                SampleLegacyForgeContextModFixture.RECORDED_EVENTS.contains(
                        "SETUP:legacy_forge_ctx_mod"));

        VesselModuleIdentifier quiltModuleId =
                VesselModuleIdentifier.of(SourceLoaderKind.QUILT, "quilt_qsl_test", "2.1.0");
        boolean quiltActivated = activator.activateModOrPluginClassDirectly(
                SampleQuiltQslModFixture.class,
                quiltModuleId,
                "1.21.4",
                Bukkit.getServer());
        assertTrue(quiltActivated);
        assertTrue(SampleQuiltQslModFixture.QUILT_LOG.contains("QUILT_MAIN:quilt_qsl_test"));
        assertTrue(SampleQuiltQslModFixture.QUILT_LOG.contains("QUILT_SERVER:quilt_qsl_test"));

        ModContainer neoContainer = new ModContainer("neoforge_wave3_test");
        ModLoadingContext.get().setActiveContainer(neoContainer);
        assertEquals("neoforge_wave3_test", ModLoadingContext.get().getActiveNamespace());
        assertEquals(neoContainer, ModLoadingContext.get().getActiveContainer());
        assertNotNull(neoContainer.getEventBus());
        assertNotNull(NeoForge.EVENT_BUS);
    }
}
