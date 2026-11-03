package net.fabricmc.fabric.api.event.player;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Event Player API block break event hooks
 * ({@code net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerBlockBreakEvents {

    /**
     * Event fired before a block is broken by a player; returning {@code false} from any
     * listener cancels the block break.
     */
    public static final Event<BlockBreakCallback> BEFORE =
            EventFactory.createArrayBacked(
                    BlockBreakCallback.class,
                    PlayerBlockBreakEvents::createBeforeComposite);

    private PlayerBlockBreakEvents() {
    }

    private static BlockBreakCallback createBeforeComposite(
            List<BlockBreakCallback> callbacks) {
        return (worldName, playerName, pos, blockStateId) -> {
            for (BlockBreakCallback cb : callbacks) {
                if (!cb.beforeBlockBreak(worldName, playerName, pos, blockStateId)) {
                    return false;
                }
            }
            return true;
        };
    }
}
