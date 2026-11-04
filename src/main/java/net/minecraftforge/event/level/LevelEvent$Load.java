package net.minecraftforge.event.level;

/**
 * Universal multi-era event fired when a level/world is loaded.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public class LevelEvent$Load extends LevelEvent {

    public LevelEvent$Load() {
        super(null);
    }

    public LevelEvent$Load(Object level) {
        super(level);
    }
}
