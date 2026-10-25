package dev.vessel.kernel.modelengine.driver;

import java.util.List;
import java.util.Objects;

/**
 * Controller orchestrating the driver mob's lifecycle, health management,
 * invisibility enforcement, and custom drops.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DriverMobController {

    private final DriverMobState state;
    private final DriverHealthScaler healthScaler;
    private final DriverInvisibilityGovernor invisibilityGovernor;
    private final DriverDropSuppressor dropSuppressor;

    /**
     * Constructs a DriverMobController with the specified components.
     *
     * @param state                driver mob state
     * @param healthScaler         health scaler
     * @param invisibilityGovernor invisibility and silence governor
     * @param dropSuppressor       drop suppressor
     */
    public DriverMobController(
            DriverMobState state,
            DriverHealthScaler healthScaler,
            DriverInvisibilityGovernor invisibilityGovernor,
            DriverDropSuppressor dropSuppressor
    ) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.healthScaler = Objects.requireNonNull(healthScaler, "healthScaler cannot be null");
        this.invisibilityGovernor = Objects.requireNonNull(invisibilityGovernor, "invisibilityGovernor cannot be null");
        this.dropSuppressor = Objects.requireNonNull(dropSuppressor, "dropSuppressor cannot be null");

        // Initial setup
        this.invisibilityGovernor.governDriver(this.state);
        this.healthScaler.configureDriverHealth(this.state);
    }

    /**
     * Executes periodic controller logic, maintaining invisibility flags and AI state.
     */
    public void tick() {
        if (!state.isAlive()) {
            return;
        }
        invisibilityGovernor.governDriver(state);
    }

    /**
     * Applies damage to the driver entity, checking for death.
     *
     * @param damage damage amount
     * @return true if damage was applied and entity remains alive, false if fatal
     */
    public boolean applyDamage(float damage) {
        if (!state.isAlive() || damage <= 0.0f) {
            return state.isAlive();
        }
        float newHealth = state.getCurrentHealth() - damage;
        state.setCurrentHealth(newHealth);
        return state.isAlive();
    }

    /**
     * Heals the driver entity up to its maximum health.
     *
     * @param amount healing amount
     */
    public void heal(float amount) {
        if (!state.isAlive() || amount <= 0.0f) {
            return;
        }
        state.setCurrentHealth(state.getCurrentHealth() + amount);
    }

    /**
     * Handles mob death, computing custom dropped items if configured.
     *
     * @param lootingLevel looting enchantment level
     * @return list of custom drops produced
     */
    public List<DroppedItemEntry> onDeath(int lootingLevel) {
        state.setAlive(false);
        return dropSuppressor.generateCustomDrops(lootingLevel);
    }

    public boolean isAlive() {
        return state.isAlive();
    }

    public DriverMobState getState() {
        return state;
    }

    public DriverHealthScaler getHealthScaler() {
        return healthScaler;
    }

    public DriverInvisibilityGovernor getInvisibilityGovernor() {
        return invisibilityGovernor;
    }

    public DriverDropSuppressor getDropSuppressor() {
        return dropSuppressor;
    }
}
