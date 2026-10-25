package dev.vessel.kernel.vstm.engine;

import java.util.Objects;

/**
 * Bidirectional dialect routing coordinator connecting arbitrary mapping formats.
 *
 * <p>Enables instant cross-loader transmutation, for example:
 * <ul>
 *     <li>Fabric (Yarn / Intermediary) &rarr; Sovereign Vessel Canonical MojMap</li>
 *     <li>Sovereign MojMap &rarr; Spigot NMS (for Bukkit / Paper API compatibility)</li>
 *     <li>Forge / NeoForge (SRG / Official) &rarr; MojMap</li>
 * </ul>
 * </p>
 *
 * <p>Prevents ecosystem capability and event-bus mapping conflicts (Catalog refs: ECO-0001 to ECO-0050).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmDialectBridgeRouter {

    /**
     * Routes a class internal name from a source dialect to a target dialect.
     *
     * @param className     the source class internal name
     * @param sourceDialect the dialect of the source identifier
     * @param targetDialect the desired target dialect
     * @param lookup        the active fast lookup engine
     * @return transformed class internal name, or original if unchanged
     */
    public String routeClass(
            String className,
            int sourceDialect,
            int targetDialect,
            VstmTransmutationFastLookup lookup
    ) {
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(lookup, "lookup must not be null");

        if (sourceDialect == targetDialect) {
            return className;
        }

        // Direct lookup with source & target dialect mask
        String remapped = lookup.lookupClass(className, sourceDialect, targetDialect);
        if (remapped != null) {
            return remapped;
        }

        return className;
    }

    /**
     * Routes a method signature from a source dialect to a target dialect.
     *
     * @param ownerClass    declaring class internal name
     * @param methodName    method name
     * @param descriptor    method descriptor
     * @param sourceDialect source dialect bitmask
     * @param targetDialect target dialect bitmask
     * @param lookup        fast lookup engine
     * @param fallbackChain fallback resolution coordinator
     * @return remapped method name
     */
    public String routeMethod(
            String ownerClass,
            String methodName,
            String descriptor,
            int sourceDialect,
            int targetDialect,
            VstmTransmutationFastLookup lookup,
            VstmResolutionFallbackChain fallbackChain
    ) {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(lookup, "lookup must not be null");
        Objects.requireNonNull(fallbackChain, "fallbackChain must not be null");

        if (sourceDialect == targetDialect) {
            return methodName;
        }

        String remapped = lookup.lookupMethod(ownerClass, methodName, descriptor, targetDialect);
        if (remapped != null) {
            return remapped;
        }

        return fallbackChain.resolveFallbackMethod(ownerClass, methodName, descriptor, targetDialect, lookup);
    }

    /**
     * Routes a field access from a source dialect to a target dialect.
     *
     * @param ownerClass    declaring class internal name
     * @param fieldName     field name
     * @param descriptor    field descriptor
     * @param sourceDialect source dialect bitmask
     * @param targetDialect target dialect bitmask
     * @param lookup        fast lookup engine
     * @param fallbackChain fallback resolution coordinator
     * @return remapped field name
     */
    public String routeField(
            String ownerClass,
            String fieldName,
            String descriptor,
            int sourceDialect,
            int targetDialect,
            VstmTransmutationFastLookup lookup,
            VstmResolutionFallbackChain fallbackChain
    ) {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(lookup, "lookup must not be null");
        Objects.requireNonNull(fallbackChain, "fallbackChain must not be null");

        if (sourceDialect == targetDialect) {
            return fieldName;
        }

        String remapped = lookup.lookupField(ownerClass, fieldName, descriptor, targetDialect);
        if (remapped != null) {
            return remapped;
        }

        return fallbackChain.resolveFallbackField(ownerClass, fieldName, descriptor, targetDialect, lookup);
    }
}
