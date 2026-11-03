package dev.vessel.bootstrap.wave3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Integration tests for Wave 3 verifying SpongePowered Mixin annotations
 * ({@link Mixin}, {@link Shadow}, {@link Unique}, {@link Inject}, {@link Redirect}, {@link At})
 * and runtime callback carriers ({@link CallbackInfo} and {@link CallbackInfoReturnable}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@Mixin(targets = "net.minecraft.server.MinecraftServer", priority = 900)
final class WaveThreeMixinAndCallbackInfoTest {

    @Shadow(prefix = "shadow$", remap = true, aliases = {"field_tickCount"})
    private int tickCount;

    @Unique(silent = true)
    public int vesselUniqueGetTickCount() {
        return tickCount;
    }

    @Inject(method = "loadLevel", at = @At("HEAD"), cancellable = true)
    public void onLoadLevelHead(CallbackInfo ci) {
        if (ci.isCancellable()) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "runServer",
            at = @At(value = "INVOKE", target = "java/lang/Thread.yield()V", ordinal = 0))
    public void redirectYieldCall() {
        this.tickCount++;
    }

    @Test
    void testMixinAnnotationsReflectiveMetadataAndCallbackInfoSemantics() throws Exception {
        Mixin mixinAnnotation =
                WaveThreeMixinAndCallbackInfoTest.class.getAnnotation(Mixin.class);
        assertNotNull(mixinAnnotation);
        assertArrayEquals(
                new String[]{"net.minecraft.server.MinecraftServer"},
                mixinAnnotation.targets());
        assertEquals(900, mixinAnnotation.priority());
        assertTrue(mixinAnnotation.remap());

        Field shadowField =
                WaveThreeMixinAndCallbackInfoTest.class.getDeclaredField("tickCount");
        Shadow shadowAnnotation = shadowField.getAnnotation(Shadow.class);
        assertNotNull(shadowAnnotation);
        assertEquals("shadow$", shadowAnnotation.prefix());
        assertArrayEquals(new String[]{"field_tickCount"}, shadowAnnotation.aliases());

        Method uniqueMethod =
                WaveThreeMixinAndCallbackInfoTest.class.getDeclaredMethod(
                        "vesselUniqueGetTickCount");
        Unique uniqueAnnotation = uniqueMethod.getAnnotation(Unique.class);
        assertNotNull(uniqueAnnotation);
        assertTrue(uniqueAnnotation.silent());

        Method injectMethod =
                WaveThreeMixinAndCallbackInfoTest.class.getDeclaredMethod(
                        "onLoadLevelHead",
                        CallbackInfo.class);
        Inject injectAnnotation = injectMethod.getAnnotation(Inject.class);
        assertNotNull(injectAnnotation);
        assertArrayEquals(new String[]{"loadLevel"}, injectAnnotation.method());
        assertTrue(injectAnnotation.cancellable());
        assertEquals(1, injectAnnotation.at().length);
        assertEquals("HEAD", injectAnnotation.at()[0].value());

        Method redirectMethod =
                WaveThreeMixinAndCallbackInfoTest.class.getDeclaredMethod("redirectYieldCall");
        Redirect redirectAnnotation = redirectMethod.getAnnotation(Redirect.class);
        assertNotNull(redirectAnnotation);
        assertEquals("INVOKE", redirectAnnotation.at().value());
        assertEquals("java/lang/Thread.yield()V", redirectAnnotation.at().target());
        assertEquals(0, redirectAnnotation.at().ordinal());

        CallbackInfo nonCancellable = new CallbackInfo("nonCancellableHook", false);
        assertEquals("nonCancellableHook", nonCancellable.getId());
        assertFalse(nonCancellable.isCancellable());
        assertFalse(nonCancellable.isCancelled());
        try {
            nonCancellable.cancel();
            fail("Expected IllegalStateException when cancelling non-cancellable CallbackInfo");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("nonCancellableHook"));
        }

        CallbackInfo cancellableCi = new CallbackInfo("loadLevel", true);
        onLoadLevelHead(cancellableCi);
        assertTrue(cancellableCi.isCancelled());

        CallbackInfoReturnable<String> cir =
                new CallbackInfoReturnable<>("getServerModName", true, "vanilla");
        assertEquals("vanilla", cir.getReturnValue());
        assertFalse(cir.isCancelled());
        cir.setReturnValue("VESSEL_OVERRIDDEN");
        assertTrue(cir.isCancelled());
        assertEquals("VESSEL_OVERRIDDEN", cir.getReturnValue());

        CallbackInfoReturnable<Integer> intCir =
                new CallbackInfoReturnable<>("getMaxPlayers", true, 42);
        assertEquals(42, intCir.getReturnValueI());
    }
}
