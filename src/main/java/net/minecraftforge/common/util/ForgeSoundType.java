package net.minecraftforge.common.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import java.util.function.Supplier;

/**
 * Universal cross-era shim for ForgeSoundType.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class ForgeSoundType extends SoundType {

    public ForgeSoundType(
            float volume,
            float pitch,
            Supplier<SoundEvent> breakSound,
            Supplier<SoundEvent> stepSound,
            Supplier<SoundEvent> placeSound,
            Supplier<SoundEvent> hitSound,
            Supplier<SoundEvent> fallSound) {
        super(
                volume,
                pitch,
                breakSound != null ? breakSound.get() : null,
                stepSound != null ? stepSound.get() : null,
                placeSound != null ? placeSound.get() : null,
                hitSound != null ? hitSound.get() : null,
                fallSound != null ? fallSound.get() : null
        );
    }
}
