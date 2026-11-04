package net.minecraftforge.fml;

import java.util.function.Supplier;

/**
 * Universal cross-era shim for Forge DistExecutor$SafeSupplier.
 * Prevents classloading of client-only code on dedicated server runtime.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @param <T> supplied value type
 * @version 1.0.0
 */
@FunctionalInterface
public interface DistExecutor$SafeSupplier<T> extends Supplier<T> {
}
