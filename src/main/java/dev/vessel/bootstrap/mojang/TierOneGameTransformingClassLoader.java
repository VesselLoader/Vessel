package dev.vessel.bootstrap.mojang;

import dev.vessel.bootstrap.classloading.UnifiedGameAndModClassLoaderBridge;
import dev.vessel.kernel.mixin.weaver.RuntimeMixinTargetRegistry;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * Dedicated Tier-1 Game ClassLoader hosting running Minecraft server classes and bundled libraries,
 * while transparently executing the unified {@link TierOneBytecodeTransformerPipeline} to inject
 * server brand, lifecycle hooks, tick monitoring, command dispatchers, permissions, optimization
 * governors, and GPU/SIMD raycast offloaders.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.18
 * @author VESSEL Engine Team
 */
public final class TierOneGameTransformingClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final TierOneBytecodeTransformerPipeline transformerPipeline = new TierOneBytecodeTransformerPipeline();
    private final TierOneGamePackageFilter gamePackageFilter = new TierOneGamePackageFilter();
    private final TransformerFailureDiagnosticLogger diagnosticLogger;
    private volatile UnifiedGameAndModClassLoaderBridge unifiedBridge;
    private volatile RuntimeMixinTargetRegistry runtimeMixinTargetRegistry;

    public TierOneGameTransformingClassLoader(
            String name,
            URL[] urls,
            ClassLoader parent) {
        this(name, urls, parent, new TransformerFailureDiagnosticLogger());
    }

    public TierOneGameTransformingClassLoader(
            String name,
            URL[] urls,
            ClassLoader parent,
            TransformerFailureDiagnosticLogger diagnosticLogger) {
        super(name, urls, parent);
        this.diagnosticLogger = Objects.requireNonNull(diagnosticLogger, "diagnosticLogger must not be null");
    }

    public TransformerFailureDiagnosticLogger getDiagnosticLogger() {
        return diagnosticLogger;
    }

    public TierOneBytecodeTransformerPipeline getTransformerPipeline() {
        return transformerPipeline;
    }

    public void setUnifiedClassLoaderBridge(UnifiedGameAndModClassLoaderBridge unifiedBridge) {
        this.unifiedBridge = unifiedBridge;
    }

    public UnifiedGameAndModClassLoaderBridge getUnifiedClassLoaderBridge() {
        return unifiedBridge;
    }

    public void setRuntimeMixinTargetRegistry(RuntimeMixinTargetRegistry runtimeMixinTargetRegistry) {
        this.runtimeMixinTargetRegistry = runtimeMixinTargetRegistry;
    }

    public RuntimeMixinTargetRegistry getRuntimeMixinTargetRegistry() {
        return runtimeMixinTargetRegistry;
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

        if (transformerPipeline.isTransformedClass(name, runtimeMixinTargetRegistry)) {
            String resPath = name.replace('.', '/') + ".class";
            URL selfRes = findResource(resPath);
            if (selfRes != null) {
                try {
                    java.net.URLConnection conn = selfRes.openConnection();
                    conn.setUseCaches(false);
                    byte[] raw;
                    try (InputStream in = conn.getInputStream()) {
                        raw = in.readAllBytes();
                    }
                    byte[] transformed = transformerPipeline.transformClassBytecode(
                            name, raw, runtimeMixinTargetRegistry);
                    return defineClass(name, transformed, 0, transformed.length);
                } catch (Exception e) {
                    diagnosticLogger.logFailure(name, "TierOneBytecodeTransformerPipeline", e);
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
