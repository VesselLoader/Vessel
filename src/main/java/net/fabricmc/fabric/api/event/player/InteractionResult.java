package net.fabricmc.fabric.api.event.player;

/**
 * Canonical Fabric Event Player API interaction outcome enum
 * ({@code net.fabricmc.fabric.api.event.player.InteractionResult}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum InteractionResult {
    /**
     * Interaction succeeded and consumed the action (with arm swing).
     */
    SUCCESS(true),

    /**
     * Interaction succeeded and consumed the action without additional visual effect.
     */
    CONSUME(true),

    /**
     * Listener declined to handle the interaction; pass to the next listener or vanilla logic.
     */
    PASS(false),

    /**
     * Interaction was explicitly cancelled/rejected.
     */
    FAIL(false);

    private final boolean consumesAction;

    InteractionResult(boolean consumesAction) {
        this.consumesAction = consumesAction;
    }

    /**
     * Returns whether this result consumes the player action.
     *
     * @return {@code true} if the action was consumed
     */
    public boolean consumesAction() {
        return consumesAction;
    }
}
