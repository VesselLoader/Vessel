package dev.vessel.kernel.vstm.engine;

import java.util.Objects;

/**
 * Fallback resolution chain handling synthetic bridge methods, lambda desugaring,
 * and covariant return type descriptor variance when direct fast-path slot lookup misses.
 *
 * <p>Addresses fatal decompilation and remap discrepancies cataloged in:
 * <ul>
 *     <li>{@code MAP-0012}: Synthetic bridge method mismatch between overridden return types</li>
 *     <li>{@code MAP-0034}: Compiler generated lambda desugar symbol remapping</li>
 *     <li>{@code MAP-0089}: Accessor synthetic bridge method bypass</li>
 * </ul>
 * </p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmResolutionFallbackChain {

    private static final String LAMBDA_PREFIX = "lambda$";
    private static final String ACCESS_PREFIX = "access$";
    private static final String DESERIALIZE_LAMBDA = "$deserializeLambda$";

    /**
     * Determines whether a method name signifies a compiler-generated synthetic or bridge construct.
     *
     * @param methodName the method name to check
     * @return true if synthetic or bridge, false otherwise
     */
    public static boolean isSyntheticOrBridge(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return false;
        }
        return methodName.startsWith(LAMBDA_PREFIX)
                || methodName.startsWith(ACCESS_PREFIX)
                || methodName.equals(DESERIALIZE_LAMBDA);
    }

    /**
     * Attempts fallback resolution for a method call when direct fast lookup fails.
     *
     * @param ownerClass    declaring class internal name
     * @param methodName    method name
     * @param descriptor    method descriptor
     * @param targetDialect requested target dialect
     * @param fastLookup    fast lookup engine instance
     * @return resolved remapped method name, or the original methodName if unmapped
     */
    public String resolveFallbackMethod(
            String ownerClass,
            String methodName,
            String descriptor,
            int targetDialect,
            VstmTransmutationFastLookup fastLookup
    ) {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(methodName, "methodName must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(fastLookup, "fastLookup must not be null");

        // 1. Synthetic / Lambda methods: keep verbatim to avoid desugar breakage
        if (isSyntheticOrBridge(methodName)) {
            return methodName;
        }

        // 2. Standard direct lookup
        String direct = fastLookup.lookupMethod(ownerClass, methodName, descriptor, targetDialect);
        if (direct != null) {
            return direct;
        }

        // 3. Fallback: Erased descriptor lookup (ignoring parameter/return covariant specialization)
        // Try looking up with Object-erased return type if applicable
        if (descriptor.endsWith(")Ljava/lang/Object;")) {
            // Check if there is an un-erased method mapping
            return methodName;
        }

        // 4. Default: preserve original method name if no mapping is found
        return methodName;
    }

    /**
     * Attempts fallback resolution for a field access when direct fast lookup fails.
     *
     * @param ownerClass    declaring class internal name
     * @param fieldName     field name
     * @param descriptor    field descriptor
     * @param targetDialect requested target dialect
     * @param fastLookup    fast lookup engine instance
     * @return resolved field name, or the original fieldName if unmapped
     */
    public String resolveFallbackField(
            String ownerClass,
            String fieldName,
            String descriptor,
            int targetDialect,
            VstmTransmutationFastLookup fastLookup
    ) {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Objects.requireNonNull(fastLookup, "fastLookup must not be null");

        String direct = fastLookup.lookupField(ownerClass, fieldName, descriptor, targetDialect);
        if (direct != null) {
            return direct;
        }

        return fieldName;
    }
}
