package dev.vessel.kernel.modelengine.hitbox;

/**
 * Enumeration of hitbox simulation types supported by Model Engine on VESSEL.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum HitboxType {

    /**
     * Modern (1.19.4+) Minecraft Interaction entity ({@code net.minecraft.world.entity.Interaction}).
     * Native adjustable width and height, fully rendered as bounding boxes in client F3 + B debug mode.
     */
    INTERACTION,

    /**
     * Legacy compound Slime entity hitbox with dynamic size scaling.
     */
    LEGACY_SLIME,

    /**
     * Legacy compound ArmorStand hitbox.
     */
    LEGACY_ARMOR_STAND
}
