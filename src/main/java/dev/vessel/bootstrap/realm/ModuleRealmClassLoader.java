package dev.vessel.bootstrap.realm;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * Isolated ClassLoader instance dedicated to a single mod or plugin artifact.
 *
 * <p>Enables parallel class loading via {@link ClassLoader#registerAsParallelCapable()},
 * preventing ClassLoader lock contention in Folia concurrent region strands.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ModuleRealmClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }

    private final String realmId;
    private final ModuleRealmPackageExportFilter exportFilter;

    /**
     * Constructs a dedicated realm class loader.
     *
     * @param realmId      module namespace identifier
     * @param urls         jar URLs
     * @param parent       parent class loader (typically Tier-1 game loader)
     * @param exportFilter visibility policy filter
     */
    public ModuleRealmClassLoader(
            String realmId,
            URL[] urls,
            ClassLoader parent,
            ModuleRealmPackageExportFilter exportFilter
    ) {
        super(urls, parent);
        this.realmId = Objects.requireNonNull(realmId, "realmId cannot be null");
        this.exportFilter = Objects.requireNonNull(exportFilter, "exportFilter cannot be null");
    }

    /**
     * Resolves the module realm identifier.
     *
     * @return realm id
     */
    public String getRealmId() {
        return realmId;
    }

    /**
     * Resolves the visibility filter.
     *
     * @return export filter
     */
    public ModuleRealmPackageExportFilter getExportFilter() {
        return exportFilter;
    }

    /**
     * Attempts to find a class within this realm's private classpath only if exported or internal.
     *
     * @param name class name
     * @return class object
     * @throws ClassNotFoundException if not found in this realm
     */
    public Class<?> loadRealmLocalClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }
}
