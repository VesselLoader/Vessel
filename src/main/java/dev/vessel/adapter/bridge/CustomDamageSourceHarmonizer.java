package dev.vessel.adapter.bridge;

import java.util.Locale;
import java.util.Objects;

/**
 * Harmonizes custom modded {@code DamageSource} types (TACZ bullets, Iron's Spells magic,
 * Create crushing wheels, Mekanism radiation) into standard Bukkit {@code DamageCause}
 * constants while preserving rich modded death messages.
 *
 * @version 1.0.0
 */
public final class CustomDamageSourceHarmonizer {

    /**
     * Creates a new custom damage source harmonizer.
     */
    public CustomDamageSourceHarmonizer() {
    }

    /**
     * Maps a modded damage source identifier and its traits into a standard Bukkit
     * {@code EntityDamageEvent.DamageCause} constant name.
     *
     * @param modDamageId  modded damage source ID (e.g., {@code "tacz:bullet"}, {@code "create:crush"})
     * @param isProjectile whether the damage source is projectile-based
     * @param isExplosion  whether the damage source is explosion-based
     * @param isMagic      whether the damage source is magical
     * @return standard Bukkit {@code DamageCause} name
     */
    public String mapModDamageSourceToBukkitCause(
            String modDamageId, boolean isProjectile, boolean isExplosion, boolean isMagic) {
        Objects.requireNonNull(modDamageId, "modDamageId must not be null");
        if (isProjectile) {
            return "PROJECTILE";
        }
        if (isExplosion) {
            return "ENTITY_EXPLOSION";
        }
        if (isMagic) {
            return "MAGIC";
        }
        String normalized = modDamageId.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("crush") || normalized.contains("anvil")) {
            return "FALLING_BLOCK";
        }
        if (normalized.contains("radiation") || normalized.contains("acid")) {
            return "POISON";
        }
        if (normalized.contains("laser") || normalized.contains("fire") || normalized.contains("plasma")) {
            return "FIRE_TICK";
        }
        return "CUSTOM";
    }

    /**
     * Formats and preserves a modded death message without losing the custom damage source
     * or attacker attribution during Bukkit {@code PlayerDeathEvent} bridging.
     *
     * @param victimName   name of the slain player or entity
     * @param modDamageId  modded damage source identifier
     * @param attackerName name of the attacker or machine (may be null or empty)
     * @return formatted death message string
     */
    public String preserveCustomDeathMessage(
            String victimName, String modDamageId, String attackerName) {
        Objects.requireNonNull(victimName, "victimName must not be null");
        Objects.requireNonNull(modDamageId, "modDamageId must not be null");
        if (attackerName != null && !attackerName.isBlank()) {
            return victimName + " was eliminated by " + attackerName + " using [" + modDamageId + "]";
        }
        return victimName + " perished due to [" + modDamageId + "]";
    }
}
