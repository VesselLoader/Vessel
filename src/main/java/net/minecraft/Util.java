package net.minecraft;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * Universal multi-era compatibility bridge providing the legacy {@code net.minecraft.Util}
 * static utility methods expected by Minecraft 1.20.1 Forge and Fabric mods.
 *
 * <p>In modern Minecraft (1.21+ / 26.3), Mojang relocated this class to {@code net.minecraft.util.Util}.
 * This shim ensures 1.20.1 bytecode linking resolves seamlessly without breaking class verification.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Util {

    private Util() {
    }

    /**
     * Constructs a namespaced translation or description identifier string.
     *
     * @param prefix translation prefix (e.g., "block", "item", "constants")
     * @param id     resource location identifier
     * @return dot-separated description id
     */
    public static String makeDescriptionId(String prefix, ResourceLocation id) {
        if (id == null) {
            return prefix != null ? prefix : "";
        }
        String cleanPrefix = prefix != null ? prefix : "";
        return cleanPrefix.isEmpty()
                ? id.getNamespace() + "." + id.getPath()
                : cleanPrefix + "." + id.getNamespace() + "." + id.getPath();
    }

    /**
     * SRG mapped method alias for {@link #makeDescriptionId(String, ResourceLocation)}.
     *
     * @param prefix translation prefix
     * @param id     resource location
     * @return dot-separated description id
     */
    public static String m_137492_(String prefix, ResourceLocation id) {
        return makeDescriptionId(prefix, id);
    }

    /**
     * Utility method executing a supplier and returning its result.
     *
     * @param <T>      return type
     * @param supplier instance supplier
     * @return supplied instance
     */
    public static <T> T make(Supplier<T> supplier) {
        return supplier != null ? supplier.get() : null;
    }

    /**
     * Utility method configuring an object via a consumer and returning it.
     *
     * @param <T>      target type
     * @param object   target object
     * @param consumer configuration logic
     * @return configured object
     */
    public static <T> T make(T object, Consumer<T> consumer) {
        if (consumer != null && object != null) {
            consumer.accept(object);
        }
        return object;
    }

    /**
     * Returns the global background worker executor service.
     *
     * @return background executor
     */
    public static ExecutorService backgroundExecutor() {
        return ForkJoinPool.commonPool();
    }

    /**
     * Returns current wall-clock epoch time in milliseconds.
     *
     * @return current epoch milliseconds
     */
    public static long getMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Returns current monotonic high-resolution time in nanoseconds.
     *
     * @return nanoseconds timestamp
     */
    public static long getNanos() {
        return System.nanoTime();
    }
}
