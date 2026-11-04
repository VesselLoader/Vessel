package net.minecraftforge.event.entity.player;

/**
 * Universal multi-era event matching bytecode signature PlayerEvent$StartTracking.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.1
 */
public class PlayerEvent$StartTracking extends PlayerEvent {

    public PlayerEvent$StartTracking() {
        super();
    }

    public PlayerEvent$StartTracking(Object player, Object target) {
        super(player);
    }
}
