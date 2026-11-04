package com.example.mod.pure;

import dev.vessel.api.VesselAPI;
import dev.vessel.api.block.VesselBlock;
import dev.vessel.api.item.VesselItem;
import dev.vessel.api.network.VesselPayloadChannel;
import dev.vessel.api.registry.VesselDeferredRegister;
import dev.vessel.api.registry.VesselRegistryKey;
import dev.vessel.api.registry.VesselRegistrySupplier;

import java.util.Objects;

/**
 * Main lifecycle entrypoint and developer reference for a pure VESSEL Mod.
 *
 * <p>Demonstrates how mod developers can build ultra-high performance mods that run seamlessly
 * across Fabric, Quilt, Forge, NeoForge, Paper, Spigot, Bukkit, and Folia using the expressive,
 * lightweight VESSEL Super-API without writing a single line of loader-specific boilerplate.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 * @since 1.0.0
 */
public final class PureVesselExampleMod {

    public static final String MOD_ID = "vesselmod";

    public static final VesselDeferredRegister<VesselBlock> BLOCKS =
            VesselAPI.createDeferredRegister(VesselRegistryKey.BLOCK, MOD_ID);

    public static final VesselDeferredRegister<VesselItem> ITEMS =
            VesselAPI.createDeferredRegister(VesselRegistryKey.ITEM, MOD_ID);

    public static final VesselRegistrySupplier<VesselBlock> QUANTUM_BATTERY =
            BLOCKS.register("quantum_battery", () ->
                    VesselAPI.createBlock("vesselmod:quantum_battery")
                            .hardness(4.5f)
                            .resistance(12.0f)
                            .luminance(15)
                            .sound("metal")
                            .energyStorage(50_000_000L)
                            .build()
            );

    public static final VesselRegistrySupplier<VesselItem> PLASMA_BLADE =
            ITEMS.register("plasma_blade", () ->
                    VesselAPI.createItem("vesselmod:plasma_blade")
                            .durability(1500)
                            .rarity("EPIC")
                            .fireResistant(true)
                            .energyCapacity(100_000L)
                            .onRightClick((player, world, hand) -> true)
                            .build()
            );

    public static final VesselPayloadChannel NETWORK_CHANNEL =
            VesselAPI.createNetworkChannel("vesselmod:main");

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
     * Initializes the mod, registers deferred content, commands, and binds listeners.
     */
    public void onInitialize() {
        // 1. Mount Deferred Registers into universal registry
        BLOCKS.register();
        ITEMS.register();

        // 2. Load configuration via flat token substrate
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

        // 3. Register universal command into VesselCommandHub
        modCommand.registerCommand("vesselmod", this);

        // 4. Register event listener
        VesselAPI.registerEvents(eventListener);
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
