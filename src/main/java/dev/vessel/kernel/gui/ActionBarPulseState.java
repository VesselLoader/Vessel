package dev.vessel.kernel.gui;

import java.util.Objects;

/**
 * Immutable snapshot representing actionbar animation and pulsing attributes.
 *
 * @param barTitle human-readable title or label of the actionbar gauge
 * @param currentMana current resource value (e.g. Mana or Cast Progress)
 * @param maxMana maximum resource capacity
 * @param pulsePhase phase of the sinusoidal pulse cycle [0.0, 2 * PI]
 * @param activeSegmentIndex current animated tick segment position
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ActionBarPulseState(
        String barTitle,
        float currentMana,
        float maxMana,
        float pulsePhase,
        int activeSegmentIndex
) {
    public ActionBarPulseState {
        Objects.requireNonNull(barTitle, "barTitle cannot be null");
        if (maxMana <= 0.0f) {
            throw new IllegalArgumentException("maxMana must be positive: " + maxMana);
        }
    }

    /**
     * Calculates the fill ratio between 0.0 and 1.0 clamped.
     */
    public float getFillRatio() {
        return Math.clamp(currentMana / maxMana, 0.0f, 1.0f);
    }
}
