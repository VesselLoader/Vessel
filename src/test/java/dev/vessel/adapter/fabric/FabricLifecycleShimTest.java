package dev.vessel.adapter.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.event.EventExecutionPhase;
import dev.vessel.kernel.identity.QuiltModJsonParser;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for Sub-Stage 6.5: {@link QuiltModJsonParser}, {@link FabricLoaderShim},
 * {@link FabricModContainerShim}, {@link FabricLifecycleAdapter}, and
 * {@link UniversalPacketChannelDispatcher}.
 *
 * @version 1.0.0
 */
class FabricLifecycleShimTest {

    @TempDir
    Path tempDir;

    @Test
    void testQuiltModJsonParserExtractsIdentifierDependenciesAndNestedJars() {
        String quiltJson = """
                {
                  "schema_version": 1,
                  "quilt_loader": {
                    "group": "dev.vessel.test",
                    "id": "quilt_biome_enhancer",
                    "version": "2.4.1",
                    "depends": [
                      { "id": "quilted_fabric_api", "versions": ">=7.0.0" },
                      { "id": "lithium", "versions": ">=0.12.0", "optional": true }
                    ],
                    "breaks": [
                      { "id": "optifabric", "versions": "*" }
                    ],
                    "jars": [
                      "META-INF/jars/sub-library-1.0.jar",
                      "META-INF/jars/math-core-2.1.jar"
                    ]
                  }
                }
                """;

        QuiltModJsonParser parser = new QuiltModJsonParser();
        VesselModuleIdentifier identifier = parser.parseQuiltModJson(quiltJson);
        assertEquals(SourceLoaderKind.QUILT, identifier.loader());
        assertEquals("quilt_biome_enhancer", identifier.namespace());
        assertEquals("2.4.1", identifier.version());

        List<ModuleDependencyRule> deps = parser.extractQuiltDependencies(quiltJson);
        assertEquals(3, deps.size());
        assertEquals("quilted_fabric_api", deps.get(0).targetNamespace());
        assertEquals(DependencyConstraintKind.MANDATORY, deps.get(0).kind());
        assertEquals("lithium", deps.get(1).targetNamespace());
        assertEquals(DependencyConstraintKind.OPTIONAL, deps.get(1).kind());
        assertEquals("optifabric", deps.get(2).targetNamespace());
        assertEquals(DependencyConstraintKind.CONFLICTS, deps.get(2).kind());

        List<String> nestedJars = parser.extractNestedJarPaths(quiltJson);
        assertEquals(
                List.of("META-INF/jars/sub-library-1.0.jar", "META-INF/jars/math-core-2.1.jar"),
                nestedJars);
    }

    @Test
    void testFabricLoaderShimAndContainerOpaqueCustomMetadata() {
        FabricLoaderShim loader = new FabricLoaderShim(tempDir, tempDir.resolve("config"));
        VesselModuleIdentifier modId =
                VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "sodium_server_compat", "0.5.8");
        FabricModContainerShim container = new FabricModContainerShim(
                modId,
                Map.of("loom:injected_interfaces", "net/minecraft/class_1937", "modmenu:api", "true"),
                tempDir.resolve("mods/sodium_server_compat.jar"));

        loader.registerModContainer(container);
        assertTrue(loader.isModLoaded("sodium_server_compat"));
        assertFalse(loader.isModLoaded("non_existent_mod"));
        assertTrue(loader.getModContainer("sodium_server_compat").isPresent());
        assertEquals(
                "net/minecraft/class_1937",
                loader.getModContainer("sodium_server_compat")
                        .orElseThrow()
                        .getOpaqueCustomMetadata()
                        .get("loom:injected_interfaces"));

        loader.putObjectShare("fabric-Sync:token", 42L);
        assertEquals(42L, loader.getObjectShare("fabric-Sync:token"));
    }

    @Test
    void testFabricLifecycleAdapterEntrypointOrderingAndKernelEventBridge() {
        FabricLifecycleAdapter lifecycle = new FabricLifecycleAdapter();
        List<String> executionLog = new ArrayList<>();

        lifecycle.registerEntrypoint("preLaunch", "mixinextras", () -> executionLog.add("preLaunch:mixinextras"));
        lifecycle.registerEntrypoint("main", "create_fabric", () -> executionLog.add("main:create_fabric"));
        lifecycle.registerEntrypoint("server", "create_fabric", () -> executionLog.add("server:create_fabric"));

        assertEquals(1, lifecycle.invokeEntrypointsForPhase("preLaunch"));
        assertEquals(1, lifecycle.invokeEntrypointsForPhase("main"));
        assertEquals(1, lifecycle.invokeEntrypointsForPhase("server"));
        assertEquals(
                List.of("preLaunch:mixinextras", "main:create_fabric", "server:create_fabric"),
                executionLog);

        List<String> phaseOrder = new ArrayList<>();
        lifecycle.bridgeFabricEventToKernel(
                "ServerTickEvents.END_SERVER_TICK",
                EventExecutionPhase.LATE,
                payload -> phaseOrder.add("LATE:" + payload));
        lifecycle.bridgeFabricEventToKernel(
                "ServerTickEvents.END_SERVER_TICK",
                EventExecutionPhase.EARLIEST,
                payload -> phaseOrder.add("EARLIEST:" + payload));

        int invoked = lifecycle.dispatchBridgedEvent("ServerTickEvents.END_SERVER_TICK", "tick-100");
        assertEquals(2, invoked);
        assertEquals(List.of("EARLIEST:tick-100", "LATE:tick-100"), phaseOrder);
    }

    @Test
    void testUniversalPacketChannelDispatcherCollisionGuardAndVanillaFilter() {
        UniversalPacketChannelDispatcher dispatcher = new UniversalPacketChannelDispatcher();
        AtomicInteger receivedBytes = new AtomicInteger(0);

        dispatcher.registerChannelHandler(
                "create:contraption_sync",
                SourceLoaderKind.FABRIC,
                bytes -> receivedBytes.addAndGet(bytes.length));
        dispatcher.registerChannelHandler(
                "bungeecord:main",
                SourceLoaderKind.PAPER,
                bytes -> receivedBytes.addAndGet(bytes.length));

        assertThrows(
                IllegalStateException.class,
                () -> dispatcher.registerChannelHandler(
                        "create:contraption_sync",
                        SourceLoaderKind.FORGE,
                        bytes -> {}));

        assertTrue(dispatcher.dispatchIncomingCustomPayload(
                "create:contraption_sync", "hello".getBytes(StandardCharsets.UTF_8)));
        assertEquals(5, receivedBytes.get());

        Set<String> moddedChannels = dispatcher.getAdvertisedChannelsForClient(false);
        assertEquals(Set.of("create:contraption_sync", "bungeecord:main"), moddedChannels);

        Set<String> vanillaChannels = dispatcher.getAdvertisedChannelsForClient(true);
        assertEquals(Set.of("bungeecord:main"), vanillaChannels);
    }
}
