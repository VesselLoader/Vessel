package org.bukkit.potion;

import java.util.Objects;

/**
 * Represents an active or template status effect instance applied to a living entity
 * ({@code org.bukkit.potion.PotionEffect}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PotionEffect {

    private final PotionEffectType type;
    private final int duration;
    private final int amplifier;
    private final boolean ambient;
    private final boolean particles;
    private final boolean icon;

    /**
     * Constructs a {@code PotionEffect} with ambient, particles, and icon enabled.
     *
     * @param type      non-null effect type
     * @param duration  duration in ticks
     * @param amplifier zero-based effect amplifier tier
     */
    public PotionEffect(PotionEffectType type, int duration, int amplifier) {
        this(type, duration, amplifier, true, true, true);
    }

    /**
     * Constructs a {@code PotionEffect} with explicit ambient and particle flags (`icon = particles`).
     *
     * @param type      non-null effect type
     * @param duration  duration in ticks
     * @param amplifier zero-based effect amplifier tier
     * @param ambient   whether this effect originates from an ambient beacon-like source
     * @param particles whether effect particles are rendered
     */
    public PotionEffect(
            PotionEffectType type,
            int duration,
            int amplifier,
            boolean ambient,
            boolean particles) {
        this(type, duration, amplifier, ambient, particles, particles);
    }

    /**
     * Constructs a {@code PotionEffect} with full display configuration.
     *
     * @param type      non-null effect type
     * @param duration  duration in ticks
     * @param amplifier zero-based effect amplifier tier
     * @param ambient   whether this effect originates from an ambient source
     * @param particles whether effect particles are rendered
     * @param icon      whether the HUD effect icon is displayed
     */
    public PotionEffect(
            PotionEffectType type,
            int duration,
            int amplifier,
            boolean ambient,
            boolean particles,
            boolean icon) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
        this.icon = icon;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getDuration() {
        return duration;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public boolean isAmbient() {
        return ambient;
    }

    public boolean hasParticles() {
        return particles;
    }

    public boolean hasIcon() {
        return icon;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PotionEffect other)) {
            return false;
        }
        return duration == other.duration
                && amplifier == other.amplifier
                && ambient == other.ambient
                && particles == other.particles
                && icon == other.icon
                && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, duration, amplifier, ambient, particles, icon);
    }

    @Override
    public String toString() {
        return type.getName() + ":{duration=" + duration + ", amplifier=" + amplifier + "}";
    }
}
