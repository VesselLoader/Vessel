package dev.vessel.bootstrap.wave4;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sample Mixin and MixinExtras fixture verifying Wave 4 {@link Accessor}, {@link Invoker},
 * {@link Shadow}, {@link Inject}, {@link ModifyReturnValue}, and {@link WrapOperation}
 * metadata inspection.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = "net.minecraft.world.level.Level", priority = 1100)
public final class SampleMixinAccessorAndInjectFixture {

    @Shadow
    private boolean isClientSide;

    public SampleMixinAccessorAndInjectFixture() {
    }

    @Accessor("isClientSide")
    public boolean vessel$getIsClientSide() {
        return isClientSide;
    }

    @Invoker("tickBlockEntities")
    public void vessel$invokeTickBlockEntities() {
    }

    @Inject(method = "setBlock", at = @At("HEAD"), cancellable = true)
    public void onSetBlock(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.TRUE);
    }

    @ModifyReturnValue(method = "getDayTime", at = @At("RETURN"))
    public long modifyDayTime(long original) {
        return original + 1000L;
    }

    @WrapOperation(method = "tick", at = @At("INVOKE"))
    public Object wrapTick(Operation<Object> original) {
        return original.call();
    }
}
