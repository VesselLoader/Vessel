package dev.vessel.kernel.modelengine.hitbox;

import java.util.Objects;

/**
 * Immutable event describing a player interaction with a dynamic hitbox.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param hitboxId   identifier of the interacted hitbox
 * @param playerId   Minecraft entity ID of the interacting player
 * @param isMainHand true if main hand, false if off hand
 * @param clickX     interaction coordinate X
 * @param clickY     interaction coordinate Y
 * @param clickZ     interaction coordinate Z
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record HitboxInteractionEvent(
        String hitboxId,
        int playerId,
        boolean isMainHand,
        double clickX,
        double clickY,
        double clickZ
) {
    /**
     * Constructs a HitboxInteractionEvent with validation.
     */
    public HitboxInteractionEvent {
        Objects.requireNonNull(hitboxId, "hitboxId cannot be null");
    }
}
