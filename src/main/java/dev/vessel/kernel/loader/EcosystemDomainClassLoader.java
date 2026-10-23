package dev.vessel.kernel.loader;

import dev.vessel.kernel.identity.SourceLoaderKind;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tier-3 Ecosystem Domain ClassLoader in VESSEL's 4-tier ClassLoader hierarchy.
 *
 * <p>Isolates public loader API contracts (Fabric Loader API, Quilt Loader API, Forge/NeoForge
 * EventBus and Capabilities, Bukkit/Spigot/Paper API) per {@link SourceLoaderKind}, enforcing
 * strict package prefix allowlists to prevent cross-ecosystem ABI pollution.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-28)
 */
public final class EcosystemDomainClassLoader extends ClassLoader {

    private final SourceLoaderKind ecosystemDomain;
    private final Set<String> allowedPackagePrefixes = ConcurrentHashMap.newKeySet();
    private final Map<String, byte[]> registeredApiBytecode = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> definedClasses = new ConcurrentHashMap<>();

    public EcosystemDomainClassLoader(SourceLoaderKind ecosystemDomain, ClassLoader parent) {
        super(parent);
        this.ecosystemDomain = Objects.requireNonNull(ecosystemDomain, "ecosystemDomain cannot be null");
        allowedPackagePrefixes.add("java.");
        allowedPackagePrefixes.add("javax.");
        allowedPackagePrefixes.add("dev.vessel.");
    }

    public EcosystemDomainClassLoader(SourceLoaderKind ecosystemDomain) {
        this(ecosystemDomain, EcosystemDomainClassLoader.class.getClassLoader());
    }

    /**
     * Loads an ecosystem API class if its binary name matches one of the allowed package prefixes
     * registered for this ecosystem domain.
     *
     * @param binaryName fully qualified binary class name
     * @param resolve    whether to link/resolve the loaded class
     * @return loaded {@link Class} object
     * @throws ClassNotFoundException if the package prefix is not permitted or the class cannot be found
     */
    public Class<?> loadEcosystemApiClass(String binaryName, boolean resolve) throws ClassNotFoundException {
        Objects.requireNonNull(binaryName, "binaryName cannot be null");

        if (!isPackageAllowed(binaryName)) {
            throw new ClassNotFoundException(
                    "Class '" + binaryName + "' is outside allowed API prefixes for domain " + ecosystemDomain
            );
        }

        Class<?> cached = definedClasses.get(binaryName);
        if (cached != null) {
            if (resolve) {
                resolveClass(cached);
            }
            return cached;
        }

        byte[] customBytes = registeredApiBytecode.get(binaryName);
        if (customBytes != null) {
            Class<?> defined = defineClass(binaryName, customBytes, 0, customBytes.length);
            definedClasses.put(binaryName, defined);
            if (resolve) {
                resolveClass(defined);
            }
            return defined;
        }

        return super.loadClass(binaryName, resolve);
    }

    /**
     * Registers an allowed package prefix (e.g. {@code "net.fabricmc.loader.api."}) for this
     * ecosystem domain.
     *
     * @param packagePrefix package prefix in binary dotted or internal slash notation
     */
    public void registerAllowedApiPackagePrefix(String packagePrefix) {
        Objects.requireNonNull(packagePrefix, "packagePrefix cannot be null");
        String normalized = packagePrefix.replace('/', '.');
        if (!normalized.isEmpty()) {
            allowedPackagePrefixes.add(normalized);
        }
    }

    /**
     * Registers in-memory API class bytecode for this ecosystem domain.
     *
     * @param binaryName binary class name
     * @param classBytes raw JVM classfile bytes
     */
    public void registerInMemoryApiClass(String binaryName, byte[] classBytes) {
        Objects.requireNonNull(binaryName, "binaryName cannot be null");
        Objects.requireNonNull(classBytes, "classBytes cannot be null");
        registeredApiBytecode.put(binaryName.replace('/', '.'), classBytes.clone());
    }

    /**
     * Returns the source loader ecosystem domain managed by this classloader.
     *
     * @return ecosystem domain
     */
    public SourceLoaderKind getEcosystemDomain() {
        return ecosystemDomain;
    }

    private boolean isPackageAllowed(String binaryName) {
        for (String prefix : allowedPackagePrefixes) {
            if (binaryName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
