package dev.vessel.kernel.gui;

import java.util.Objects;

/**
 * 20 Hz (20 ticks/sec) animation ticker for Action Bar resource gauges, spell cast indicators,
 * and Mana bars using high-precision Unicode font glyphs with smooth color pulsing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FontGlyphActionbarAnimationTicker {

    public static final int DEFAULT_SEGMENT_COUNT = 10;
    public static final float PULSE_FREQUENCY_RADIANS_PER_TICK = 0.314159f; // ~0.1 Hz full pulse cycle
    public static final float DANGER_THRESHOLD = 0.20f;

    private final String gaugeId;
    private final int totalSegments;
    private final ActionBarGlyphTheme theme;

    private float currentVal;
    private float maxVal;
    private float pulsePhase;
    private int currentTick;

    /**
     * Constructs a FontGlyphActionbarAnimationTicker with default mana theme.
     *
     * @param gaugeId unique identifier for this actionbar gauge
     */
    public FontGlyphActionbarAnimationTicker(String gaugeId) {
        this(gaugeId, DEFAULT_SEGMENT_COUNT, ActionBarGlyphTheme.DEFAULT_MANA_THEME);
    }

    /**
     * Constructs a FontGlyphActionbarAnimationTicker with custom configuration.
     *
     * @param gaugeId unique identifier for this actionbar gauge
     * @param totalSegments number of discrete segments on the actionbar
     * @param theme visual glyph theme
     */
    public FontGlyphActionbarAnimationTicker(String gaugeId, int totalSegments, ActionBarGlyphTheme theme) {
        this.gaugeId = Objects.requireNonNull(gaugeId, "gaugeId cannot be null");
        if (totalSegments <= 0) {
            throw new IllegalArgumentException("totalSegments must be positive: " + totalSegments);
        }
        this.totalSegments = totalSegments;
        this.theme = Objects.requireNonNull(theme, "theme cannot be null");
        this.maxVal = 100.0f;
        this.currentVal = 100.0f;
    }

    /**
     * Updates current gauge values.
     *
     * @param current current resource value
     * @param max maximum resource capacity
     */
    public synchronized void setValues(float current, float max) {
        if (max <= 0.0f) {
            throw new IllegalArgumentException("max must be positive: " + max);
        }
        this.currentVal = Math.clamp(current, 0.0f, max);
        this.maxVal = max;
    }

    /**
     * Advances the animation by one server tick (50ms / 20 Hz).
     *
     * @return updated pulse state snapshot
     */
    public synchronized ActionBarPulseState tick() {
        currentTick++;
        pulsePhase += PULSE_FREQUENCY_RADIANS_PER_TICK;
        if (pulsePhase >= Math.PI * 2.0) {
            pulsePhase -= (float) (Math.PI * 2.0);
        }

        int filledSegments = (int) Math.floor((currentVal / maxVal) * totalSegments);
        return new ActionBarPulseState(
                gaugeId,
                currentVal,
                maxVal,
                pulsePhase,
                Math.clamp(filledSegments, 0, totalSegments)
        );
    }

    /**
     * Composes and renders the Minecraft Actionbar Raw JSON text packet payload.
     *
     * @return valid Minecraft Raw JSON text component string
     */
    public synchronized String renderJson() {
        float ratio = currentVal / maxVal;
        boolean inDanger = ratio <= DANGER_THRESHOLD;

        // Calculate pulsing color variation: lerp between primary and accent or danger
        float pulseFactor = (float) (0.5 * (1.0 + Math.sin(pulsePhase)));
        int activeColor = inDanger
                ? interpolateColor(theme.dangerRgb(), theme.accentRgb(), pulseFactor)
                : interpolateColor(theme.primaryRgb(), theme.accentRgb(), pulseFactor);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"text\":\"\",\"extra\":[");

        // Prefix title
        sb.append("{\"text\":\"[").append(gaugeId).append("] \",\"color\":\"#AAAAAA\"},");

        int activeCount = Math.round(ratio * totalSegments);

        for (int i = 0; i < totalSegments; i++) {
            if (i > 0) {
                sb.append(",");
            }
            if (i < activeCount - 1) {
                // Fully solid segment
                sb.append("{\"text\":\"").append(theme.filledGlyph())
                        .append("\",\"color\":\"").append(String.format("#%06X", inDanger ? theme.dangerRgb() : theme.primaryRgb()))
                        .append("\"}");
            } else if (i == activeCount - 1) {
                // Pulsing head segment
                sb.append("{\"text\":\"").append(theme.chargingGlyph())
                        .append("\",\"color\":\"").append(String.format("#%06X", activeColor))
                        .append("\",\"bold\":true}");
            } else {
                // Empty segment
                sb.append("{\"text\":\"").append(theme.emptyGlyph())
                        .append("\",\"color\":\"#555555\"}");
            }
        }

        // Numeric percentage suffix
        int percent = Math.round(ratio * 100.0f);
        sb.append(String.format(",{\"text\":\" %d%%\",\"color\":\"#FFFFFF\"}", percent));
        sb.append("]}");

        return sb.toString();
    }

    private static int interpolateColor(int colorA, int colorB, float t) {
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int r = Math.clamp(Math.round(rA + (rB - rA) * t), 0, 255);
        int g = Math.clamp(Math.round(gA + (gB - gA) * t), 0, 255);
        int b = Math.clamp(Math.round(bA + (bB - bA) * t), 0, 255);

        return (r << 16) | (g << 8) | b;
    }

    public String getGaugeId() {
        return gaugeId;
    }

    public int getTotalSegments() {
        return totalSegments;
    }

    public ActionBarGlyphTheme getTheme() {
        return theme;
    }

    public synchronized float getCurrentVal() {
        return currentVal;
    }

    public synchronized float getMaxVal() {
        return maxVal;
    }

    public synchronized int getCurrentTick() {
        return currentTick;
    }
}
