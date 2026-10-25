package dev.vessel.bootstrap.mojang;

import dev.vessel.kernel.compat.era.MultiEraBytecodeTransformationPipeline;
import dev.vessel.kernel.identity.SourceLoaderKind;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Sovereign live classloader that loads physical {@code .class} entries from mod and plugin
 * {@code .jar} archives while transparently applying {@link MultiEraBytecodeTransformationPipeline}
 * in memory before {@link #defineClass(String, byte[], int, int)}.
 *
 * <p>This allows mods and plugins compiled for older Minecraft versions (1.8–1.20.6) to load and
 * execute on the latest Minecraft release in a single continuous pass without pre-patched JARs.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.3
 */
public final class LiveJarBytecodeTransformingClassLoader extends URLClassLoader {

    private final List<Path> jarPaths;
    private final MultiEraBytecodeTransformationPipeline transformationPipeline;
    private final boolean universalCompat;
    private final boolean deepDebug;

    /**
     * Constructs a live bytecode-transforming classloader over the given mod and plugin JAR paths.
     *
     * @param jarPaths        physical {@code .jar} paths discovered in {@code mods/} and {@code plugins/}
     * @param parent          parent classloader (typically the Tier-1 or VESSEL Kernel classloader)
     * @param universalCompat whether multi-era lower-version compatibility rewriting is active
     * @param deepDebug       whether deep bytecode diagnostic tracing is active
     */
    public LiveJarBytecodeTransformingClassLoader(
            List<Path> jarPaths,
            ClassLoader parent,
            boolean universalCompat,
            boolean deepDebug) {
        super(
                "vessel-live-multi-era-loader",
                new URL[0],
                parent != null ? parent : ClassLoader.getSystemClassLoader());
        this.jarPaths = List.copyOf(Objects.requireNonNull(jarPaths, "jarPaths must not be null"));
        this.transformationPipeline = new MultiEraBytecodeTransformationPipeline();
        this.universalCompat = universalCompat;
        this.deepDebug = deepDebug;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name must not be null");
        String entryName = name.replace('.', '/') + ".class";
        byte[] rawBytes = readClassBytesFromJars(entryName);
        if (rawBytes == null) {
            return super.findClass(name);
        }
        byte[] transformedBytes = rawBytes;
        if (universalCompat) {
            byte[] modUpgraded =
                    transformationPipeline.transformLowerVersionClassForLatestServer(
                            rawBytes,
                            SourceLoaderKind.FABRIC,
                            "1.16.5",
                            MojangVersionManifestResolver.FALLBACK_LATEST_RELEASE);
            transformedBytes =
                    transformationPipeline.transformLowerVersionClassForLatestServer(
                            modUpgraded,
                            SourceLoaderKind.PAPER,
                            "1.16.5",
                            MojangVersionManifestResolver.FALLBACK_LATEST_RELEASE);
        }
        if (deepDebug && transformedBytes.length == 0) {
            transformedBytes = rawBytes;
        }
        return defineClass(name, transformedBytes, 0, transformedBytes.length);
    }

    private byte[] readClassBytesFromJars(String entryName) throws ClassNotFoundException {
        for (Path jarPath : jarPaths) {
            if (jarPath == null || !Files.isRegularFile(jarPath)) {
                continue;
            }
            try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                JarEntry entry = jarFile.getJarEntry(entryName);
                if (entry != null) {
                    try (InputStream in = jarFile.getInputStream(entry)) {
                        return in.readAllBytes();
                    }
                }
            } catch (IOException e) {
                throw new ClassNotFoundException(
                        "Failed to read class entry " + entryName + " from " + jarPath, e);
            }
        }
        return null;
    }
}
