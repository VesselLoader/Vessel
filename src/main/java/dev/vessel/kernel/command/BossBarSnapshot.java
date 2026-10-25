package dev.vessel.kernel.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot record capturing the full visual state of a BossBar.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param barId                unique bar UUID
 * @param title                display title string
 * @param progress             normalized progress value [0.0..1.0]
 * @param color                visual color style
 * @param overlay              notch segmentation overlay
 * @param darkenScreen         whether sky darkness is applied
 * @param playBossMusic        whether boss music is played
 * @param createWorldFog       whether atmospheric fog is generated
 * @param visible              whether bar is rendered on client HUD
 * @param updateTimestampNanos high-resolution timestamp of last update
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record BossBarSnapshot(
        UUID barId,
        String title,
        float progress,
        BossBarColor color,
        BossBarOverlay overlay,
        boolean darkenScreen,
        boolean playBossMusic,
        boolean createWorldFog,
        boolean visible,
        long updateTimestampNanos
) {

    /**
     * Compact constructor clamping progress into valid [0.0, 1.0] range.
     */
    public BossBarSnapshot {
        Objects.requireNonNull(barId, "barId cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        Objects.requireNonNull(overlay, "overlay cannot be null");

        progress = Math.max(0.0f, Math.min(1.0f, progress));
    }
}
