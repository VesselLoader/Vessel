package dev.vessel.kernel.compat.era;

import java.util.Objects;
import java.util.ServiceLoader;

/**
 * Universal multi-era bridge for {@link ServiceLoader#load(Class)} calls from mods and libraries.
 * <p>
 * Ensures that service lookups (such as Catnip/Create platform services, Flywheel, Registrate)
 * query the service interface's own classloader when the thread context classloader is constrained
 * to Vanilla/System classloaders.
 * </p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossEraServiceLoaderBridge {

    private CrossEraServiceLoaderBridge() {
    }

    /**
     * Resiliently loads services by falling back to the service interface's classloader
     * if the thread context classloader yields no providers.
     *
     * @param <T>     service type
     * @param service service class or interface
     * @return active {@link ServiceLoader} instance containing discovered providers
     */
    public static <T> ServiceLoader<T> load(Class<T> service) {
        Objects.requireNonNull(service, "service must not be null");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        ServiceLoader<T> loader = tccl != null ? ServiceLoader.load(service, tccl) : ServiceLoader.load(service);
        if (loader.findFirst().isPresent()) {
            return loader;
        }

        ClassLoader serviceClassLoader = service.getClassLoader();
        if (serviceClassLoader != null && serviceClassLoader != tccl) {
            ServiceLoader<T> fallbackLoader = ServiceLoader.load(service, serviceClassLoader);
            if (fallbackLoader.findFirst().isPresent()) {
                return fallbackLoader;
            }
        }
        return loader;
    }
}
