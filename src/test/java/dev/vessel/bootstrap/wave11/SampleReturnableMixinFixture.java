package dev.vessel.bootstrap.wave11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin fixture for testing Wave 11 {@link CallbackInfoReturnable} early return bytecode weaving.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = {"dev.vessel.bootstrap.wave11.SyntheticDefenseTarget"})
public final class SampleReturnableMixinFixture {

    public SampleReturnableMixinFixture() {
    }

    @Inject(method = "computeBaseDefense", at = @At("HEAD"), cancellable = true)
    public void injectEarlyDefenseOverride(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(999);
    }
}
