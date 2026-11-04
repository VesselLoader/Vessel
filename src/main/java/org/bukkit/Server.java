package org.bukkit;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import java.util.List;
import java.util.UUID;
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
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Standard server contract ({@code org.bukkit.Server}) for the Bukkit, Spigot, Paper, and Folia ecosystems.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.8
 */
public interface Server {

    String getName();

    String getVersion();

    String getBukkitVersion();

    PluginManager getPluginManager();

    SimpleCommandMap getCommandMap();

    Logger getLogger();

    BukkitScheduler getScheduler();

    GlobalRegionScheduler getGlobalRegionScheduler();

    RegionScheduler getRegionScheduler();

    AsyncScheduler getAsyncScheduler();

    List<World> getWorlds();

    World getWorld(String name);

    World getWorld(UUID uid);

    int getMaxWorldSize();

    ConsoleCommandSender getConsoleSender();

    boolean addRecipe(Recipe recipe);

    List<Recipe> getRecipesFor(ItemStack result);

    void clearRecipes();

    ServicesManager getServicesManager();

    Messenger getMessenger();

    boolean hasWhitelist();

    void setWhitelist(boolean value);

    BanList getBanList(BanListType type);

    OfflinePlayer getOfflinePlayer(UUID id);

    OfflinePlayer getOfflinePlayer(String name);

    Inventory createInventory(InventoryHolder owner, int size, String title);

    Inventory createInventory(InventoryHolder owner, int size);

    Inventory createInventory(InventoryHolder owner, InventoryType type);

    Inventory createInventory(InventoryHolder owner, InventoryType type, String title);
}
