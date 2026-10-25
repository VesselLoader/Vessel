package dev.vessel.kernel.world.worldgen;

import java.util.function.Supplier;

/**
 * Factory supplier producing {@link CascadingThreadDepthTracker} instances for thread-local initialization.
 * <p>
 * Ensures zero anonymous class allocations and strict compliance with Rule 17.
 * </p>
 * <p>
 * Directly resolves ERR-33 and WGN-0001..0250.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CascadingThreadDepthTrackerSupplier implements Supplier<CascadingThreadDepthTracker> {

    /**
     * Singleton supplier instance.
     */
    public static final CascadingThreadDepthTrackerSupplier INSTANCE = new CascadingThreadDepthTrackerSupplier();

    @Override
    public CascadingThreadDepthTracker get() {
        return new CascadingThreadDepthTracker();
    }
}
