package dev.vessel.kernel.access;

/**
 * Standardized access visibility level across Forge Access Transformers and Fabric Access Wideners.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum CanonicalAccessLevel {

    /**
     * JVM public access (ACC_PUBLIC).
     */
    PUBLIC,

    /**
     * JVM protected access (ACC_PROTECTED).
     */
    PROTECTED,

    /**
     * JVM package-private (default) access.
     */
    PACKAGE_PRIVATE,

    /**
     * JVM private access (ACC_PRIVATE).
     */
    PRIVATE
}
