package dev.vessel.bootstrap.wave7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.TeleportCause;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 7 verifying Bukkit {@link ShapedRecipe}, {@link ShapelessRecipe},
 * {@link FurnaceRecipe}, {@link Bukkit} (`v1.0.2`) recipe registration, {@link GameMode},
 * {@link Attribute}, {@link AttributeInstance}, {@link Player} (`v1.0.3`),
 * {@link PlayerMoveEvent}, and {@link PlayerTeleportEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSevenBukkitRecipeGameModeAndMoveTest {

    @Test
    void testBukkitRecipesGameModeAttributesAndPlayerMoveTeleportEvents() {
        Bukkit.clearRecipes();

        ShapedRecipe shaped = new ShapedRecipe(
                new NamespacedKey("vessel", "emerald_sword"),
                new ItemStack(Material.DIAMOND_SWORD, 1))
                .shape(" E ", " E ", " S ")
                .setIngredient('E', Material.EMERALD)
                .setIngredient('S', Material.OAK_PLANKS);

        ShapelessRecipe shapeless = new ShapelessRecipe(
                new NamespacedKey("vessel", "quick_apple"),
                new ItemStack(Material.GOLDEN_APPLE, 1))
                .addIngredient(Material.APPLE)
                .addIngredient(Material.GOLD_INGOT);

        FurnaceRecipe furnace = new FurnaceRecipe(
                new NamespacedKey("vessel", "smelt_iron"),
                new ItemStack(Material.IRON_INGOT, 1),
                Material.COBBLESTONE,
                0.7F,
                200);

        assertTrue(Bukkit.addRecipe(shaped));
        assertTrue(Bukkit.addRecipe(shapeless));
        assertTrue(Bukkit.addRecipe(furnace));

        assertEquals(3, shaped.getShape().length);
        assertEquals(Material.EMERALD, shaped.getIngredientMap().get('E'));
        assertEquals(2, shapeless.getIngredientList().size());
        assertEquals(Material.COBBLESTONE, furnace.getInput());
        assertEquals(0.7F, furnace.getExperience());
        assertEquals(200, furnace.getCookingTime());

        assertEquals(
                1,
                Bukkit.getRecipesFor(new ItemStack(Material.DIAMOND_SWORD, 1)).size());
        assertEquals(
                1,
                Bukkit.getRecipesFor(new ItemStack(Material.GOLDEN_APPLE, 1)).size());
        assertEquals(
                1,
                Bukkit.getRecipesFor(new ItemStack(Material.IRON_INGOT, 1)).size());

        World world = new VesselWorld("world");
        Location from = new Location(world, 0.0D, 64.0D, 0.0D);
        Location to = new Location(world, 10.0D, 65.0D, 10.0D);
        Player player = new Player("Shiroz", from);

        assertEquals(GameMode.SURVIVAL, player.getGameMode());
        player.setGameMode(GameMode.CREATIVE);
        assertEquals(GameMode.CREATIVE, player.getGameMode());

        assertEquals(20.0D, player.getHealth());
        player.setHealth(15.0D);
        assertEquals(15.0D, player.getHealth());

        assertEquals(20, player.getFoodLevel());
        player.setFoodLevel(18);
        assertEquals(18, player.getFoodLevel());

        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assertNotNull(maxHealthAttr);
        assertEquals(20.0D, maxHealthAttr.getDefaultValue());
        maxHealthAttr.setBaseValue(40.0D);
        assertEquals(40.0D, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        PlayerMoveEvent moveEvent = new PlayerMoveEvent(player, from, to);
        assertEquals(player, moveEvent.getPlayer());
        assertEquals(from, moveEvent.getFrom());
        assertEquals(to, moveEvent.getTo());
        assertFalse(moveEvent.isCancelled());
        moveEvent.setCancelled(true);
        assertTrue(moveEvent.isCancelled());

        PlayerTeleportEvent teleportEvent = new PlayerTeleportEvent(
                player,
                from,
                to,
                TeleportCause.ENDER_PEARL);
        assertEquals(TeleportCause.ENDER_PEARL, teleportEvent.getCause());
        assertEquals(from, teleportEvent.getFrom());
        assertEquals(to, teleportEvent.getTo());

        Bukkit.clearRecipes();
    }
}
