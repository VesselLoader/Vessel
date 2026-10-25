package dev.vessel.kernel.modelengine.hitbox;

import java.util.Objects;

/**
 * Immutable outcome of a localized attack relayed from a dynamic hitbox to the root driver mob.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param hitboxId            identifier of the struck hitbox (e.g. "head", "body", "tail")
 * @param rawDamage           initial damage received from attacker
 * @param localizedMultiplier damage multiplier applied to this body part (e.g. 2.0x for headshot)
 * @param finalDamage         damage applied to root mob (rawDamage * localizedMultiplier)
 * @param remainingHealth     health of driver mob after damage application
 * @param fatal               whether this attack was fatal (mob health &lt;= 0)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DamageRelayResult(
        String hitboxId,
        float rawDamage,
        float localizedMultiplier,
        float finalDamage,
        float remainingHealth,
        boolean fatal
) {
    /**
     * Constructs a DamageRelayResult with validation.
     */
    public DamageRelayResult {
        Objects.requireNonNull(hitboxId, "hitboxId cannot be null");
    }
}
