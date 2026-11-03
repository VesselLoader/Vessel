package dev.vessel.bootstrap.wave9;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import net.fabricmc.fabric.api.event.player.InteractionResult;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.player.PlayerLoggedInEvent;
import net.minecraftforge.event.level.BlockBreakEvent;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 9 verifying Fabric {@link PlayerBlockBreakEvents},
 * {@link UseBlockCallback}, {@link UseItemCallback}, {@link InteractionResult}, and Forge/NeoForge
 * {@link BlockBreakEvent} and {@link PlayerLoggedInEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveNineFabricPlayerEventsAndForgeBlockPlayerTest {

    private static final BlockPos PROTECTED_SPAWN_POS = new BlockPos(0, 64, 0);

    private static boolean checkBlockBreakPermission(
            String worldName,
            String playerName,
            BlockPos pos,
            String blockStateId) {
        return !PROTECTED_SPAWN_POS.equals(pos);
    }

    private static InteractionResult handleBlockInteract(
            String playerName,
            String worldName,
            String hand,
            BlockPos hitPos) {
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleItemUse(
            String playerName,
            String worldName,
            String hand,
            BlockPos hitPos) {
        return InteractionResult.CONSUME;
    }

    @Test
    void testFabricPlayerBlockBreakUseBlockAndForgeNeoForgeBlockAndPlayerEvents() {
        PlayerBlockBreakEvents.BEFORE.clearListeners();
        UseBlockCallback.EVENT.clearListeners();
        UseItemCallback.EVENT.clearListeners();

        PlayerBlockBreakEvents.BEFORE.register(
                WaveNineFabricPlayerEventsAndForgeBlockPlayerTest::checkBlockBreakPermission);
        UseBlockCallback.EVENT.register(
                WaveNineFabricPlayerEventsAndForgeBlockPlayerTest::handleBlockInteract);
        UseItemCallback.EVENT.register(
                WaveNineFabricPlayerEventsAndForgeBlockPlayerTest::handleItemUse);

        assertFalse(
                PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                        "minecraft:overworld",
                        "Shiroz",
                        new BlockPos(0, 64, 0),
                        "minecraft:bedrock"));
        assertTrue(
                PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                        "minecraft:overworld",
                        "Shiroz",
                        new BlockPos(10, 64, 10),
                        "minecraft:stone"));

        InteractionResult blockResult = UseBlockCallback.EVENT.invoker().interact(
                "Shiroz",
                "minecraft:overworld",
                "MAIN_HAND",
                new BlockPos(5, 64, 5));
        assertEquals(InteractionResult.SUCCESS, blockResult);
        assertTrue(blockResult.consumesAction());
        assertFalse(InteractionResult.PASS.consumesAction());

        InteractionResult itemResult = UseItemCallback.EVENT.invoker().interact(
                "Shiroz",
                "minecraft:overworld",
                "MAIN_HAND",
                new BlockPos(5, 64, 5));
        assertEquals(InteractionResult.CONSUME, itemResult);
        assertTrue(itemResult.consumesAction());

        UUID playerUuid = UUID.randomUUID();
        BlockPos breakPos = new BlockPos(12, 70, -8);
        BlockBreakEvent forgeBreak = new BlockBreakEvent(
                "minecraft:overworld",
                breakPos,
                "minecraft:diamond_ore",
                playerUuid,
                10);
        assertEquals("minecraft:overworld", forgeBreak.getLevelName());
        assertEquals(breakPos, forgeBreak.getPos());
        assertEquals("minecraft:diamond_ore", forgeBreak.getState());
        assertEquals(playerUuid, forgeBreak.getPlayerUuid());
        assertEquals(10, forgeBreak.getExpToDrop());
        forgeBreak.setExpToDrop(25);
        assertEquals(25, forgeBreak.getExpToDrop());
        assertFalse(forgeBreak.isCanceled());
        forgeBreak.setCanceled(true);
        assertTrue(forgeBreak.isCanceled());

        PlayerLoggedInEvent forgeLogin = new PlayerLoggedInEvent(playerUuid, "Shiroz");
        assertEquals(playerUuid, forgeLogin.getPlayerUuid());
        assertEquals("Shiroz", forgeLogin.getPlayerName());

        net.neoforged.neoforge.event.level.BlockBreakEvent neoBreak =
                new net.neoforged.neoforge.event.level.BlockBreakEvent(
                        "minecraft:overworld",
                        breakPos,
                        "minecraft:emerald_ore",
                        playerUuid);
        assertEquals(breakPos, neoBreak.getPos());
        assertEquals("minecraft:emerald_ore", neoBreak.getState());
        assertFalse(neoBreak.isCanceled());
        neoBreak.setCanceled(true);
        assertTrue(neoBreak.isCanceled());

        net.neoforged.neoforge.event.entity.player.PlayerLoggedInEvent neoLogin =
                new net.neoforged.neoforge.event.entity.player.PlayerLoggedInEvent(
                        playerUuid,
                        "Shiroz");
        assertEquals(playerUuid, neoLogin.getPlayerUuid());
        assertEquals("Shiroz", neoLogin.getPlayerName());

        PlayerBlockBreakEvents.BEFORE.clearListeners();
        UseBlockCallback.EVENT.clearListeners();
        UseItemCallback.EVENT.clearListeners();
    }
}
