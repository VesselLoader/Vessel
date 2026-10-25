package dev.vessel.kernel.command;

/**
 * Minecraft boss bar segmentation overlay patterns.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum BossBarOverlay {

    /**
     * Continuous unbroken progress bar.
     */
    PROGRESS,

    /**
     * Bar divided into 6 segments.
     */
    NOTCHED_6,

    /**
     * Bar divided into 10 segments.
     */
    NOTCHED_10,

    /**
     * Bar divided into 12 segments.
     */
    NOTCHED_12,

    /**
     * Bar divided into 20 segments.
     */
    NOTCHED_20
}
