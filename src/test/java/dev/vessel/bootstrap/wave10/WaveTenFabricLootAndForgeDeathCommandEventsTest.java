package dev.vessel.bootstrap.wave10;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.loot.v2.LootTableSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 10 verifying Fabric {@link LootTableEvents}, {@link LootTableSource},
 * {@link ServerEntityEvents}, and Forge/NeoForge {@link LivingDeathEvent} and
 * {@link RegisterCommandsEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
final class WaveTenFabricLootAndForgeDeathCommandEventsTest {

    private final List<String> entityLifecycleLog = new ArrayList<>();

    private static void appendBossLootPool(
            ResourceLocation id,
            List<String> poolEntriesSink,
            LootTableSource source) {
        if (source.isBuiltin() && "minecraft:entities/warden".equals(id.toString())) {
            poolEntriesSink.add("vessel:echo_shard_cluster");
        }
    }

    public void onEntityLoaded(Entity entity, World world) {
        entityLifecycleLog.add("LOAD:" + entity.getType().getKeyName() + "@" + world.getName());
    }

    public void onEntityUnloaded(Entity entity, World world) {
        entityLifecycleLog.add("UNLOAD:" + entity.getType().getKeyName() + "@" + world.getName());
    }

    private static int runForgeTestCommand(CommandContext<Object> context) {
        return Command.SINGLE_SUCCESS;
    }

    private static void cancelForgeLivingDeath(LivingDeathEvent event) {
        if ("outOfWorld".equals(event.getDamageSourceId())) {
            event.setCanceled(true);
        }
    }

    @Test
    void testFabricLootEntityLifecycleAndForgeNeoForgeDeathCommandEvents() throws Exception {
        LootTableEvents.MODIFY.clearListeners();
        ServerEntityEvents.ENTITY_LOAD.clearListeners();
        ServerEntityEvents.ENTITY_UNLOAD.clearListeners();
        entityLifecycleLog.clear();

        assertTrue(LootTableSource.VANILLA.isBuiltin());
        assertTrue(LootTableSource.MOD.isBuiltin());
        assertFalse(LootTableSource.DATA_PACK.isBuiltin());
        assertFalse(LootTableSource.REPLACED.isBuiltin());

        LootTableEvents.MODIFY.register(
                WaveTenFabricLootAndForgeDeathCommandEventsTest::appendBossLootPool);
        List<String> lootSink = new ArrayList<>();
        LootTableEvents.MODIFY.invoker().modifyLootTable(
                ResourceLocation.parse("minecraft:entities/warden"),
                lootSink,
                LootTableSource.VANILLA);
        assertEquals(List.of("vessel:echo_shard_cluster"), lootSink);

        ServerEntityEvents.ENTITY_LOAD.register(this::onEntityLoaded);
        ServerEntityEvents.ENTITY_UNLOAD.register(this::onEntityUnloaded);

        World world = new VesselWorld("world");
        Entity golem = world.spawnEntity(
                new Location(world, 0.0D, 64.0D, 0.0D),
                EntityType.IRON_GOLEM);
        ServerEntityEvents.ENTITY_LOAD.invoker().onEntityLifecycle(golem, world);
        ServerEntityEvents.ENTITY_UNLOAD.invoker().onEntityLifecycle(golem, world);
        assertEquals(
                List.of("LOAD:iron_golem@world", "UNLOAD:iron_golem@world"),
                entityLifecycleLog);

        UUID entityId = UUID.randomUUID();
        LivingDeathEvent forgeDeath = new LivingDeathEvent(entityId, "outOfWorld");
        assertEquals(entityId, forgeDeath.getEntityUuid());
        assertEquals("outOfWorld", forgeDeath.getDamageSourceId());
        assertTrue(forgeDeath.isCancelable());
        assertFalse(forgeDeath.isCanceled());

        MinecraftForge.EVENT_BUS.addListener(
                LivingDeathEvent.class,
                WaveTenFabricLootAndForgeDeathCommandEventsTest::cancelForgeLivingDeath);
        MinecraftForge.EVENT_BUS.post(forgeDeath);
        assertTrue(forgeDeath.isCanceled());

        RegisterCommandsEvent forgeCmdEvent = new RegisterCommandsEvent();
        assertNotNull(forgeCmdEvent.getDispatcher());
        forgeCmdEvent.getDispatcher().register(
                LiteralArgumentBuilder.literal("forgereload")
                        .executes(
                                WaveTenFabricLootAndForgeDeathCommandEventsTest
                                        ::runForgeTestCommand));
        assertEquals(1, forgeCmdEvent.getDispatcher().execute("forgereload", "ServerSource"));

        net.neoforged.neoforge.event.entity.living.LivingDeathEvent neoDeath =
                new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(
                        entityId,
                        "dragonBreath");
        assertEquals(entityId, neoDeath.getEntityUuid());
        assertEquals("dragonBreath", neoDeath.getDamageSourceId());
        NeoForge.EVENT_BUS.post(neoDeath);

        net.neoforged.neoforge.event.RegisterCommandsEvent neoCmdEvent =
                new net.neoforged.neoforge.event.RegisterCommandsEvent();
        assertNotNull(neoCmdEvent.getDispatcher());
        neoCmdEvent.getDispatcher().register(
                LiteralArgumentBuilder.literal("neoreload")
                        .executes(
                                WaveTenFabricLootAndForgeDeathCommandEventsTest
                                        ::runForgeTestCommand));
        assertEquals(1, neoCmdEvent.getDispatcher().execute("neoreload", "ServerSource"));

        LootTableEvents.MODIFY.clearListeners();
        ServerEntityEvents.ENTITY_LOAD.clearListeners();
        ServerEntityEvents.ENTITY_UNLOAD.clearListeners();
    }
}
