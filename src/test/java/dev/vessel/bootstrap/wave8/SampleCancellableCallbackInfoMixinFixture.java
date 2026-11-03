package dev.vessel.bootstrap.wave8;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sample Mixin fixture verifying Wave 8 cancellable {@link CallbackInfo} {@link Inject}
 * (`@At("HEAD")`, `cancellable = true`) bytecode inlining and runtime early-return cancellation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = {"dev.vessel.bootstrap.wave8.SyntheticCancellableTarget"})
public final class SampleCancellableCallbackInfoMixinFixture {

    @Unique
    public boolean cancelExecution;

    public SampleCancellableCallbackInfoMixinFixture() {
    }

    @Inject(method = {"executeAction"}, at = @At("HEAD"), cancellable = true)
    public void vessel$onExecuteActionHead(CallbackInfo ci) {
        if (this.cancelExecution) {
            ci.cancel();
        }
    }
}
