package dev.vessel.kernel.nativebridge;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry managing native shared libraries (.dll, .so, .dylib) loaded at the Root ClassLoader.
 * Prevents multiple conflicting loads of duplicate native binaries across multi-loader modules (ERR-29, OPT-015).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GlobalNativeLibraryRegistry {

    private static final String LIB_PREFIX = "lib";
    private static final String EXT_SO = ".so";
    private static final String EXT_DLL = ".dll";
    private static final String EXT_DYLIB = ".dylib";
    private static final String EXT_JNILIB = ".jnilib";

    private static final GlobalNativeLibraryRegistry INSTANCE = new GlobalNativeLibraryRegistry();

    private final ConcurrentMap<String, NativeLibraryEntry> entriesByCanonicalName = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, NativeLibraryEntry> entriesByNormalizedName = new ConcurrentHashMap<>();

    /**
     * Constructs a standalone GlobalNativeLibraryRegistry.
     */
    public GlobalNativeLibraryRegistry() {
    }

    /**
     * Returns the singleton instance of the GlobalNativeLibraryRegistry.
     *
     * @return the singleton registry
     */
    public static GlobalNativeLibraryRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Normalizes a native library file name or name token into a platform-agnostic identifier.
     * Strips "lib" prefix and extensions (".dll", ".so", ".dylib", ".jnilib").
     *
     * @param rawName raw library name or filename
     * @return normalized lowercase library key
     */
    public static String normalizeLibraryName(String rawName) {
        Objects.requireNonNull(rawName, "rawName cannot be null");
        String name = rawName.trim().toLowerCase(Locale.ROOT);

        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < name.length() - 1) {
            name = name.substring(lastSlash + 1);
        }

        if (name.endsWith(EXT_SO)) {
            name = name.substring(0, name.length() - EXT_SO.length());
        } else if (name.endsWith(EXT_DLL)) {
            name = name.substring(0, name.length() - EXT_DLL.length());
        } else if (name.endsWith(EXT_DYLIB)) {
            name = name.substring(0, name.length() - EXT_DYLIB.length());
        } else if (name.endsWith(EXT_JNILIB)) {
            name = name.substring(0, name.length() - EXT_JNILIB.length());
        }

        if (name.startsWith(LIB_PREFIX) && name.length() > LIB_PREFIX.length()) {
            name = name.substring(LIB_PREFIX.length());
        }

        return name;
    }

    /**
     * Registers a native library with initial REGISTERED state if not already present.
     *
     * @param libraryName primary library name
     * @param libraryPath absolute file path on disk, or null if system-managed
     * @param ownerModuleId owner module identifier
     * @return the existing or newly registered entry
     */
    public NativeLibraryEntry register(String libraryName, Path libraryPath, String ownerModuleId) {
        Objects.requireNonNull(libraryName, "libraryName cannot be null");
        Objects.requireNonNull(ownerModuleId, "ownerModuleId cannot be null");

        String normalized = normalizeLibraryName(libraryName);
        return entriesByNormalizedName.computeIfAbsent(normalized, k -> {
            NativeLibraryEntry newEntry = new NativeLibraryEntry(
                    libraryName,
                    normalized,
                    libraryPath,
                    ownerModuleId,
                    NativeLibraryState.REGISTERED,
                    System.nanoTime()
            );
            entriesByCanonicalName.put(libraryName, newEntry);
            return newEntry;
        });
    }

    /**
     * Finds a library entry by raw or canonical library name.
     *
     * @param libraryName raw library name
     * @return optional containing the entry if found
     */
    public Optional<NativeLibraryEntry> find(String libraryName) {
        if (libraryName == null) {
            return Optional.empty();
        }
        NativeLibraryEntry direct = entriesByCanonicalName.get(libraryName);
        if (direct != null) {
            return Optional.of(direct);
        }
        String normalized = normalizeLibraryName(libraryName);
        return Optional.ofNullable(entriesByNormalizedName.get(normalized));
    }

    /**
     * Checks if a native library has already been successfully loaded into the JVM.
     *
     * @param libraryName library name
     * @return true if state is LOADED
     */
    public boolean isLoaded(String libraryName) {
        return find(libraryName)
                .map(e -> e.state() == NativeLibraryState.LOADED)
                .orElse(false);
    }

    /**
     * Transitions the state of a registered library.
     *
     * @param libraryName library name
     * @param newState target state
     * @return true if updated, false if library was not found
     */
    public boolean markState(String libraryName, NativeLibraryState newState) {
        Objects.requireNonNull(newState, "newState cannot be null");
        Optional<NativeLibraryEntry> opt = find(libraryName);
        if (opt.isEmpty()) {
            return false;
        }
        NativeLibraryEntry entry = opt.get();
        NativeLibraryEntry updated = entry.withState(newState);
        entriesByNormalizedName.put(entry.normalizedName(), updated);
        entriesByCanonicalName.put(entry.libraryName(), updated);
        return true;
    }

    /**
     * Marks a native library as LOADED.
     *
     * @param libraryName library name
     * @return true if updated
     */
    public boolean markLoaded(String libraryName) {
        return markState(libraryName, NativeLibraryState.LOADED);
    }

    /**
     * Returns an unmodifiable collection of all registered native library entries.
     *
     * @return collection of entries
     */
    public Collection<NativeLibraryEntry> allEntries() {
        return Collections.unmodifiableCollection(entriesByNormalizedName.values());
    }

    /**
     * Returns the total count of registered native libraries.
     *
     * @return registered count
     */
    public int size() {
        return entriesByNormalizedName.size();
    }

    /**
     * Clears all registered entries.
     */
    public void clear() {
        entriesByCanonicalName.clear();
        entriesByNormalizedName.clear();
    }
}
