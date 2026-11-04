package net.minecraftforge.fml.event.config;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent;

/**
 * Base event dispatched when a Forge mod configuration is loaded or reloaded.
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.1
 */
public class ModConfigEvent extends ModLifecycleEvent {

    private final ModConfig config;

    public ModConfigEvent() {
        this(null, "minecraft");
    }

    public ModConfigEvent(ModConfig config) {
        this(config, config != null ? config.getModId() : "minecraft");
    }

    public ModConfigEvent(ModConfig config, String modId) {
        super(modId);
        this.config = config;
    }

    public ModConfig getConfig() {
        return config;
    }
}
