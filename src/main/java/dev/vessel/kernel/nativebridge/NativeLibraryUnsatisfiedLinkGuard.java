package dev.vessel.kernel.nativebridge;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Protection layer intercepting and suppressing duplicate native shared library loading calls.
 * Eliminates fatal JVM {@link UnsatisfiedLinkError} ("already loaded in another classloader")
 * when multiple independent ecosystem modules bundle overlapping native binaries
 * such as Opus (Simple Voice Chat), Netty native transport, or SQLite JDBC (ERR-29).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class NativeLibraryUnsatisfiedLinkGuard {

    private static final String JVM_ALREADY_LOADED_SUBSTRING = "already loaded in another classloader";
    private static final String JVM_ALREADY_LOADED_ALT_SUBSTRING = "already loaded";

    private static final Map<String, String> KNOWN_ALIASES = Map.of(
            "voicechat_opus", "opus",
            "libopus", "opus",
            "netty_transport_native_epoll_x86_64", "netty_transport_native_epoll",
            "netty_transport_native_epoll_aarch_64", "netty_transport_native_epoll",
            "netty_transport_native_kqueue_x86_64", "netty_transport_native_kqueue",
            "netty_transport_native_kqueue_aarch_64", "netty_transport_native_kqueue"
    );

    private final GlobalNativeLibraryRegistry registry;
    private final ConcurrentMap<String, Object> libraryLocks = new ConcurrentHashMap<>();

    /**
     * Constructs a NativeLibraryUnsatisfiedLinkGuard bound to the specified registry.
     *
     * @param registry the global native library registry
     */
    public NativeLibraryUnsatisfiedLinkGuard(GlobalNativeLibraryRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry cannot be null");
    }

    /**
     * Resolves the canonical alias for a library name if recognized.
     *
     * @param rawName raw library name
     * @return canonical unified name
     */
    public String resolveAlias(String rawName) {
        String normalized = GlobalNativeLibraryRegistry.normalizeLibraryName(rawName);
        return KNOWN_ALIASES.getOrDefault(normalized, normalized);
    }

    /**
     * Guards execution of a native library loading action, preventing redundant invocation
     * and catching duplicate ClassLoader link errors.
     *
     * @param libraryName name of the native library
     * @param ownerModuleId module requesting the load
     * @param loadAction execution action performing {@code System.load} or {@code System.loadLibrary}
     * @return outcome descriptor of the load operation
     */
    public NativeLoadResult guardLoad(String libraryName, String ownerModuleId, Runnable loadAction) {
        Objects.requireNonNull(libraryName, "libraryName cannot be null");
        Objects.requireNonNull(ownerModuleId, "ownerModuleId cannot be null");
        Objects.requireNonNull(loadAction, "loadAction cannot be null");

        String canonicalAlias = resolveAlias(libraryName);

        // Fast-path: Check if already loaded in registry
        if (registry.isLoaded(canonicalAlias) || registry.isLoaded(libraryName)) {
            return NativeLoadResult.success(canonicalAlias, true);
        }

        Object lock = libraryLocks.computeIfAbsent(canonicalAlias, k -> new Object());
        synchronized (lock) {
            // Double-check inside synchronization barrier
            if (registry.isLoaded(canonicalAlias) || registry.isLoaded(libraryName)) {
                return NativeLoadResult.success(canonicalAlias, true);
            }

            registry.register(canonicalAlias, null, ownerModuleId);
            registry.markState(canonicalAlias, NativeLibraryState.LOADING);

            try {
                loadAction.run();
                registry.markState(canonicalAlias, NativeLibraryState.LOADED);
                return NativeLoadResult.success(canonicalAlias, false);
            } catch (UnsatisfiedLinkError linkError) {
                String message = linkError.getMessage();
                if (message != null && (message.contains(JVM_ALREADY_LOADED_SUBSTRING)
                        || message.contains(JVM_ALREADY_LOADED_ALT_SUBSTRING))) {
                    // Suppress harmless duplicate load error and treat as already loaded
                    registry.markState(canonicalAlias, NativeLibraryState.LOADED);
                    return NativeLoadResult.success(canonicalAlias, true);
                }
                registry.markState(canonicalAlias, NativeLibraryState.FAILED);
                return NativeLoadResult.failure(canonicalAlias, linkError);
            } catch (Throwable otherError) {
                registry.markState(canonicalAlias, NativeLibraryState.FAILED);
                return NativeLoadResult.failure(canonicalAlias, otherError);
            }
        }
    }

    /**
     * Safely loads a system native library by name using standard {@link System#loadLibrary(String)}.
     *
     * @param libraryName name of the system library
     * @param ownerModuleId owner module identifier
     * @return outcome descriptor
     */
    public NativeLoadResult safeLoadLibrary(String libraryName, String ownerModuleId) {
        return guardLoad(libraryName, ownerModuleId, () -> System.loadLibrary(libraryName));
    }

    /**
     * Safely loads a native library from an absolute disk path using {@link System#load(String)}.
     *
     * @param absolutePath absolute file path of the native shared object
     * @param ownerModuleId owner module identifier
     * @return outcome descriptor
     */
    public NativeLoadResult safeLoad(Path absolutePath, String ownerModuleId) {
        Objects.requireNonNull(absolutePath, "absolutePath cannot be null");
        String fileName = absolutePath.getFileName().toString();
        String canonicalAlias = resolveAlias(fileName);

        registry.register(canonicalAlias, absolutePath, ownerModuleId);
        return guardLoad(fileName, ownerModuleId, () -> System.load(absolutePath.toAbsolutePath().toString()));
    }

    /**
     * Checks if a native library is already registered and loaded.
     *
     * @param libraryName library name
     * @return true if loaded
     */
    public boolean isLoaded(String libraryName) {
        String canonicalAlias = resolveAlias(libraryName);
        return registry.isLoaded(canonicalAlias) || registry.isLoaded(libraryName);
    }
}
