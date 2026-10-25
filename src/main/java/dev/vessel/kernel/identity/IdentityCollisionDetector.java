package dev.vessel.kernel.identity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Registry and collision detector for scoped module identities.
 * <p>
 * Enforces Architectural Decisions 7 and 8:
 * <ul>
 *   <li><b>Decision 7:</b> Modules sharing the same namespace across <i>different</i> loaders
 *       (e.g., {@code bukkit:worldedit} and {@code fabric:worldedit}) are isolated in distinct
 *       loader scopes and registered concurrently without interference.</li>
 *   <li><b>Decision 8 (Hard Stop):</b> Duplicate namespace claims within the <i>same</i> loader
 *       immediately throw {@link ModuleCollisionException}, identifying both conflicting modules
 *       and originating JAR paths to protect server integrity.</li>
 * </ul>
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-4)
 */
public class IdentityCollisionDetector {

    private record ScopedKey(SourceLoaderKind loader, String namespace) {}

    private record Entry(VesselModuleIdentifier identifier, String jarPath) {}

    private final Map<ScopedKey, Entry> registeredModules = new HashMap<>();

    /**
     * Registers a module and performs collision detection.
     *
     * @param identifier the canonical module identifier
     * @param jarPath originating JAR file path for diagnostic reporting
     * @throws ModuleCollisionException if another module has already claimed this namespace under the same loader
     */
    public synchronized void register(VesselModuleIdentifier identifier, String jarPath) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(jarPath, "jarPath cannot be null");

        ScopedKey key = new ScopedKey(identifier.loader(), identifier.namespace());
        Entry existing = registeredModules.get(key);

        if (existing != null) {
            throw new ModuleCollisionException(
                    existing.identifier(),
                    identifier,
                    existing.jarPath(),
                    jarPath
            );
        }

        registeredModules.put(key, new Entry(identifier, jarPath));
    }

    /**
     * Checks if a namespace is already claimed under the specified loader.
     *
     * @param loader ecosystem loader
     * @param namespace canonical module namespace
     * @return true if already registered
     */
    public synchronized boolean isRegistered(SourceLoaderKind loader, String namespace) {
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        return registeredModules.containsKey(new ScopedKey(loader, namespace.trim().toLowerCase()));
    }

    /**
     * Retrieves the registered module identifier for a given loader and namespace.
     *
     * @param loader ecosystem loader
     * @param namespace canonical module namespace
     * @return optional containing the identifier if present
     */
    public synchronized Optional<VesselModuleIdentifier> get(SourceLoaderKind loader, String namespace) {
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Entry entry = registeredModules.get(new ScopedKey(loader, namespace.trim().toLowerCase()));
        return entry != null ? Optional.of(entry.identifier()) : Optional.empty();
    }

    /**
     * Returns total count of registered modules.
     *
     * @return registered module count
     */
    public synchronized int size() {
        return registeredModules.size();
    }

    /**
     * Clears all registered module mappings.
     */
    public synchronized void clear() {
        registeredModules.clear();
    }
}
