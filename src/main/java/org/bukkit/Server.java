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
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Runtime representation of the VESSEL Minecraft Server for the Bukkit, Spigot, Paper, and Folia
 * plugin ecosystems.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.5
 */
public class Server {

    private final String name;
    private final String version;
    private final String bukkitVersion;
    private final PluginManager pluginManager;
    private final Logger logger;
    private final BukkitScheduler scheduler = new BukkitScheduler();
    private final GlobalRegionScheduler globalRegionScheduler = new GlobalRegionScheduler();
    private final RegionScheduler regionScheduler = new RegionScheduler();
    private final AsyncScheduler asyncScheduler = new AsyncScheduler();
    private final List<Recipe> recipes = new CopyOnWriteArrayList<>();
    private final ServicesManager servicesManager = new ServicesManager();
    private final Messenger messenger = new Messenger();
    private volatile boolean whitelistEnabled;
    private final Map<BanListType, BanList> banLists = new ConcurrentHashMap<>();
    private final Map<UUID, OfflinePlayer> offlinePlayers = new ConcurrentHashMap<>();

    /**
     * Constructs a default VESSEL Bukkit Server instance targeting the latest Minecraft release.
     */
    public Server() {
        this("VESSEL-Sovereign", "1.21.4-R0.1-SNAPSHOT", "1.21.4-R0.1-SNAPSHOT");
    }

    /**
     * Constructs a VESSEL Bukkit Server instance with explicit brand and version identifiers.
     *
     * @param name          the server brand name
     * @param version       the full server version string
     * @param bukkitVersion the Bukkit API version string
     */
    public Server(String name, String version, String bukkitVersion) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.bukkitVersion = Objects.requireNonNull(bukkitVersion, "bukkitVersion must not be null");
        this.pluginManager = new PluginManager();
        this.logger = Logger.getLogger("Minecraft.VESSEL");
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public Logger getLogger() {
        return logger;
    }

    public BukkitScheduler getScheduler() {
        return scheduler;
    }

    public GlobalRegionScheduler getGlobalRegionScheduler() {
        return globalRegionScheduler;
    }

    public RegionScheduler getRegionScheduler() {
        return regionScheduler;
    }

    public AsyncScheduler getAsyncScheduler() {
        return asyncScheduler;
    }

    public boolean addRecipe(Recipe recipe) {
        if (recipe == null) {
            return false;
        }
        recipes.add(recipe);
        return true;
    }

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

    public void clearRecipes() {
        recipes.clear();
    }

    public ServicesManager getServicesManager() {
        return servicesManager;
    }

    public Messenger getMessenger() {
        return messenger;
    }

    public boolean hasWhitelist() {
        return whitelistEnabled;
    }

    public void setWhitelist(boolean value) {
        this.whitelistEnabled = value;
    }

    public BanList getBanList(BanListType type) {
        Objects.requireNonNull(type, "type must not be null");
        return banLists.computeIfAbsent(type, BanList::new);
    }

    public OfflinePlayer getOfflinePlayer(UUID id) {
        Objects.requireNonNull(id, "id must not be null");
        return offlinePlayers.computeIfAbsent(
                id,
                uuid -> new OfflinePlayer(uuid, "Offline-" + uuid.toString().substring(0, 8)));
    }

    public OfflinePlayer getOfflinePlayer(String name) {
        Objects.requireNonNull(name, "name must not be null");
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return offlinePlayers.computeIfAbsent(id, uuid -> new OfflinePlayer(uuid, name));
    }

    public Inventory createInventory(InventoryHolder owner, int size, String title) {
        return new Inventory(owner, size, title);
    }

    public Inventory createInventory(InventoryHolder owner, int size) {
        return new Inventory(owner, size, "Chest");
    }

    public Inventory createInventory(InventoryHolder owner, InventoryType type) {
        return new Inventory(owner, type);
    }

    public Inventory createInventory(InventoryHolder owner, InventoryType type, String title) {
        Objects.requireNonNull(type, "type must not be null");
        return new Inventory(owner, type, type.getDefaultSize(), title);
    }
}
