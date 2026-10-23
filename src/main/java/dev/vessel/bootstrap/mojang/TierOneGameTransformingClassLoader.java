package dev.vessel.bootstrap.mojang;

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
 * @version 1.0.1
 */
public final class TierOneGameTransformingClassLoader extends URLClassLoader {

    private final VesselServerBrandTransformer brandTransformer = new VesselServerBrandTransformer();
    private final VesselServerLifecycleTransformer lifecycleTransformer = new VesselServerLifecycleTransformer();
    private final VesselCommandsTransformer commandsTransformer = new VesselCommandsTransformer();

    public TierOneGameTransformingClassLoader(
            String name,
            URL[] urls,
            ClassLoader parent) {
        super(name, urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name must not be null");
        // Always prioritize parent classloader for VESSEL core classes to guarantee class identity
        if (name.startsWith("dev.vessel.") && getParent() != null) {
            try {
                return getParent().loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name must not be null");

        if (VesselServerBrandTransformer.TARGET_CLASS_NAME.equals(name)) {
            String resPath = VesselServerBrandTransformer.TARGET_INTERNAL_NAME + ".class";
            try (InputStream in = getResourceAsStream(resPath)) {
                if (in != null) {
                    byte[] raw = in.readAllBytes();
                    byte[] branded = brandTransformer.transformServerBrand(raw);
                    byte[] hooked = lifecycleTransformer.transformServerLifecycle(branded);
                    return defineClass(name, hooked, 0, hooked.length);
                }
            } catch (Exception ignored) {
                // Fall back to standard definition
            }
        }

        if (VesselCommandsTransformer.TARGET_CLASS_NAME.equals(name)) {
            String resPath = VesselCommandsTransformer.TARGET_INTERNAL_NAME + ".class";
            try (InputStream in = getResourceAsStream(resPath)) {
                if (in != null) {
                    byte[] raw = in.readAllBytes();
                    byte[] hooked = commandsTransformer.transformCommands(raw);
                    return defineClass(name, hooked, 0, hooked.length);
                }
            } catch (Exception ignored) {
                // Fall back to standard definition
            }
        }

        return super.findClass(name);
    }
}
