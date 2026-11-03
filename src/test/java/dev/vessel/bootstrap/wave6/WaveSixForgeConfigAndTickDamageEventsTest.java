package dev.vessel.bootstrap.wave6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.minecraftforge.common.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.IntValue;
import net.minecraftforge.event.ServerTickEvent;
import net.minecraftforge.event.TickPhase;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfigType;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 6 verifying Forge {@link ForgeConfigSpec}, {@link BooleanValue},
 * {@link IntValue}, {@link ModConfig}, {@link ModConfigType}, Forge {@link ServerTickEvent},
 * {@link TickPhase}, {@link LivingHurtEvent}, and NeoForge
 * {@link net.neoforged.neoforge.event.tick.ServerTickEvent} and {@link LivingIncomingDamageEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSixForgeConfigAndTickDamageEventsTest {

    private IntValue activeMaxThreads;

    public void setOutOfRangeThreads() {
        activeMaxThreads.set(128);
    }

    @Test
    void testForgeConfigSpecAndForgeNeoForgeTickAndDamageEvents() {
        ForgeConfigSpec spec = new ForgeConfigSpec();
        spec.comment("General settings").push("general");
        BooleanValue enableOptimizations = spec.define("enableOptimizations", true);
        IntValue maxThreads = spec.defineInRange("maxThreads", 8, 1, 64);
        spec.pop().build();

        this.activeMaxThreads = maxThreads;

        assertTrue(spec.isBuilt());
        ModConfig config = new ModConfig(ModConfigType.SERVER, spec, "vessel_mod");
        assertEquals(ModConfigType.SERVER, config.getType());
        assertEquals(spec, config.getSpec());
        assertEquals("vessel_mod", config.getModId());
        assertEquals("vessel_mod-server.toml", config.getFileName());

        assertEquals("general.enableOptimizations", enableOptimizations.getPath());
        assertEquals("General settings", enableOptimizations.getComment());
        assertTrue(enableOptimizations.getAsBoolean());

        assertEquals("general.maxThreads", maxThreads.getPath());
        assertEquals(8, maxThreads.getAsInt());
        assertEquals(1, maxThreads.getMinValue());
        assertEquals(64, maxThreads.getMaxValue());

        maxThreads.set(16);
        assertEquals(16, maxThreads.getAsInt());
        assertThrows(IllegalArgumentException.class, this::setOutOfRangeThreads);

        ServerTickEvent forgeTick = new ServerTickEvent(TickPhase.END, 42L);
        assertEquals("SERVER", forgeTick.getType());
        assertEquals(TickPhase.END, forgeTick.getPhase());
        assertEquals(42L, forgeTick.getTickNumber());

        UUID entityId = UUID.randomUUID();
        LivingHurtEvent forgeHurt = new LivingHurtEvent(entityId, "lava", 6.0F);
        assertEquals(entityId, forgeHurt.getEntityUuid());
        assertEquals("lava", forgeHurt.getDamageSource());
        assertEquals(6.0F, forgeHurt.getAmount());
        assertFalse(forgeHurt.isCanceled());
        forgeHurt.setAmount(3.0F);
        forgeHurt.setCanceled(true);
        assertEquals(3.0F, forgeHurt.getAmount());
        assertTrue(forgeHurt.isCanceled());

        net.neoforged.neoforge.event.tick.ServerTickEvent neoTick =
                new net.neoforged.neoforge.event.tick.ServerTickEvent(true, 42L);
        assertTrue(neoTick.isPostTick());
        assertFalse(neoTick.isPreTick());
        assertEquals(42L, neoTick.getTickNumber());

        UUID neoEntityId = UUID.randomUUID();
        LivingIncomingDamageEvent neoDamage =
                new LivingIncomingDamageEvent(neoEntityId, "fall", 8.0F);
        assertEquals(neoEntityId, neoDamage.getEntityUuid());
        assertEquals("fall", neoDamage.getDamageSource());
        assertEquals(8.0F, neoDamage.getAmount());
        assertFalse(neoDamage.isCanceled());
        neoDamage.setAmount(4.5F);
        neoDamage.setCanceled(true);
        assertEquals(4.5F, neoDamage.getAmount());
        assertTrue(neoDamage.isCanceled());
    }
}
