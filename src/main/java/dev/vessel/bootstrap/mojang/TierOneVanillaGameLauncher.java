package dev.vessel.bootstrap.mojang;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Constructs the Tier-1 Vanilla Game Layer {@link URLClassLoader} from the unpacked Mojang
 * {@code server-<version>.jar} and bundled libraries, and reflectively invokes
 * {@code net.minecraft.server.Main#main(String[])}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TierOneVanillaGameLauncher {

    /**
     * Creates a Tier-1 {@link URLClassLoader} containing all extracted Mojang server and library
     * JAR paths.
     *
     * @param classpathJars     the ordered list of extracted server and library JAR paths
     * @param parentClassLoader the parent classloader (typically the VESSEL Kernel classloader)
     * @return the constructed {@link URLClassLoader}
     */
    public URLClassLoader createTierOneGameClassLoader(
            List<Path> classpathJars,
            ClassLoader parentClassLoader) {
        Objects.requireNonNull(classpathJars, "classpathJars must not be null");
        List<URL> urls = new ArrayList<>(classpathJars.size());
        for (Path jarPath : classpathJars) {
            if (jarPath != null) {
                try {
                    urls.add(jarPath.toUri().toURL());
                } catch (Exception e) {
                    throw new IllegalStateException("Invalid classpath JAR path: " + jarPath, e);
                }
            }
        }
        return new URLClassLoader(
                "vessel-tier1-vanilla-game",
                urls.toArray(new URL[0]),
                parentClassLoader != null ? parentClassLoader : ClassLoader.getSystemClassLoader());
    }

    /**
     * Loads {@code mainClassName} (e.g., {@code net.minecraft.server.Main}) from
     * {@code gameClassLoader} and invokes its {@code public static void main(String[])} entrypoint.
     *
     * @param gameClassLoader the Tier-1 game classloader
     * @param mainClassName   the fully qualified main class name
     * @param mcArgs          the command-line arguments to pass to Minecraft DedicatedServer
     * @return {@code true} if the entrypoint class was located and invoked successfully
     */
    public boolean invokeMinecraftServerMain(
            ClassLoader gameClassLoader,
            String mainClassName,
            String[] mcArgs) {
        Objects.requireNonNull(gameClassLoader, "gameClassLoader must not be null");
        Objects.requireNonNull(mainClassName, "mainClassName must not be null");
        Objects.requireNonNull(mcArgs, "mcArgs must not be null");

        ClassLoader previousContextLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(gameClassLoader);
            Class<?> mainClass = Class.forName(mainClassName, true, gameClassLoader);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) mcArgs);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to invoke Minecraft server entrypoint: " + mainClassName, e);
        } finally {
            Thread.currentThread().setContextClassLoader(previousContextLoader);
        }
    }
}
