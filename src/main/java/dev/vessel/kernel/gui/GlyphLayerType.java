package dev.vessel.kernel.gui;

/**
 * Rendering layer priority types for negative-space font glyph user interfaces.
 * Higher ordinal values render on top of lower ones to guarantee proper visual depth.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum GlyphLayerType {

    /**
     * Darkened screen tint, vignette, or full-screen canvas backdrop.
     */
    BACKGROUND(0),

    /**
     * Machine hull, chassis frames, borders, and panel textures.
     */
    MACHINERY_FRAME(10),

    /**
     * Interactive slot borders, fluid gauge tanks, and energy bars.
     */
    CONTAINER_SLOTS(20),

    /**
     * Modded custom icons, animated indicators, and recipe progress arrows.
     */
    FOREGROUND_ICON(30),

    /**
     * Floating contextual information, popovers, and interactive tooltips.
     */
    TOOLTIP(40),

    /**
     * Top-level modal notifications, confirmation dialogs, and alert badges.
     */
    MODAL_OVERLAY(50);

    private final int layerPriority;

    GlyphLayerType(int layerPriority) {
        this.layerPriority = layerPriority;
    }

    public int getLayerPriority() {
        return layerPriority;
    }
}
