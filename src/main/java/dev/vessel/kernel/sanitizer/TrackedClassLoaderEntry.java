package dev.vessel.kernel.sanitizer;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Immutable tracking metadata associated with a classloader queued for inspection and Metaspace reclamation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param loaderName diagnostic identifier of the classloader
 * @param reference weak reference to the monitored ClassLoader
 * @param registeredEpochMs timestamp when the classloader was registered
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TrackedClassLoaderEntry(
        String loaderName,
        WeakReference<ClassLoader> reference,
        long registeredEpochMs
) {
    public TrackedClassLoaderEntry {
        Objects.requireNonNull(loaderName, "loaderName must not be null");
        Objects.requireNonNull(reference, "reference must not be null");
    }

    /**
     * Checks if the tracked ClassLoader has been cleared by garbage collection.
     *
     * @return true if the referenced ClassLoader is unreachable
     */
    public boolean isCollected() {
        return reference.get() == null;
    }
}
