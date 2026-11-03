package com.example.mod.pure;

import java.util.Objects;

/**
 * Main lifecycle entrypoint and developer reference for a pure VESSEL Mod.
 *
 * <p>Demonstrates how mod developers can build ultra-high performance mods that run seamlessly
 * across Fabric, Quilt, Forge, NeoForge, Paper, Spigot, Bukkit, and Folia without writing a single line
 * of loader-specific boilerplate or suffering thread-safety desyncs.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class PureVesselExampleMod {

    private final PureModConfigHolder configHolder;
    private final ZeroAllocEnergyService energyService;
    private final QuantumBatteryBlockHandler blockHandler;
    private final PlasmaBladeItemManager itemManager;
    private final RegionalTeleportDispatcher teleportDispatcher;
    private final SovereignModCommand modCommand;
    private final PureVesselEventListener eventListener;

    /**
     * Constructs and bootstraps all pure VESSEL subsystems.
     */
    public PureVesselExampleMod() {
        this.configHolder = new PureModConfigHolder();
        this.energyService = new ZeroAllocEnergyService(10000000L, 50000L);
        this.blockHandler = new QuantumBatteryBlockHandler();
        this.itemManager = new PlasmaBladeItemManager();
        this.teleportDispatcher = new RegionalTeleportDispatcher();
        this.modCommand = new SovereignModCommand();
        this.eventListener = new PureVesselEventListener();
    }

    /**
     * Initializes the mod, registers commands, and binds listeners.
     */
    public void onInitialize() {
        // 1. Load configuration via flat token substrate
        configHolder.loadConfiguration("""
                energy:
                  battery:
                    capacity: 50000000
                  conduit:
                    max-transfer: 100000
                teleport:
                  blacklisted-dimensions:
                    - "minecraft:the_end"
                """);

        // 2. Register universal command into VesselCommandHub
        modCommand.registerCommand("vesselmod", this);
    }

    /**
     * Returns the active configuration holder.
     *
     * @return config holder
     */
    public PureModConfigHolder getConfigHolder() {
        return configHolder;
    }

    /**
     * Returns the energy service.
     *
     * @return energy service
     */
    public ZeroAllocEnergyService getEnergyService() {
        return energyService;
    }

    /**
     * Returns the block handler.
     *
     * @return block handler
     */
    public QuantumBatteryBlockHandler getBlockHandler() {
        return blockHandler;
    }

    /**
     * Returns the item manager.
     *
     * @return item manager
     */
    public PlasmaBladeItemManager getItemManager() {
        return itemManager;
    }

    /**
     * Returns the regional teleport dispatcher.
     *
     * @return teleport dispatcher
     */
    public RegionalTeleportDispatcher getTeleportDispatcher() {
        return teleportDispatcher;
    }

    /**
     * Returns the universal command handler.
     *
     * @return command handler
     */
    public SovereignModCommand getModCommand() {
        return modCommand;
    }

    /**
     * Returns the event listener.
     *
     * @return event listener
     */
    public PureVesselEventListener getEventListener() {
        return eventListener;
    }
}
