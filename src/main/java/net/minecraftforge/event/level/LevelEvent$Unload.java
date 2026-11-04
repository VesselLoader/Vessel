package net.minecraftforge.event.level;

/**
 * Universal multi-era event fired when a level/world is unloaded.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class LevelEvent$Unload extends LevelEvent {

    public LevelEvent$Unload() {
        super(null);
    }

    public LevelEvent$Unload(Object level) {
        super(level);
    }
}
