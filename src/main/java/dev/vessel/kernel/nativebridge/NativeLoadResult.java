package dev.vessel.kernel.nativebridge;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable outcome descriptor for a guarded native library load operation.
 *
 * @param success whether the library is loaded and operational
 * @param wasAlreadyLoaded whether the load was intercepted because the library was already loaded
 * @param libraryName name of the native library
 * @param failureCause exception if load failed, or null if successful
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record NativeLoadResult(
        boolean success,
        boolean wasAlreadyLoaded,
        String libraryName,
        Throwable failureCause
) {
    public NativeLoadResult {
        Objects.requireNonNull(libraryName, "libraryName cannot be null");
    }

    /**
     * Factory method for a successful load operation.
     *
     * @param libraryName name of the library
     * @param wasAlreadyLoaded true if already linked earlier
     * @return successful NativeLoadResult
     */
    public static NativeLoadResult success(String libraryName, boolean wasAlreadyLoaded) {
        return new NativeLoadResult(true, wasAlreadyLoaded, libraryName, null);
    }

    /**
     * Factory method for a failed load operation.
     *
     * @param libraryName name of the library
     * @param cause failure cause
     * @return failed NativeLoadResult
     */
    public static NativeLoadResult failure(String libraryName, Throwable cause) {
        return new NativeLoadResult(false, false, libraryName, cause);
    }

    /**
     * Returns an optional containing the failure cause if present.
     *
     * @return optional exception
     */
    public Optional<Throwable> error() {
        return Optional.ofNullable(failureCause);
    }
}
