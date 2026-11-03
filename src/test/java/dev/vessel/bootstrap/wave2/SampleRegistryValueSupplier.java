package dev.vessel.bootstrap.wave2;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Standalone {@link Supplier} fixture used in Wave 2 tests to supply deferred registry values
 * without anonymous or inner classes (strict Rule 17 compliance).
 *
 * @version 1.0.0
 */
public final class SampleRegistryValueSupplier implements Supplier<String> {

    private final String value;

    public SampleRegistryValueSupplier(String value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public String get() {
        return value;
    }
}
