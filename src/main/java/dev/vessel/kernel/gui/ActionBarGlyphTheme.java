package dev.vessel.kernel.gui;

import java.util.Objects;

/**
 * Immutable visual theme definition for actionbar resource bar animations.
 * Specifies Unicode glyph icons and RGB color palettes for filled, charging, and empty states.
 *
 * @param filledGlyph Unicode character representing fully charged segments
 * @param chargingGlyph Unicode character representing active pulsing segments
 * @param emptyGlyph Unicode character representing uncharged/empty segments
 * @param primaryRgb primary gauge color hex
 * @param accentRgb pulsing accent color hex
 * @param dangerRgb low resource warning color hex
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ActionBarGlyphTheme(
        String filledGlyph,
        String chargingGlyph,
        String emptyGlyph,
        int primaryRgb,
        int accentRgb,
        int dangerRgb
) {
    public ActionBarGlyphTheme {
        Objects.requireNonNull(filledGlyph, "filledGlyph cannot be null");
        Objects.requireNonNull(chargingGlyph, "chargingGlyph cannot be null");
        Objects.requireNonNull(emptyGlyph, "emptyGlyph cannot be null");
    }

    /**
     * Default magical mana bar theme.
     */
    public static final ActionBarGlyphTheme DEFAULT_MANA_THEME = new ActionBarGlyphTheme(
            "\uE101", // Solid mana orb
            "\uE102", // Pulsing mana spark
            "\uE103", // Hollow mana orb
            0x268BD2, // Arcane Cyan
            0x6C71C4, // Violet pulse
            0xDC322F  // Crimson critical
    );
}
