package org.bukkit;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.command.SimpleCommandMap;
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
 * Static singleton facade (`org.bukkit.Bukkit`) providing global access to the active VESSEL
 * {@link Server} and {@link PluginManager} for Bukkit, Spigot, Paper, and Folia plugins.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.8
 */
public final class Bukkit {

    private static volatile Server server = new VesselServer();

    private Bukkit() {
    }

    /**
     * Binds the active {@link Server} singleton instance.
     *
     * @param activeServer the server instance to bind
     */
    public static void setServer(Server activeServer) {
        server = Objects.requireNonNull(activeServer, "activeServer must not be null");
    }

    /**
     * Returns the active {@link Server} singleton instance.
     *
     * @return the active server instance
     */
    public static Server getServer() {
        return server;
    }

    public static String getName() {
        return server.getName();
    }

    public static String getVersion() {
        return server.getVersion();
    }

    public static String getBukkitVersion() {
        return server.getBukkitVersion();
    }

    public static List<World> getWorlds() {
        return server.getWorlds();
    }

    public static World getWorld(String name) {
        return server.getWorld(name);
    }

    public static World getWorld(UUID uid) {
        return server.getWorld(uid);
    }

    public static int getMaxWorldSize() {
        return server.getMaxWorldSize();
    }

    public static org.bukkit.command.ConsoleCommandSender getConsoleSender() {
        return server.getConsoleSender();
    }

    public static PluginManager getPluginManager() {
        return server.getPluginManager();
    }

    public static SimpleCommandMap getCommandMap() {
        return server.getCommandMap();
    }

    public static Logger getLogger() {
        return server.getLogger();
    }

    public static BukkitScheduler getScheduler() {
        return server.getScheduler();
    }

    public static GlobalRegionScheduler getGlobalRegionScheduler() {
        return server.getGlobalRegionScheduler();
    }

    public static RegionScheduler getRegionScheduler() {
        return server.getRegionScheduler();
    }

    public static AsyncScheduler getAsyncScheduler() {
        return server.getAsyncScheduler();
    }

    public static boolean addRecipe(Recipe recipe) {
        return server.addRecipe(recipe);
    }

    public static List<Recipe> getRecipesFor(ItemStack result) {
        return server.getRecipesFor(result);
    }

    public static void clearRecipes() {
        server.clearRecipes();
    }

    public static ServicesManager getServicesManager() {
        return server.getServicesManager();
    }

    public static Messenger getMessenger() {
        return server.getMessenger();
    }

    public static boolean hasWhitelist() {
        return server.hasWhitelist();
    }

    public static void setWhitelist(boolean value) {
        server.setWhitelist(value);
    }

    public static BanList getBanList(BanListType type) {
        return server.getBanList(type);
    }

    public static OfflinePlayer getOfflinePlayer(UUID id) {
        return server.getOfflinePlayer(id);
    }

    public static OfflinePlayer getOfflinePlayer(String name) {
        return server.getOfflinePlayer(name);
    }

    public static Inventory createInventory(InventoryHolder owner, int size, String title) {
        return server.createInventory(owner, size, title);
    }

    public static Inventory createInventory(InventoryHolder owner, int size) {
        return server.createInventory(owner, size);
    }

    public static Inventory createInventory(InventoryHolder owner, InventoryType type) {
        return server.createInventory(owner, type);
    }

    public static Inventory createInventory(InventoryHolder owner, InventoryType type, String title) {
        return server.createInventory(owner, type, title);
    }
}
