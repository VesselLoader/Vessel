package dev.vessel.kernel.nativebridge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite for {@link GlobalNativeLibraryRegistry}, {@link NativeLibraryUnsatisfiedLinkGuard},
 * and {@link JniSymbolTableResolver}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class NativeBridgeGuardTest {

    private GlobalNativeLibraryRegistry registry;
    private NativeLibraryUnsatisfiedLinkGuard guard;
    private JniSymbolTableResolver resolver;

    @BeforeEach
    void setUp() {
        registry = new GlobalNativeLibraryRegistry();
        guard = new NativeLibraryUnsatisfiedLinkGuard(registry);
        resolver = new JniSymbolTableResolver();
    }

    @Test
    @DisplayName("Normalizes native library file names across operating system formats")
    void testLibraryNameNormalization() {
        assertThat(GlobalNativeLibraryRegistry.normalizeLibraryName("libopus.so")).isEqualTo("opus");
        assertThat(GlobalNativeLibraryRegistry.normalizeLibraryName("opus.dll")).isEqualTo("opus");
        assertThat(GlobalNativeLibraryRegistry.normalizeLibraryName("libopus.dylib")).isEqualTo("opus");
        assertThat(GlobalNativeLibraryRegistry.normalizeLibraryName("/usr/lib/libsqlitejdbc.jnilib")).isEqualTo("sqlitejdbc");
        assertThat(GlobalNativeLibraryRegistry.normalizeLibraryName("C:\\lib\\netty_transport.dll")).isEqualTo("netty_transport");
    }

    @Test
    @DisplayName("Registers and retrieves native library metadata in GlobalNativeLibraryRegistry")
    void testRegistryLifecycle() {
        NativeLibraryEntry entry = registry.register("libopus.so", null, "voicechat");
        assertThat(entry.normalizedName()).isEqualTo("opus");
        assertThat(entry.state()).isEqualTo(NativeLibraryState.REGISTERED);

        Optional<NativeLibraryEntry> found = registry.find("opus");
        assertThat(found).isPresent();
        assertThat(found.get().libraryName()).isEqualTo("libopus.so");

        boolean marked = registry.markLoaded("opus");
        assertThat(marked).isTrue();
        assertThat(registry.isLoaded("opus")).isTrue();
    }

    @Test
    @DisplayName("Guards against redundant loads and suppresses duplicate UnsatisfiedLinkError")
    void testNativeLibraryUnsatisfiedLinkGuard() {
        AtomicInteger invocationCount = new AtomicInteger(0);

        // First load succeeds
        NativeLoadResult firstResult = guard.guardLoad("opus", "mod-a", invocationCount::incrementAndGet);
        assertThat(firstResult.success()).isTrue();
        assertThat(firstResult.wasAlreadyLoaded()).isFalse();
        assertThat(invocationCount.get()).isEqualTo(1);

        // Second load of same library is intercepted; load action NOT called again
        NativeLoadResult secondResult = guard.guardLoad("libopus.so", "mod-b", invocationCount::incrementAndGet);
        assertThat(secondResult.success()).isTrue();
        assertThat(secondResult.wasAlreadyLoaded()).isTrue();
        assertThat(invocationCount.get()).isEqualTo(1); // Not incremented!
    }

    @Test
    @DisplayName("Suppresses JVM already-loaded UnsatisfiedLinkError and treats as successful load")
    void testSuppressionOfDuplicateLinkError() {
        NativeLoadResult result = guard.guardLoad("sqlitejdbc", "mod-db", () -> {
            throw new UnsatisfiedLinkError("Native Library /tmp/sqlite.so already loaded in another classloader");
        });

        assertThat(result.success()).isTrue();
        assertThat(result.wasAlreadyLoaded()).isTrue();
        assertThat(registry.isLoaded("sqlitejdbc")).isTrue();
    }

    @Test
    @DisplayName("Resolves JNI symbol addresses and detects symbol collisions")
    void testJniSymbolTableResolver() {
        String symbol = "Java_org_example_Native_call";
        long address1 = 0x7FFF12345678L;

        resolver.registerSymbol(symbol, address1, "libexample.so", "1.0.0");
        assertThat(resolver.hasSymbol(symbol)).isTrue();

        OptionalLong resolved = resolver.resolveAddress(symbol);
        assertThat(resolved).hasValue(address1);

        // Collision detection
        long address2 = 0x7FFF87654321L;
        assertThat(resolver.detectCollision(symbol, address2)).isTrue();
        assertThat(resolver.detectCollision(symbol, address1)).isFalse();
    }
}
