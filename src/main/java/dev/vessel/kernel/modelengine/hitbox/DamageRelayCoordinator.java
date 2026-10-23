package dev.vessel.kernel.modelengine.hitbox;

import dev.vessel.kernel.modelengine.driver.DriverMobController;

import java.util.Objects;

/**
 * Coordinates localized damage forwarding from dynamic sub-part hitboxes to the root driver mob.
 * Applies localized damage multipliers (e.g., 2.0x headshots, 1.0x torso, 0.75x limbs)
 * and relays final damage into the driver mob's health controller.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DamageRelayCoordinator {

    /**
     * Constructs a DamageRelayCoordinator.
     */
    public DamageRelayCoordinator() {
    }

    /**
     * Intercepts an attack against a dynamic hitbox, scales the damage with the localized multiplier,
     * and applies it directly to the driver mob.
     *
     * @param hitbox     struck dynamic hitbox
     * @param controller driver mob controller
     * @param rawDamage  incoming raw damage
     * @return outcome record containing localized multiplier and remaining health
     */
    public DamageRelayResult relayDamage(DynamicHitbox hitbox, DriverMobController controller, float rawDamage) {
        Objects.requireNonNull(hitbox, "hitbox cannot be null");
        Objects.requireNonNull(controller, "controller cannot be null");

        if (rawDamage < 0.0f) {
            throw new IllegalArgumentException("rawDamage cannot be negative: " + rawDamage);
        }

        float multiplier = hitbox.getDamageMultiplier();
        float finalDamage = rawDamage * multiplier;

        boolean alive = controller.applyDamage(finalDamage);
        float remainingHealth = controller.getState().getCurrentHealth();

        return new DamageRelayResult(
                hitbox.getHitboxId(),
                rawDamage,
                multiplier,
                finalDamage,
                remainingHealth,
                !alive
        );
    }
}
