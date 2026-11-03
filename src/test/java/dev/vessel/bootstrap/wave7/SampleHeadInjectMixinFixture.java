package dev.vessel.bootstrap.wave7;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Sample Mixin fixture verifying Wave 7 no-arg {@link Inject} (`@At("HEAD")`) instruction inlining
 * and {@link Unique} field merging into target method bytecode.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = {"dev.vessel.bootstrap.wave7.SyntheticTickTarget"})
public final class SampleHeadInjectMixinFixture {

    @Unique
    public int headHookCallCount;

    public SampleHeadInjectMixinFixture() {
    }

    @Inject(method = {"runTick"}, at = @At("HEAD"))
    public void vessel$onRunTickHead() {
        this.headHookCallCount++;
    }
}
