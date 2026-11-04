package dev.vessel.bootstrap.target;

import dev.vessel.command.VesselCommandRootRegistry;
import dev.vessel.command.VesselServerBridgeRegistry;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

/**
 * Evaluates raw administrator console input strings to determine whether they should be intercepted
 * by VESSEL's sovereign handler, routed to the live Bukkit {@link SimpleCommandMap}, or forwarded
 * transparently to Vanilla Minecraft's native console reader.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class VesselConsoleCommandRouteResolver {

    /**
     * Determines whether a raw console line should be intercepted by VESSEL's console handler.
     *
     * @param rawLine raw console input line
     * @return {@code true} if intercepted by VESSEL or Bukkit command map, {@code false} if passed through to vanilla
     */
    public boolean shouldIntercept(String rawLine) {
        if (rawLine == null) {
            return false;
        }
        String clean = rawLine.replace("\uFEFF", "").trim();
        if (clean.isEmpty()) {
            return false;
        }
        String lower = clean.toLowerCase(Locale.ROOT);
        if ("cls".equals(lower) || "clear".equals(lower)) {
            return true;
        }

        String withoutSlash = lower.startsWith("/") ? lower.substring(1).trim() : lower;
        String[] tokens = withoutSlash.split("\\s+");
        String rootCmd = tokens[0];

        // 1. Check Sovereign Kernel registry
        if (VesselCommandRootRegistry.isKernelRoot(rootCmd)) {
            return true;
        }

        // 2. Check live Bukkit command map (e.g. "chunky")
        SimpleCommandMap commandMap = Bukkit.getCommandMap();
        if (commandMap != null) {
            Command cmd = commandMap.getCommand(rootCmd);
            if (cmd != null) {
                return true;
            }
        }

        // 3. Check registered Bukkit root registry
        if (VesselCommandRootRegistry.isBukkitRoot(rootCmd)) {
            return true;
        }

        // 4. When the MinecraftServer is active, intercept all console commands for sovereign execution
        if (VesselServerBridgeRegistry.getActiveMinecraftServer() != null) {
            return true;
        }

        return false;
    }
}
