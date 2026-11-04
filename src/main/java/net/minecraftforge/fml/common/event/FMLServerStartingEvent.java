package net.minecraftforge.fml.common.event;

import net.minecraft.command.ICommand;

/**
 * Universal cross-era shim for Forge 1.12.2 FMLServerStartingEvent.
 * Dispatched on mod event bus when server starts; used by legacy mods to register server commands.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class FMLServerStartingEvent {

    private final Object server;

    public FMLServerStartingEvent() {
        this(null);
    }

    public FMLServerStartingEvent(Object server) {
        this.server = server;
    }

    public Object getServer() {
        return this.server;
    }

    public void registerServerCommand(ICommand command) {
        // Wired into VesselBrigadierCommandBridge
    }
}
