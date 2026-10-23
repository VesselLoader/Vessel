package dev.vessel.kernel.registry;

import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-phase pre-flight registry collision scanner implementing Architectural Decisions 8, 17, 20, and 31.
 *
 * <p>Combines Phase 1 Static Bytecode Symbol Scanning with Phase 2 Isolated Dry-Run execution
 * for imperative registration APIs ({@code DeferredRegister}, {@code CriteriaTriggers#register},
 * {@code CraftingHelper#register}, {@code EntitySelectorManager#register}, and
 * {@code ArgumentTypeInfos#registerByClass}). Triggers a Hard Stop with ANSI red diagnostics
 * naming both conflicting modules and JAR paths when duplicate keys are claimed in the same registry.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-28)
 */
public final class PreFlightRegistryCollisionScanner {

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static final ThreadLocal<Set<String>> ACTIVE_DRY_RUN_COLLECTOR = new ThreadLocal<>();

    private final Map<String, VesselModuleIdentifier> firstOwnerByRegistryKey = new ConcurrentHashMap<>();
    private final Map<VesselModuleIdentifier, String> moduleJarPaths = new ConcurrentHashMap<>();
    private final List<String> detectedCollisionErrors = Collections.synchronizedList(new ArrayList<>());

    /**
     * Records static registry keys discovered during Phase 1 bytecode symbol scanning.
     *
     * @param module  module owning the scanned class
     * @param symbols scanned class symbols
     */
    public void recordStaticRegistryKeys(VesselModuleIdentifier module, ScannedClassSymbols symbols) {
        Objects.requireNonNull(module, "module cannot be null");
        Objects.requireNonNull(symbols, "symbols cannot be null");

        for (String entrypointOrKey : symbols.entrypoints()) {
            if (entrypointOrKey.contains(":")) {
                claimRegistryKey(module, entrypointOrKey);
            }
        }
    }

    /**
     * Determines whether a scanned class requires Phase 2 isolated dry-run registration execution
     * (Decisions 17 &amp; 31).
     *
     * @param symbols scanned class symbols
     * @return {@code true} if imperative or deferred registration hooks are present
     */
    public boolean requiresIsolatedDryRun(ScannedClassSymbols symbols) {
        Objects.requireNonNull(symbols, "symbols cannot be null");

        for (String marker : symbols.entrypoints()) {
            if (isDynamicRegistrationMarker(marker)) {
                return true;
            }
        }
        for (String ann : symbols.annotations()) {
            if (isDynamicRegistrationMarker(ann)) {
                return true;
            }
        }
        for (String iface : symbols.interfaces()) {
            if (isDynamicRegistrationMarker(iface)) {
                return true;
            }
        }
        return isDynamicRegistrationMarker(symbols.className());
    }

    /**
     * Executes a module's registration entrypoint inside an isolated dry-run capture scope and
     * records all claimed registry keys.
     *
     * @param module                 module being evaluated
     * @param registrationEntrypoint runnable registration hook
     * @return unmodifiable set of registry keys claimed during the dry run
     */
    public Set<String> executeIsolatedRegistrationDryRun(
            VesselModuleIdentifier module,
            Runnable registrationEntrypoint
    ) {
        Objects.requireNonNull(module, "module cannot be null");
        Objects.requireNonNull(registrationEntrypoint, "registrationEntrypoint cannot be null");

        Set<String> capturedKeys = new LinkedHashSet<>();
        ACTIVE_DRY_RUN_COLLECTOR.set(capturedKeys);
        try {
            registrationEntrypoint.run();
        } finally {
            ACTIVE_DRY_RUN_COLLECTOR.remove();
        }

        for (String key : capturedKeys) {
            claimRegistryKey(module, key);
        }
        return Set.copyOf(capturedKeys);
    }

    /**
     * Verifies that zero duplicate registry keys were claimed across all modules.
     * If any collision occurred, throws an {@link IllegalStateException} containing the formatted
     * ANSI red Hard Stop report naming both modules and their JAR paths.
     *
     * @return empty list when zero collisions exist
     */
    public List<String> verifyZeroDuplicateKeysOrThrow() {
        if (!detectedCollisionErrors.isEmpty()) {
            List<String> snapshot = List.copyOf(detectedCollisionErrors);
            throw new IllegalStateException(String.join("\n", snapshot));
        }
        return List.of();
    }

    /**
     * Records a registry key during an active isolated dry-run callback.
     *
     * @param registryAndKey canonical key formatted as {@code registryName#namespace:path} or {@code namespace:path}
     */
    public static void recordDryRunKey(String registryAndKey) {
        Objects.requireNonNull(registryAndKey, "registryAndKey cannot be null");
        Set<String> collector = ACTIVE_DRY_RUN_COLLECTOR.get();
        if (collector != null) {
            collector.add(registryAndKey);
        }
    }

    /**
     * Associates a module identifier with its physical JAR path for diagnostic reporting.
     *
     * @param module  module identifier
     * @param jarPath physical JAR path
     */
    public void registerModuleJarPath(VesselModuleIdentifier module, String jarPath) {
        Objects.requireNonNull(module, "module cannot be null");
        Objects.requireNonNull(jarPath, "jarPath cannot be null");
        moduleJarPaths.put(module, jarPath);
    }

    private void claimRegistryKey(VesselModuleIdentifier module, String rawRegistryKey) {
        VesselModuleIdentifier existingOwner = firstOwnerByRegistryKey.putIfAbsent(rawRegistryKey, module);
        if (existingOwner != null && !existingOwner.equals(module)) {
            String firstJar = moduleJarPaths.getOrDefault(
                    existingOwner,
                    "mods/" + existingOwner.namespace() + ".jar"
            );
            String secondJar = moduleJarPaths.getOrDefault(
                    module,
                    "mods/" + module.namespace() + ".jar"
            );
            String redMessage = ANSI_RED
                    + "[VESSEL HARD STOP] Duplicate Registry Key Collision on '" + rawRegistryKey + "': "
                    + "Claimed by (" + existingOwner.loader() + ", " + existingOwner.namespace() + ") [" + firstJar + "] "
                    + "AND (" + module.loader() + ", " + module.namespace() + ") [" + secondJar + "]"
                    + ANSI_RESET;
            detectedCollisionErrors.add(redMessage);
        }
    }

    private static boolean isDynamicRegistrationMarker(String symbol) {
        if (symbol == null) {
            return false;
        }
        return symbol.contains("DeferredRegister")
                || symbol.contains("CriteriaTriggers")
                || symbol.contains("CraftingHelper")
                || symbol.contains("EntitySelectorManager")
                || symbol.contains("ArgumentTypeInfos");
    }
}
