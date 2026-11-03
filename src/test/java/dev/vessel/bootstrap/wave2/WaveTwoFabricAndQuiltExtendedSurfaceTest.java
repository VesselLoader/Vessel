package dev.vessel.bootstrap.wave2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.adapter.fabric.FabricLoaderShim;
import dev.vessel.adapter.fabric.FabricModContainerShim;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.EntrypointContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.ObjectShare;
import org.junit.jupiter.api.Test;
import org.quiltmc.loader.api.QuiltLoader;

/**
 * Integration tests for Wave 2 verifying extended Fabric Loader and Quilt Loader runtime
 * surfaces: {@link ObjectShare}, {@link MappingResolver}, {@link EntrypointContainer}, and
 * {@link QuiltLoader}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
final class WaveTwoFabricAndQuiltExtendedSurfaceTest {

    private String observedAsyncShareKey;
    private Object observedAsyncShareValue;

    public void recordSharedObject(String key, Object value) {
        this.observedAsyncShareKey = key;
        this.observedAsyncShareValue = value;
    }

    @Test
    void testFabricAndQuiltObjectShareMappingResolverAndEntrypoints() {
        Path gameDir = Paths.get(".").toAbsolutePath().normalize();
        Path configDir = gameDir.resolve("config/fabric");
        FabricLoaderShim shim = new FabricLoaderShim(gameDir, configDir);
        FabricLoader loader = new FabricLoader(shim, false);
        FabricLoader.setInstance(loader);

        ObjectShare share = FabricLoader.getInstance().getObjectShare();
        assertNotNull(share);
        assertSame(share, QuiltLoader.getObjectShare());

        share.whenAvailable("sodium:renderer_ready", this::recordSharedObject);
        assertNull(observedAsyncShareKey);

        share.put("sodium:renderer_ready", Boolean.TRUE);
        assertEquals("sodium:renderer_ready", observedAsyncShareKey);
        assertEquals(Boolean.TRUE, observedAsyncShareValue);
        assertEquals(Boolean.TRUE, share.get("sodium:renderer_ready"));

        Object existing = share.putIfAbsent("sodium:renderer_ready", Boolean.FALSE);
        assertEquals(Boolean.TRUE, existing);
        assertEquals(Boolean.TRUE, share.get("sodium:renderer_ready"));

        MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
        assertNotNull(resolver);
        assertSame(resolver, QuiltLoader.getMappingResolver());
        assertEquals("named", resolver.getCurrentRuntimeNamespace());
        assertTrue(resolver.getNamespaces().contains("intermediary"));

        assertEquals(
                "net.minecraft.world.level.Level",
                resolver.mapClassName("intermediary", "net.minecraft.class_1937"));
        assertEquals(
                "net.minecraft.class_1937",
                resolver.unmapClassName("intermediary", "net.minecraft.world.level.Level"));
        assertEquals(
                "getBlockState",
                resolver.mapMethodName(
                        "intermediary",
                        "net.minecraft.class_1937",
                        "method_8520",
                        "(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;"));
        assertEquals(
                "isClientSide",
                resolver.mapFieldName(
                        "intermediary",
                        "net.minecraft.class_1937",
                        "field_9236",
                        "Z"));

        FabricModContainerShim modShim = new FabricModContainerShim(
                VesselModuleIdentifier.of(SourceLoaderKind.QUILT, "quilt_biome_mod", "1.2.0"),
                Map.of("name", "Quilt Biome Mod"),
                gameDir.resolve("mods/quilt_biome.jar"));
        ModContainer modContainer = loader.registerModContainer(modShim);

        loader.registerEntrypoint("main", "quilt_main_entrypoint_instance", modContainer);
        List<String> entrypoints = QuiltLoader.getEntrypoints("main", String.class);
        assertEquals(List.of("quilt_main_entrypoint_instance"), entrypoints);

        List<EntrypointContainer<String>> containers =
                QuiltLoader.getEntrypointContainers("main", String.class);
        assertEquals(1, containers.size());
        assertEquals("quilt_main_entrypoint_instance", containers.get(0).getEntrypoint());
        assertSame(modContainer, containers.get(0).getProvider());

        assertEquals(gameDir, QuiltLoader.getGameDir());
        assertEquals(configDir, QuiltLoader.getConfigDir());
    }
}
