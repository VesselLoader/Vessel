package dev.vessel.bootstrap.wave9;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sample Mixin fixture verifying Wave 9 {@link ModifyReturnValue} (`@At("RETURN")`) bytecode
 * inlining and runtime return value amplification.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = {"dev.vessel.bootstrap.wave9.SyntheticDamageCalculatorTarget"})
public final class SampleModifyReturnValueMixinFixture {

    @Unique
    public int bonusDamage = 15;

    public SampleModifyReturnValueMixinFixture() {
    }

    @ModifyReturnValue(method = {"computeBaseDamage"}, at = @At("RETURN"))
    public int vessel$amplifyDamage(int originalDamage) {
        int effectiveBonus = this.bonusDamage != 0 ? this.bonusDamage : 15;
        return originalDamage + effectiveBonus;
    }
}
