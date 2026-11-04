package net.minecraft.world.level.block;

import net.minecraft.sounds.SoundEvent;

/**
 * Universal cross-era shim for Mojang SoundType.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class SoundType {

    public static final SoundType WOOD = new SoundType(1.0F, 1.0F, null, null, null, null, null);
    public static final SoundType STONE = new SoundType(1.0F, 1.0F, null, null, null, null, null);
    public static final SoundType METAL = new SoundType(1.0F, 1.0F, null, null, null, null, null);
    public static final SoundType GLASS = new SoundType(1.0F, 1.0F, null, null, null, null, null);

    private final float volume;
    private final float pitch;
    private final SoundEvent breakSound;
    private final SoundEvent stepSound;
    private final SoundEvent placeSound;
    private final SoundEvent hitSound;
    private final SoundEvent fallSound;

    public SoundType(
            float volume,
            float pitch,
            SoundEvent breakSound,
            SoundEvent stepSound,
            SoundEvent placeSound,
            SoundEvent hitSound,
            SoundEvent fallSound) {
        this.volume = volume;
        this.pitch = pitch;
        this.breakSound = breakSound;
        this.stepSound = stepSound;
        this.placeSound = placeSound;
        this.hitSound = hitSound;
        this.fallSound = fallSound;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public SoundEvent getBreakSound() {
        return this.breakSound;
    }

    public SoundEvent getStepSound() {
        return this.stepSound;
    }

    public SoundEvent getPlaceSound() {
        return this.placeSound;
    }

    public SoundEvent getHitSound() {
        return this.hitSound;
    }

    public SoundEvent getFallSound() {
        return this.fallSound;
    }
}
