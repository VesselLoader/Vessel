package dev.vessel.bootstrap.realm;

import java.util.Objects;
import java.util.Optional;

/**
 * Cross-realm classloading coordinator and lookup router.
 *
 * <p>Dispatches class resolution requests across isolated module realms while enforcing
 * {@link ModuleRealmPackageExportFilter} visibility constraints to eliminate classpath pollution.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModuleCrossRealmBridgeRouter {

    private final ModuleRealmRegistry realmRegistry;
    private final ClassLoader vanillaGameClassLoader;

    /**
     * Constructs a cross-realm bridge router.
     *
     * @param realmRegistry          realm directory
     * @param vanillaGameClassLoader Tier-1 vanilla class loader
     */
    public ModuleCrossRealmBridgeRouter(
            ModuleRealmRegistry realmRegistry,
            ClassLoader vanillaGameClassLoader
    ) {
        this.realmRegistry = Objects.requireNonNull(realmRegistry, "realmRegistry cannot be null");
        this.vanillaGameClassLoader = Objects.requireNonNull(vanillaGameClassLoader, "vanillaGameClassLoader cannot be null");
    }

    /**
     * Routes class resolution across module realms.
     *
     * @param requestingRealmId requesting realm identifier
     * @param className         target class name
     * @return resolved class optional
     */
    public Optional<Class<?>> resolveCrossRealmClass(String requestingRealmId, String className) {
        Objects.requireNonNull(className, "className cannot be null");

        // 1. Query Vanilla / Tier-1 first
        try {
            Class<?> vanillaClass = Class.forName(className, false, vanillaGameClassLoader);
            return Optional.of(vanillaClass);
        } catch (ClassNotFoundException ignored) {
            // Proceed to other realms
        }

        // 2. Query other realms that export this class
        for (ModuleRealmClassLoader realmLoader : realmRegistry.getAllRealms()) {
            if (requestingRealmId != null && requestingRealmId.equals(realmLoader.getRealmId())) {
                continue;
            }
            if (!realmLoader.getExportFilter().isExported(className)) {
                continue;
            }
            try {
                Class<?> loaded = realmLoader.loadRealmLocalClass(className);
                return Optional.of(loaded);
            } catch (ClassNotFoundException ignored) {
                // Keep searching
            }
        }

        return Optional.empty();
    }
}
