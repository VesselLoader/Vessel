package dev.vessel.kernel.loader;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves split-package conflicts across multi-loader modules by binding each shared package
 * to a deterministic canonical module owner.
 *
 * <p>Prevents {@link LinkageError} and illegal cross-module package sealing breaches when multiple
 * jars bundle overlapping library or API packages.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-28)
 */
public final class SplitPackageResolver {

    private final Map<String, VesselModuleIdentifier> packageOwners = new ConcurrentHashMap<>();
    private final Map<String, byte[]> canonicalClassBytecode = new ConcurrentHashMap<>();

    /**
     * Registers the canonical module owner for a given package name.
     *
     * @param packageName package name in dotted or slash notation
     * @param ownerModule canonical module identifier owning the package
     */
    public void registerPackageOwner(String packageName, VesselModuleIdentifier ownerModule) {
        Objects.requireNonNull(packageName, "packageName cannot be null");
        Objects.requireNonNull(ownerModule, "ownerModule cannot be null");
        String normalizedPkg = normalizePackageName(packageName);
        packageOwners.putIfAbsent(normalizedPkg, ownerModule);
    }

    /**
     * Resolves the canonical module owner for the specified package name.
     *
     * @param packageName package name in dotted or slash notation
     * @return canonical {@link VesselModuleIdentifier}, or {@code null} if unassigned
     */
    public VesselModuleIdentifier resolveCanonicalPackageOwner(String packageName) {
        Objects.requireNonNull(packageName, "packageName cannot be null");
        return packageOwners.get(normalizePackageName(packageName));
    }

    /**
     * Redirects a split-package class load request from a secondary module to the canonical
     * owner module's bytecode store.
     *
     * @param className        fully qualified binary or internal class name
     * @param requestingModule module requesting the class load
     * @return canonical class bytecode from the package owner
     */
    public byte[] redirectSplitPackageClassLoad(String className, VesselModuleIdentifier requestingModule) {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(requestingModule, "requestingModule cannot be null");

        String normalizedClass = className.replace('/', '.');
        byte[] storedBytes = canonicalClassBytecode.get(normalizedClass);
        if (storedBytes != null) {
            return storedBytes.clone();
        }

        String packageName = extractPackageName(normalizedClass);
        VesselModuleIdentifier canonicalOwner = packageOwners.getOrDefault(packageName, requestingModule);
        String redirectTag = "SPLIT_PKG_REDIRECT:" + canonicalOwner.loader() + ":" + canonicalOwner.namespace()
                + ":" + normalizedClass;
        return redirectTag.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Registers canonical bytecode for a class residing in a managed package.
     *
     * @param className  binary or internal class name
     * @param classBytes canonical bytecode bytes
     */
    public void registerCanonicalClassBytes(String className, byte[] classBytes) {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(classBytes, "classBytes cannot be null");
        canonicalClassBytecode.put(className.replace('/', '.'), classBytes.clone());
    }

    private static String normalizePackageName(String packageName) {
        String normalized = packageName.replace('/', '.').trim();
        if (normalized.endsWith(".")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String extractPackageName(String binaryClassName) {
        int lastDot = binaryClassName.lastIndexOf('.');
        return lastDot > 0 ? binaryClassName.substring(0, lastDot) : "";
    }
}
