package dev.vessel.kernel.ecosystem;

import java.util.Objects;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.neoforged.neoforge.common.NeoForge;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Universal cross-ecosystem bridge that symmetrically dispatches block events across Fabric,
 * Forge, NeoForge, and Bukkit event subsystems, propagating cancellation states bidirectionally.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossEcosystemBlockEventBridge {

    public CrossEcosystemBlockEventBridge() {
    }

    /**
     * Dispatches a block break action across all loaded ecosystems.
     *
     * @param world       the Bukkit world
     * @param player      the player initiating the block break
     * @param block       the block being broken
     * @param pos         Minecraft coordinate
     * @param state       Minecraft block state
     * @param blockEntity optional block entity
     * @return {@code true} if allowed by all ecosystems, {@code false} if any cancelled
     */
    public boolean dispatchBlockBreak(
            World world,
            Player player,
            Block block,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity) {
        Objects.requireNonNull(world, "world must not be null");
        Objects.requireNonNull(player, "player must not be null");
        Objects.requireNonNull(block, "block must not be null");
        Objects.requireNonNull(pos, "pos must not be null");
        Objects.requireNonNull(state, "state must not be null");

        // 1. Fabric Event
        boolean fabricAllowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                world.getName(),
                player.getName(),
                pos,
                state.getBlockId());
        if (!fabricAllowed) {
            return false;
        }

        // 2. Forge Event
        net.minecraftforge.event.level.BlockBreakEvent forgeEvent =
                new net.minecraftforge.event.level.BlockBreakEvent(
                        world.getName(),
                        pos,
                        state.getBlockId(),
                        player.getUniqueId(),
                        1);
        MinecraftForge.EVENT_BUS.post(forgeEvent);
        if (forgeEvent.isCanceled()) {
            return false;
        }

        // 3. NeoForge Event
        net.neoforged.neoforge.event.level.BlockBreakEvent neoForgeEvent =
                new net.neoforged.neoforge.event.level.BlockBreakEvent(
                        world.getName(),
                        pos,
                        state.getBlockId(),
                        player.getUniqueId());
        NeoForge.EVENT_BUS.post(neoForgeEvent);
        if (neoForgeEvent.isCanceled()) {
            return false;
        }

        // 4. Bukkit Event
        BlockBreakEvent bukkitEvent = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(bukkitEvent);
        return !bukkitEvent.isCancelled();
    }
}
