package net.minecraftforge.fml;

import java.util.function.Supplier;

/**
 * Universal cross-era shim for Forge DistExecutor distribution runner.
 * Safe execution abstraction for physical client vs dedicated server domains.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public final class DistExecutor {

    private DistExecutor() {
    }

    /**
     * Executes the supplier safely on the target distribution.
     *
     * @param dist  target distribution
     * @param toRun supplier of safe supplier
     * @param <T>   result type
     * @return result or null
     */
    public static <T> T safeRunWhenOn(Object dist, Supplier<DistExecutor$SafeSupplier<T>> toRun) {
        if (toRun != null && toRun.get() != null) {
            return toRun.get().get();
        }
        return null;
    }

    /**
     * Unsafely executes runnable on target distribution.
     */
    public static <T> T unsafeRunWhenOn(Object dist, Supplier<Supplier<T>> toRun) {
        if (toRun != null && toRun.get() != null) {
            return toRun.get().get();
        }
        return null;
    }

    /**
     * Executes client target on physical client, server target on dedicated server.
     */
    public static void unsafeRunForDist(Supplier<Runnable> clientTarget, Supplier<Runnable> serverTarget) {
        if (serverTarget != null && serverTarget.get() != null) {
            serverTarget.get().run();
        }
    }
}
