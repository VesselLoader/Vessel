package net.fabricmc.fabric.api.event.player;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Event Player API callback holder for using an item in air or on a target
 * ({@code net.fabricmc.fabric.api.event.player.UseItemCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class UseItemCallback {

    /**
     * Event fired when a player uses an item.
     */
    public static final Event<UseBlockHandler> EVENT =
            EventFactory.createArrayBacked(
                    UseBlockHandler.class,
                    UseItemCallback::createComposite);

    private UseItemCallback() {
    }

    private static UseBlockHandler createComposite(List<UseBlockHandler> handlers) {
        return (playerName, worldName, hand, hitPos) -> {
            for (UseBlockHandler h : handlers) {
                InteractionResult res = h.interact(playerName, worldName, hand, hitPos);
                if (res != InteractionResult.PASS) {
                    return res;
                }
            }
            return InteractionResult.PASS;
        };
    }
}
