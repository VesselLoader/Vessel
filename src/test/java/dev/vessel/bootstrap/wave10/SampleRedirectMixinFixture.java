package dev.vessel.bootstrap.wave10;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sample Mixin fixture verifying Wave 10 {@link Redirect} (`@At(value = "INVOKE", target = "...")`)
 * bytecode call-site redirection inlining and runtime execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = {"dev.vessel.bootstrap.wave10.SyntheticRedirectDamageTarget"})
public final class SampleRedirectMixinFixture {

    public SampleRedirectMixinFixture() {
    }

    @Redirect(
            method = {"clampDamageFloor"},
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I")
    )
    public static int redirectMathMaxToMinimumTen(int a, int b) {
        return Math.max(a, 10);
    }
}
