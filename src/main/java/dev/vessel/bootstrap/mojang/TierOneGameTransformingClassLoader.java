package dev.vessel.bootstrap.mojang;

import dev.vessel.bootstrap.classloading.UnifiedGameAndModClassLoaderBridge;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * Dedicated Tier-1 Game ClassLoader that hosts the running Minecraft server classes and bundled
 * libraries, while transparently applying {@link VesselServerBrandTransformer},
 * {@link VesselServerLifecycleTransformer}, and {@link VesselCommandsTransformer} so the server
 * brand is {@code "VESSEL"} and commands dispatcher hooks are wired at instantiation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.4
 */
public final class TierOneGameTransformingClassLoader extends URLClassLoader {

    private final VesselServerBrandTransformer brandTransformer = new VesselServerBrandTransformer();
    private final VesselServerLifecycleTransformer lifecycleTransformer = new VesselServerLifecycleTransformer();
    private final VesselCommandsTransformer commandsTransformer = new VesselCommandsTransformer();
    private final TierOneGamePackageFilter gamePackageFilter = new TierOneGamePackageFilter();
    private volatile UnifiedGameAndModClassLoaderBridge unifiedBridge;

    public TierOneGameTransformingClassLoader(
            String name,
            URL[] urls,
            ClassLoader parent) {
        super(name, urls, parent);
    }

    public void setUnifiedClassLoaderBridge(UnifiedGameAndModClassLoaderBridge unifiedBridge) {
        this.unifiedBridge = unifiedBridge;
    }

    public UnifiedGameAndModClassLoaderBridge getUnifiedClassLoaderBridge() {
        return unifiedBridge;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name must not be null");
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) {
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }

            // Always prioritize parent classloader for VESSEL core classes to guarantee class identity
            if (name.startsWith("dev.vessel.") && getParent() != null) {
                try {
                    Class<?> kernelClass = getParent().loadClass(name);
                    if (resolve) {
                        resolveClass(kernelClass);
                    }
                    return kernelClass;
                } catch (ClassNotFoundException ignored) {
                }
            }

            // Child-First (Self-First) for Minecraft vanilla game and Mojang library packages:
            // Always prioritize the real Minecraft server JAR and library classpath over kernel stubs.
            if (gamePackageFilter.isGameOrMojangClass(name)) {
                try {
                    Class<?> gameClass = findClass(name);
                    if (resolve) {
                        resolveClass(gameClass);
                    }
                    return gameClass;
                } catch (ClassNotFoundException ignored) {
                    // Fall back to parent if not found directly in game classpath
                }
            }

            return super.loadClass(name, resolve);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name must not be null");

        if (VesselServerBrandTransformer.isServerClass(name)) {
            String resPath = name.replace('.', '/') + ".class";
            URL selfRes = findResource(resPath);
            if (selfRes != null) {
                try (InputStream in = selfRes.openStream()) {
                    byte[] raw = in.readAllBytes();
                    byte[] branded = brandTransformer.transformServerBrand(raw);
                    byte[] hooked = lifecycleTransformer.transformServerLifecycle(branded);
                    return defineClass(name, hooked, 0, hooked.length);
                } catch (Exception ignored) {
                    // Fall back to standard definition
                }
            }
        }

        if (VesselCommandsTransformer.isCommandsClass(name)) {
            String resPath = name.replace('.', '/') + ".class";
            URL selfRes = findResource(resPath);
            if (selfRes != null) {
                try (InputStream in = selfRes.openStream()) {
                    byte[] raw = in.readAllBytes();
                    byte[] hooked = commandsTransformer.transformCommands(raw);
                    return defineClass(name, hooked, 0, hooked.length);
                } catch (Exception ignored) {
                    // Fall back to standard definition
                }
            }
        }

        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            if (unifiedBridge != null) {
                Class<?> modLoaded = unifiedBridge.tryLoadFromModClassLoader(name);
                if (modLoaded != null) {
                    return modLoaded;
                }
            }
            throw e;
        }
    }
}
