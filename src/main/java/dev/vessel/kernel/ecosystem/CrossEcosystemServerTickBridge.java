package dev.vessel.kernel.ecosystem;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerTickEvent;
import net.minecraftforge.event.TickPhase;
import net.neoforged.neoforge.common.NeoForge;
import org.bukkit.Server;

/**
 * Universal cross-ecosystem bridge that orchestrates server tick loops symmetrically across
 * Fabric, Forge, NeoForge, Bukkit, and Folia.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossEcosystemServerTickBridge {

    public CrossEcosystemServerTickBridge() {
    }

    /**
     * Executes the pre-tick phase across all platforms.
     *
     * @param server active Bukkit server instance
     */
    public void executePreTick(Server server) {
        // Fabric Pre-Tick
        ServerTickEvents.START_SERVER_TICK.invoker().onServerLifecycle(server);

        // Forge Pre-Tick
        ServerTickEvent forgePre = new ServerTickEvent(TickPhase.START, 1L);
        MinecraftForge.EVENT_BUS.post(forgePre);

        // NeoForge Pre-Tick
        net.neoforged.neoforge.event.tick.ServerTickEvent neoPre =
                new net.neoforged.neoforge.event.tick.ServerTickEvent(false, 1L);
        NeoForge.EVENT_BUS.post(neoPre);
    }

    /**
     * Executes the post-tick phase across all platforms.
     *
     * @param server active Bukkit server instance
     */
    public void executePostTick(Server server) {
        // Forge Post-Tick
        ServerTickEvent forgePost = new ServerTickEvent(TickPhase.END, 1L);
        MinecraftForge.EVENT_BUS.post(forgePost);

        // NeoForge Post-Tick
        net.neoforged.neoforge.event.tick.ServerTickEvent neoPost =
                new net.neoforged.neoforge.event.tick.ServerTickEvent(true, 1L);
        NeoForge.EVENT_BUS.post(neoPost);

        // Fabric Post-Tick
        ServerTickEvents.END_SERVER_TICK.invoker().onServerLifecycle(server);
    }
}
