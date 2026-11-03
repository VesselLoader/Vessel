package net.fabricmc.fabric.api.biome.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Canonical Fabric Biome API v1 worldgen biome feature injector
 * ({@code net.fabricmc.fabric.api.biome.v1.BiomeModifications}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BiomeModifications {

    private static final List<String> APPLIED_FEATURES = new CopyOnWriteArrayList<>();
    private static final List<Predicate<BiomeSelectionContext>> SELECTORS = new CopyOnWriteArrayList<>();
    private static final List<String> FEATURE_KEYS = new CopyOnWriteArrayList<>();

    private BiomeModifications() {
    }

    /**
     * Registers a worldgen placed feature addition for all biomes matching {@code biomeSelector}.
     *
     * @param biomeSelector    non-null biome selection predicate
     * @param stepName         non-null generation step name (e.g., {@code "UNDERGROUND_ORES"})
     * @param placedFeatureKey non-null placed feature registry key
     */
    public static void addFeature(
            Predicate<BiomeSelectionContext> biomeSelector,
            String stepName,
            String placedFeatureKey) {
        Objects.requireNonNull(biomeSelector, "biomeSelector must not be null");
        Objects.requireNonNull(stepName, "stepName must not be null");
        Objects.requireNonNull(placedFeatureKey, "placedFeatureKey must not be null");
        SELECTORS.add(biomeSelector);
        FEATURE_KEYS.add(stepName + ":" + placedFeatureKey);
    }

    /**
     * Evaluates all registered biome feature selectors against the given {@link BiomeSelectionContext}
     * and records matched features in {@link #getAppliedFeatures()}.
     *
     * @param context non-null biome selection context
     * @return unmodifiable list of {@code stepName:placedFeatureKey} entries matching {@code context}
     */
    public static List<String> evaluateForBiome(BiomeSelectionContext context) {
        Objects.requireNonNull(context, "context must not be null");
        List<String> matched = new ArrayList<>();
        int count = Math.min(SELECTORS.size(), FEATURE_KEYS.size());
        for (int i = 0; i < count; i++) {
            Predicate<BiomeSelectionContext> selector = SELECTORS.get(i);
            String featureEntry = FEATURE_KEYS.get(i);
            if (selector.test(context)) {
                matched.add(featureEntry);
                APPLIED_FEATURES.add(featureEntry);
            }
        }
        return Collections.unmodifiableList(matched);
    }

    /**
     * Returns an unmodifiable list of all feature entries applied across evaluated biomes.
     *
     * @return unmodifiable list of applied feature keys
     */
    public static List<String> getAppliedFeatures() {
        return Collections.unmodifiableList(APPLIED_FEATURES);
    }

    /**
     * Clears all registered selectors and applied biome feature records.
     */
    public static void clearAll() {
        APPLIED_FEATURES.clear();
        SELECTORS.clear();
        FEATURE_KEYS.clear();
    }
}
