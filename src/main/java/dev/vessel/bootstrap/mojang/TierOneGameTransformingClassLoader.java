package dev.vessel.bootstrap.mojang;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * Dedicated Tier-1 Game ClassLoader that hosts the running Minecraft server classes and bundled
 * libraries, while transparently applying {@link VesselServerBrandTransformer} to
 * {@code net.minecraft.server.MinecraftServer} so the server brand is {@code "VESSEL"}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TierOneGameTransformingClassLoader extends URLClassLoader {

    private final VesselServerBrandTransformer brandTransformer = new VesselServerBrandTransformer();

    public TierOneGameTransformingClassLoader(
            String name,
            URL[] urls,
            ClassLoader parent) {
        super(name, urls, parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name must not be null");
        if (VesselServerBrandTransformer.TARGET_CLASS_NAME.equals(name)) {
            String resPath = VesselServerBrandTransformer.TARGET_INTERNAL_NAME + ".class";
            try (InputStream in = getResourceAsStream(resPath)) {
                if (in != null) {
                    byte[] raw = in.readAllBytes();
                    byte[] transformed = brandTransformer.transformServerBrand(raw);
                    return defineClass(name, transformed, 0, transformed.length);
                }
            } catch (Exception ignored) {
                // Fall back to standard definition
            }
        }
        return super.findClass(name);
    }
}
