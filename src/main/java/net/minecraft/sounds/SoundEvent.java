package net.minecraft.sounds;

import java.util.Objects;

/**
 * Universal cross-era shim for Mojang SoundEvent.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class SoundEvent {

    private final String location;

    public SoundEvent() {
        this("minecraft:empty");
    }

    public SoundEvent(String location) {
        this.location = Objects.requireNonNull(location, "location must not be null");
    }

    public String getLocation() {
        return this.location;
    }
}
