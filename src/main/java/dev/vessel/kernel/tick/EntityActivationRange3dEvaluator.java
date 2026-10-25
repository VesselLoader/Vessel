package dev.vessel.kernel.tick;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 3D Spherical Entity Activation Range (EAR) Evaluator.
 * <p>
 * Replaces traditional 2D cylindrical EAR (which needlessly ticks mobs 100 blocks
 * underground in caves or high in the sky) with true 3D ellipsoidal activation spheres.
 * Drastically reduces CPU tick overhead on high-density servers.
 * </p>
 * <p>
 * All hot-path evaluations operate exclusively via primitive arithmetic without object allocations.
 * </p>
 * <p>
 * Directly resolves ERR-41 and ERR-42.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class EntityActivationRange3dEvaluator {

    private final EarCategoryConfig[] categoryConfigs;
    private final AtomicLong evaluatedCount;
    private final AtomicLong deactivatedCount;
    private final AtomicLong immuneCount;

    /**
     * Constructs a 3D EAR evaluator pre-configured with default category radii.
     */
    public EntityActivationRange3dEvaluator() {
        this.categoryConfigs = new EarCategoryConfig[EarEntityTypeCategory.values().length];
        this.evaluatedCount = new AtomicLong(0L);
        this.deactivatedCount = new AtomicLong(0L);
        this.immuneCount = new AtomicLong(0L);

        initDefaultConfig(EarEntityTypeCategory.MONSTER, 32.0, 20.0);
        initDefaultConfig(EarEntityTypeCategory.ANIMAL, 24.0, 16.0);
        initDefaultConfig(EarEntityTypeCategory.WATER_CREATURE, 16.0, 12.0);
        initDefaultConfig(EarEntityTypeCategory.FLYING, 48.0, 32.0);
        initDefaultConfig(EarEntityTypeCategory.AMBIENT, 16.0, 12.0);
        initDefaultConfig(EarEntityTypeCategory.MISC, 16.0, 12.0);
        initDefaultConfig(EarEntityTypeCategory.VILLAGER, 32.0, 20.0);
        initDefaultConfig(EarEntityTypeCategory.RAIDER, 48.0, 32.0);
    }

    private void initDefaultConfig(EarEntityTypeCategory category, double horizontal, double vertical) {
        categoryConfigs[category.ordinal()] = new EarCategoryConfig(category, horizontal, vertical);
    }

    /**
     * Updates the activation configuration for a specific entity category.
     *
     * @param category entity category
     * @param horizontalRadius horizontal activation radius in blocks
     * @param verticalRadius vertical activation radius in blocks
     */
    public void setCategoryConfig(EarEntityTypeCategory category, double horizontalRadius, double verticalRadius) {
        Objects.requireNonNull(category, "category cannot be null");
        categoryConfigs[category.ordinal()] = new EarCategoryConfig(category, horizontalRadius, verticalRadius);
    }

    /**
     * Retrieves the active configuration for an entity category.
     *
     * @param category entity category
     * @return current category configuration
     */
    public EarCategoryConfig getCategoryConfig(EarEntityTypeCategory category) {
        Objects.requireNonNull(category, "category cannot be null");
        return categoryConfigs[category.ordinal()];
    }

    /**
     * Evaluates whether an entity should tick relative to a player position.
     * High-speed, zero-allocation hot-path method.
     *
     * @param category classification of the entity
     * @param entityX world X coordinate of entity
     * @param entityY world Y coordinate of entity
     * @param entityZ world Z coordinate of entity
     * @param playerX world X coordinate of closest player
     * @param playerY world Y coordinate of closest player
     * @param playerZ world Z coordinate of closest player
     * @param isImmune {@code true} if entity has active immunity (e.g. in water, taking damage, pathfinding)
     * @return {@code true} if entity is within active 3D sphere or immune; {@code false} if deactivated
     */
    public boolean shouldTick(
            EarEntityTypeCategory category,
            double entityX, double entityY, double entityZ,
            double playerX, double playerY, double playerZ,
            boolean isImmune
    ) {
        evaluatedCount.incrementAndGet();

        if (isImmune) {
            immuneCount.incrementAndGet();
            return true;
        }

        EarCategoryConfig config = categoryConfigs[category.ordinal()];
        double dx = entityX - playerX;
        double dy = entityY - playerY;
        double dz = entityZ - playerZ;

        double horizontalDistSq = dx * dx + dz * dz;
        double verticalDistSq = dy * dy;

        double hSq = config.horizontalRadiusSquared();
        double vSq = config.verticalRadiusSquared();

        // Ellipsoidal 3D sphere check: (hDistSq * vSq + vDistSq * hSq) <= (hSq * vSq)
        boolean insideSphere = (horizontalDistSq * vSq + verticalDistSq * hSq) <= (hSq * vSq);

        if (!insideSphere) {
            deactivatedCount.incrementAndGet();
            return false;
        }

        return true;
    }

    /**
     * Evaluates entity activation and produces an immutable {@link Ear3dEvaluationResult}.
     *
     * @param category classification of the entity
     * @param entityX world X coordinate of entity
     * @param entityY world Y coordinate of entity
     * @param entityZ world Z coordinate of entity
     * @param playerX world X coordinate of closest player
     * @param playerY world Y coordinate of closest player
     * @param playerZ world Z coordinate of closest player
     * @param isImmune {@code true} if entity is immune
     * @return evaluation result record
     */
    public Ear3dEvaluationResult evaluate(
            EarEntityTypeCategory category,
            double entityX, double entityY, double entityZ,
            double playerX, double playerY, double playerZ,
            boolean isImmune
    ) {
        EarCategoryConfig config = categoryConfigs[category.ordinal()];
        double dx = entityX - playerX;
        double dy = entityY - playerY;
        double dz = entityZ - playerZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        boolean active = shouldTick(category, entityX, entityY, entityZ, playerX, playerY, playerZ, isImmune);
        return new Ear3dEvaluationResult(active, isImmune, distSq, category);
    }

    /**
     * Returns the lifetime count of entity activation evaluations.
     *
     * @return evaluated queries count
     */
    public long evaluatedCount() {
        return evaluatedCount.get();
    }

    /**
     * Returns the lifetime count of entities throttled/deactivated by 3D EAR.
     *
     * @return deactivated entities count
     */
    public long deactivatedCount() {
        return deactivatedCount.get();
    }

    /**
     * Returns the lifetime count of entities granted ticks due to immunity.
     *
     * @return immune ticks count
     */
    public long immuneCount() {
        return immuneCount.get();
    }
}
