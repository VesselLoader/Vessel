package net.fabricmc.fabric.api.event.player;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Event Player API callback holder for right-clicking a block
 * ({@code net.fabricmc.fabric.api.event.player.UseBlockCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class UseBlockCallback {

    /**
     * Event fired when a player interacts with a block.
     */
    public static final Event<UseBlockHandler> EVENT =
            EventFactory.createArrayBacked(
                    UseBlockHandler.class,
                    UseBlockCallback::createComposite);

    private UseBlockCallback() {
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
