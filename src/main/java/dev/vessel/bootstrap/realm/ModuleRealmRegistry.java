package dev.vessel.bootstrap.realm;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Directory registry mapping module namespace identifiers to their isolated {@link ModuleRealmClassLoader} instances.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModuleRealmRegistry {

    private static final ModuleRealmRegistry INSTANCE = new ModuleRealmRegistry();

    private final Map<String, ModuleRealmClassLoader> realmLoaders;

    /**
     * Constructs a realm registry.
     */
    public ModuleRealmRegistry() {
        this.realmLoaders = new ConcurrentHashMap<>();
    }

    /**
     * Resolves the global singleton instance.
     *
     * @return singleton registry
     */
    public static ModuleRealmRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a module realm loader.
     *
     * @param realmId     unique realm identifier
     * @param realmLoader dedicated classloader
     */
    public void registerRealm(String realmId, ModuleRealmClassLoader realmLoader) {
        Objects.requireNonNull(realmId, "realmId cannot be null");
        Objects.requireNonNull(realmLoader, "realmLoader cannot be null");
        realmLoaders.put(realmId, realmLoader);
    }

    /**
     * Finds a realm classloader by realm id.
     *
     * @param realmId realm id
     * @return optional classloader
     */
    public Optional<ModuleRealmClassLoader> getRealm(String realmId) {
        return Optional.ofNullable(realmLoaders.get(realmId));
    }

    /**
     * Resolves all registered realm classloaders.
     *
     * @return unmodifiable collection of realm loaders
     */
    public Collection<ModuleRealmClassLoader> getAllRealms() {
        return Collections.unmodifiableCollection(realmLoaders.values());
    }

    /**
     * Resolves the number of active realms.
     *
     * @return realm count
     */
    public int getRealmCount() {
        return realmLoaders.size();
    }
}
