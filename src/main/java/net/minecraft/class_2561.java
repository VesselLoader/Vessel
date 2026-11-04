package net.minecraft;

/**
 * Universal cross-era intermediary bridge shim for Fabric Component class (class_2561).
 * Bridges Fabric 1.20.1 Intermediary text component references into modern chat runtime.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface class_2561 {

    /**
     * Returns plain string representation.
     *
     * @return string value
     */
    default String getString() {
        return "";
    }
}
