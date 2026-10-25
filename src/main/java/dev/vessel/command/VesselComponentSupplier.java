package dev.vessel.command;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Top-level {@link Supplier} implementation supplying a pre-computed Minecraft chat Component
 * to {@code CommandSourceStack#sendSuccess(Supplier, boolean)}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselComponentSupplier implements Supplier<Object> {

    private final Object component;

    public VesselComponentSupplier(Object component) {
        this.component = Objects.requireNonNull(component, "component must not be null");
    }

    @Override
    public Object get() {
        return component;
    }
}
