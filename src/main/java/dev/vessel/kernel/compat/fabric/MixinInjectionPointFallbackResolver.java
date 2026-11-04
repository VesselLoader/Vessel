package dev.vessel.kernel.compat.fabric;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Resolves or safely bypasses missing Mixin injection points in modded bytecode to prevent fatal MixinApplyError halts.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class MixinInjectionPointFallbackResolver {

    private final ConcurrentMap<String, String> injectionPointAliases = new ConcurrentHashMap<>();
    private final Set<String> optionalInjectionPoints = ConcurrentHashMap.newKeySet();

    public void registerInjectionPointAlias(String legacyPoint, String modernPoint) {
        Objects.requireNonNull(legacyPoint, "legacyPoint cannot be null");
        Objects.requireNonNull(modernPoint, "modernPoint cannot be null");
        injectionPointAliases.put(legacyPoint, modernPoint);
    }

    public void markInjectionPointOptional(String injectionPointSignature) {
        Objects.requireNonNull(injectionPointSignature, "injectionPointSignature cannot be null");
        optionalInjectionPoints.add(injectionPointSignature);
    }

    public String resolveInjectionPoint(String requestedPoint) {
        if (requestedPoint == null) {
            return null;
        }
        return injectionPointAliases.getOrDefault(requestedPoint, requestedPoint);
    }

    public boolean canSafelySkipFailure(String injectionPointSignature) {
        if (injectionPointSignature == null) {
            return true;
        }
        return optionalInjectionPoints.contains(injectionPointSignature);
    }
}
