package dev.vessel.kernel.modelengine.hitbox;

import dev.vessel.kernel.modelengine.driver.DriverMobController;

import java.util.Objects;

/**
 * Relays player right-click interaction events from a dynamic hitbox to the root driver mob
 * or custom interaction handlers (e.g. mounting, feeding, shearing, trading).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class HitboxInteractionRelay {

    /**
     * Constructs a HitboxInteractionRelay.
     */
    public HitboxInteractionRelay() {
    }

    /**
     * Processes an interaction against a dynamic hitbox and constructs the corresponding interaction event.
     *
     * @param hitbox     interacted dynamic hitbox
     * @param controller driver mob controller
     * @param playerId   player entity ID
     * @param isMainHand whether main hand was used
     * @param clickX     click position X
     * @param clickY     click position Y
     * @param clickZ     click position Z
     * @return interaction event record
     */
    public HitboxInteractionEvent relayInteraction(
            DynamicHitbox hitbox,
            DriverMobController controller,
            int playerId,
            boolean isMainHand,
            double clickX,
            double clickY,
            double clickZ
    ) {
        Objects.requireNonNull(hitbox, "hitbox cannot be null");
        Objects.requireNonNull(controller, "controller cannot be null");

        return new HitboxInteractionEvent(
                hitbox.getHitboxId(),
                playerId,
                isMainHand,
                clickX,
                clickY,
                clickZ
        );
    }
}
