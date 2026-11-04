package org.bukkit;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.VesselServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Sovereign runtime implementation of the Bukkit/Spigot/Paper {@link Server} contract,
 * hosting plugin registries, worlds, command dispatch, and scheduler services.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.3
 */
public class VesselServer implements Server {

    private final String name;
    private final String version;
    private final String bukkitVersion;
    private final PluginManager pluginManager;
    private final SimpleCommandMap commandMap;
    private final Logger logger;
    private final BukkitScheduler scheduler = new BukkitScheduler();
    private final GlobalRegionScheduler globalRegionScheduler = new GlobalRegionScheduler();
    private final RegionScheduler regionScheduler = new RegionScheduler();
    private final AsyncScheduler asyncScheduler = new AsyncScheduler();
    private final List<Recipe> recipes = new CopyOnWriteArrayList<>();
    private final ServicesManager servicesManager = new VesselServicesManager();
    private final Messenger messenger = new Messenger();
    private volatile boolean whitelistEnabled;
    private final Map<BanListType, BanList> banLists = new ConcurrentHashMap<>();
    private final Map<UUID, OfflinePlayer> offlinePlayers = new ConcurrentHashMap<>();
    private final Map<String, World> worlds = new ConcurrentHashMap<>();
    private final ConsoleCommandSender consoleSender = new ConsoleCommandSender();

    /**
     * Constructs a default VESSEL Bukkit Server instance targeting the latest Minecraft release.
     */
    public VesselServer() {
        this("VESSEL-Sovereign", "1.21.4-R0.1-SNAPSHOT", "1.21.4-R0.1-SNAPSHOT");
    }

    /**
     * Constructs a VESSEL Bukkit Server instance with explicit brand and version identifiers.
     *
     * @param name          the server brand name
     * @param version       the full server version string
     * @param bukkitVersion the Bukkit API version string
     */
    public VesselServer(String name, String version, String bukkitVersion) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.bukkitVersion = Objects.requireNonNull(bukkitVersion, "bukkitVersion must not be null");
        this.pluginManager = new org.bukkit.plugin.VesselPluginManager();
        this.commandMap = new SimpleCommandMap(this);
        this.logger = Logger.getLogger("Minecraft.VESSEL");

        World overworld = new VesselWorld("world");
        World nether = new VesselWorld("world_nether");
        World end = new VesselWorld("world_the_end");
        worlds.put(overworld.getName(), overworld);
        worlds.put(nether.getName(), nether);
        worlds.put(end.getName(), end);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getBukkitVersion() {
        return bukkitVersion;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public SimpleCommandMap getCommandMap() {
        return commandMap;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public BukkitScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public GlobalRegionScheduler getGlobalRegionScheduler() {
        return globalRegionScheduler;
    }

    @Override
    public RegionScheduler getRegionScheduler() {
        return regionScheduler;
    }

    @Override
    public AsyncScheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    @Override
    public List<World> getWorlds() {
        return new ArrayList<>(worlds.values());
    }

    @Override
    public World getWorld(String name) {
        if (name == null) {
            return null;
        }
        World direct = worlds.get(name);
        if (direct != null) {
            return direct;
        }
        for (World w : worlds.values()) {
            if (w.getName().equalsIgnoreCase(name)) {
                return w;
            }
        }
        return null;
    }

    @Override
    public World getWorld(UUID uid) {
        if (uid == null) {
            return null;
        }
        for (World w : worlds.values()) {
            if (uid.equals(w.getUID())) {
                return w;
            }
        }
        return null;
    }

    @Override
    public int getMaxWorldSize() {
        return 29999984;
    }

    @Override
    public ConsoleCommandSender getConsoleSender() {
        return consoleSender;
    }

    @Override
    public boolean addRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        recipes.add(recipe);
        return true;
    }

    @Override
    public List<Recipe> getRecipesFor(ItemStack result) {
        if (result == null) {
            return List.of();
        }
        List<Recipe> matched = new ArrayList<>();
        for (Recipe recipe : recipes) {
            ItemStack recipeResult = recipe.getResult();
            if (recipeResult != null && recipeResult.getType() == result.getType()) {
                matched.add(recipe);
            }
        }
        return Collections.unmodifiableList(matched);
    }

    @Override
    public void clearRecipes() {
        recipes.clear();
    }

    @Override
    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    @Override
    public Messenger getMessenger() {
        return messenger;
    }

    @Override
    public boolean hasWhitelist() {
        return whitelistEnabled;
    }

    @Override
    public void setWhitelist(boolean value) {
        this.whitelistEnabled = value;
    }

    @Override
    public BanList getBanList(BanListType type) {
        Objects.requireNonNull(type, "type must not be null");
        return banLists.computeIfAbsent(type, BanList::new);
    }

    @Override
    public OfflinePlayer getOfflinePlayer(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return offlinePlayers.computeIfAbsent(
                id,
                uuid -> new OfflinePlayer(uuid, "Offline-" + uuid.toString().substring(0, 8)));
    }

    @Override
    public OfflinePlayer getOfflinePlayer(String name) {
        Objects.requireNonNull(name, "name must not be null");
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return offlinePlayers.computeIfAbsent(id, uuid -> new OfflinePlayer(uuid, name));
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size, String title) {
        return new Inventory(owner, size, title);
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, int size) {
        return new Inventory(owner, size, "Chest");
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, InventoryType type) {
        return new Inventory(owner, type);
    }

    @Override
    public Inventory createInventory(InventoryHolder owner, InventoryType type, String title) {
        Objects.requireNonNull(type, "type must not be null");
        return new Inventory(owner, type, type.getDefaultSize(), title);
    }
}
