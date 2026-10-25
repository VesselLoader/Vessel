package dev.vessel.kernel.nativebridge;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable registry entry capturing metadata for a native library (.dll, .so, .dylib).
 *
 * @param libraryName primary identifier or canonical name of the native library
 * @param normalizedName platform-neutral stripped name used for duplicate detection
 * @param libraryPath absolute file path on disk, or null if system-provided
 * @param ownerModuleId module or subsystem that registered this library
 * @param state current lifecycle state
 * @param registeredTimestampNanos timestamp in nanoseconds when registered
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record NativeLibraryEntry(
        String libraryName,
        String normalizedName,
        Path libraryPath,
        String ownerModuleId,
        NativeLibraryState state,
        long registeredTimestampNanos
) {
    public NativeLibraryEntry {
        Objects.requireNonNull(libraryName, "libraryName cannot be null");
        Objects.requireNonNull(normalizedName, "normalizedName cannot be null");
        Objects.requireNonNull(ownerModuleId, "ownerModuleId cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * Creates an updated entry with a new lifecycle state.
     *
     * @param newState updated state
     * @return new entry instance with updated state
     */
    public NativeLibraryEntry withState(NativeLibraryState newState) {
        return new NativeLibraryEntry(
                libraryName,
                normalizedName,
                libraryPath,
                ownerModuleId,
                newState,
                registeredTimestampNanos
        );
    }
}
