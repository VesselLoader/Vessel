package dev.vessel.kernel.adapter.bukkit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests validating Bukkit plugin ClassLoader bridging, descriptor parsing, AsyncCatcher
 * thread validations, and CraftBukkit bytecode relocation.
 *
 * <p>Validates elimination of: ERR-62, ERR-70, ECO-081..100, MAP-045.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitPluginBridgeAndAsyncCatcherTest {

    private BukkitPluginClassLoaderBridge bridge;
    private PaperPluginYmlDescriptorParser parser;
    private AsyncCatcherThreadValidator asyncCatcher;
    private SpigotObcPackageRelocator relocator;

    @BeforeEach
    public void setUp() {
        bridge = new BukkitPluginClassLoaderBridge();
        parser = new PaperPluginYmlDescriptorParser();
        asyncCatcher = new AsyncCatcherThreadValidator(Thread.currentThread());
        relocator = new SpigotObcPackageRelocator();
    }

    @Test
    @DisplayName("BukkitPluginClassLoaderBridge: Resolves classes across registered plugin dependencies")
    public void testCrossPluginClassLookup() throws ClassNotFoundException {
        ClassLoader testLoader = getClass().getClassLoader();

        bridge.registerPluginClassLoader("plugin-a", testLoader, Set.of("plugin-b"));
        bridge.registerPluginClassLoader("plugin-b", testLoader, Set.of());

        Class<?> resolved = bridge.findCrossPluginClass("plugin-a", MockPluginBeta.class.getName());
        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo(MockPluginBeta.class.getName());
    }

    @Test
    @DisplayName("BukkitPluginClassLoaderBridge: Enforces kernel security isolation against internal classes")
    public void testKernelSecurityIsolation() {
        ClassLoader testLoader = getClass().getClassLoader();
        bridge.registerPluginClassLoader("rogue-plugin", testLoader, Set.of());

        assertThatThrownBy(() -> {
            bridge.findCrossPluginClass("rogue-plugin", "dev.vessel.kernel.vfs.DirectBufferBucket");
        }).isInstanceOf(SecurityException.class)
          .hasMessageContaining("not permitted to load internal microkernel class");
    }

    @Test
    @DisplayName("PaperPluginYmlDescriptorParser: Parses standard plugin.yml with inline arrays")
    public void testStandardPluginYamlParsing() {
        String yaml = """
                name: TestVaultPlugin
                version: 2.1.0
                main: dev.vessel.plugin.TestMain
                api-version: 1.20
                depend: [Vault, LuckPerms]
                softdepend: [ProtocolLib]
                author: VesselAuthor
                description: Demonstration of descriptor parser
                website: https://vesselmc.org
                """;

        PaperPluginDescriptor desc = parser.parse(yaml);

        assertThat(desc.name()).isEqualTo("TestVaultPlugin");
        assertThat(desc.version()).isEqualTo("2.1.0");
        assertThat(desc.mainClass()).isEqualTo("dev.vessel.plugin.TestMain");
        assertThat(desc.apiVersion()).isEqualTo("1.20");
        assertThat(desc.kind()).isEqualTo(PluginDescriptorKind.BUKKIT_PLUGIN_YML);
        assertThat(desc.requiredDependencies()).containsExactly("Vault", "LuckPerms");
        assertThat(desc.softDependencies()).containsExactly("ProtocolLib");
        assertThat(desc.authors()).containsExactly("VesselAuthor");
    }

    @Test
    @DisplayName("PaperPluginYmlDescriptorParser: Parses modern paper-plugin.yml with nested sections")
    public void testModernPaperPluginYamlParsing() {
        String yaml = """
                name: ModernPaperModule
                version: 3.0.0
                main: dev.vessel.paper.ModernMain
                bootstrapper: dev.vessel.paper.ModernBootstrapper
                loader: dev.vessel.paper.ModernLoader
                api-version: '1.20'
                dependencies:
                  server:
                    required:
                      - CoreEngine
                      - LibModule
                    load-after:
                      - EconomyBridge
                """;

        PaperPluginDescriptor desc = parser.parse(yaml);

        assertThat(desc.name()).isEqualTo("ModernPaperModule");
        assertThat(desc.version()).isEqualTo("3.0.0");
        assertThat(desc.mainClass()).isEqualTo("dev.vessel.paper.ModernMain");
        assertThat(desc.bootstrapperClass()).isEqualTo("dev.vessel.paper.ModernBootstrapper");
        assertThat(desc.loaderClass()).isEqualTo("dev.vessel.paper.ModernLoader");
        assertThat(desc.kind()).isEqualTo(PluginDescriptorKind.PAPER_PLUGIN_YML);
        assertThat(desc.requiredDependencies()).containsExactly("CoreEngine", "LibModule");
        assertThat(desc.softDependencies()).containsExactly("EconomyBridge");
    }

    @Test
    @DisplayName("AsyncCatcherThreadValidator: Permits access on primary server thread")
    public void testAsyncCatcherPermitsPrimaryThread() {
        // Current thread is primary, should not throw
        asyncCatcher.catchOp("block physics update");
        assertThat(asyncCatcher.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("AsyncCatcherThreadValidator: Traps asynchronous access from background worker thread")
    public void testAsyncCatcherTrapsAsyncThread() throws InterruptedException {
        AtomicBoolean trapped = new AtomicBoolean(false);
        AtomicReference<Throwable> caughtException = new AtomicReference<>();

        Thread worker = new Thread(() -> {
            try {
                asyncCatcher.catchOp("entity chunk add");
            } catch (IllegalStateException e) {
                trapped.set(true);
                caughtException.set(e);
            }
        }, "AsyncWorkerPool-1");

        worker.start();
        worker.join();

        assertThat(trapped.get()).isTrue();
        assertThat(caughtException.get()).isNotNull();
        assertThat(caughtException.get().getMessage()).contains("Asynchronous entity chunk add!");
    }

    @Test
    @DisplayName("AsyncCatcherThreadValidator: Scoped bypass permits asynchronous execution in try-with-resources")
    public void testAsyncCatcherScopedBypass() throws InterruptedException {
        AtomicBoolean succeeded = new AtomicBoolean(false);

        Thread worker = new Thread(() -> {
            try (AsyncCatcherBypassToken token = asyncCatcher.acquireBypass()) {
                assertThat(token.isClosed()).isFalse();
                asyncCatcher.catchOp("inventory transaction dispatch");
                succeeded.set(true);
            }
        }, "AsyncWorkerPool-2");

        worker.start();
        worker.join();

        assertThat(succeeded.get()).isTrue();
    }

    @Test
    @DisplayName("AsyncCatcherThreadValidator: Validates registered Folia regional ticking threads")
    public void testAsyncCatcherRegionThreads() throws InterruptedException {
        AtomicBoolean regionExecuted = new AtomicBoolean(false);

        Thread regionThread = new Thread(() -> {
            asyncCatcher.catchOp("chunk regional tick");
            regionExecuted.set(true);
        }, "FoliaRegion-Thread-1");

        asyncCatcher.registerRegionThread(regionThread);

        regionThread.start();
        regionThread.join();

        assertThat(regionExecuted.get()).isTrue();
    }

    @Test
    @DisplayName("SpigotObcPackageRelocator: Strips versioned token from CraftBukkit internal and dot class names")
    public void testSpigotObcRelocation() {
        String relocatedInternal = relocator.relocateInternalName("org/bukkit/craftbukkit/v1_20_R3/CraftServer");
        assertThat(relocatedInternal).isEqualTo("org/bukkit/craftbukkit/CraftServer");

        String relocatedDot = relocator.relocateClassName("org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer");
        assertThat(relocatedDot).isEqualTo("org.bukkit.craftbukkit.entity.CraftPlayer");
    }

    @Test
    @DisplayName("SpigotObcPackageRelocator: Relocates complex bytecode method descriptors")
    public void testDescriptorRelocation() {
        String desc = "(Lorg/bukkit/craftbukkit/v1_20_R3/entity/CraftPlayer;)Lorg/bukkit/craftbukkit/v1_20_R3/CraftServer;";
        String relocatedDesc = relocator.relocateDescriptor(desc);

        assertThat(relocatedDesc).isEqualTo(
                "(Lorg/bukkit/craftbukkit/entity/CraftPlayer;)Lorg/bukkit/craftbukkit/CraftServer;");
    }

    @Test
    @DisplayName("SpigotObcPackageRelocator: Maps legacy NMS classes to Mojang Official Mappings")
    public void testLegacyNmsRelocation() {
        String nmsInternal = relocator.relocateInternalName("net/minecraft/server/v1_20_R3/EntityPlayer");
        assertThat(nmsInternal).isEqualTo("net/minecraft/server/level/ServerPlayer");

        String nmsDot = relocator.relocateClassName("net.minecraft.server.v1_20_R3.WorldServer");
        assertThat(nmsDot).isEqualTo("net.minecraft.server.level.ServerLevel");
    }
}
