package dev.vessel.bootstrap.wave6;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 6 verifying Bukkit {@link Enchantment},
 * {@link org.bukkit.persistence.PersistentDataContainer}, {@link PersistentDataType},
 * {@link ItemMeta} (`v1.0.1`), {@link World} (`v1.0.1`) sound and particle logs,
 * {@link PlayerInteractEvent}, and {@link EntityDamageEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveSixBukkitEnchantPersistentDataAndCombatTest {

    @Test
    void testItemMetaEnchantmentsPersistentDataWorldSoundParticleAndEvents() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        ItemMeta meta = sword.getItemMeta();
        assertNotNull(meta);

        assertTrue(meta.addEnchant(Enchantment.SHARPNESS, 5, false));
        assertFalse(meta.addEnchant(Enchantment.SHARPNESS, 10, false));
        NamespacedKey soulKey = new NamespacedKey("vessel", "soulbound");
        meta.getPersistentDataContainer().set(soulKey, PersistentDataType.INTEGER, 1);
        sword.setItemMeta(meta);

        assertTrue(sword.getItemMeta().hasEnchants());
        assertTrue(sword.getItemMeta().hasEnchant(Enchantment.SHARPNESS));
        assertEquals(5, sword.getItemMeta().getEnchantLevel(Enchantment.SHARPNESS));
        assertTrue(sword.getItemMeta().getPersistentDataContainer().has(soulKey));
        assertTrue(
                sword.getItemMeta()
                        .getPersistentDataContainer()
                        .has(soulKey, PersistentDataType.INTEGER));
        assertEquals(
                1,
                sword.getItemMeta()
                        .getPersistentDataContainer()
                        .get(soulKey, PersistentDataType.INTEGER));

        World world = new World("world");
        Location loc = new Location(world, 10, 64, 10);
        Player player = new Player("Shiroz", loc);

        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        world.spawnParticle(Particle.FLAME, loc, 20);
        assertEquals(1, world.getPlayedSoundLog().size());
        assertEquals(1, world.getSpawnedParticleLog().size());
        assertEquals("ENTITY_PLAYER_LEVELUP@10,64,10", world.getPlayedSoundLog().get(0));
        assertEquals("FLAMEx20@10,64,10", world.getSpawnedParticleLog().get(0));

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(
                player,
                Action.RIGHT_CLICK_BLOCK,
                sword,
                world.getBlockAt(10, 64, 10));
        assertEquals(player, interactEvent.getPlayer());
        assertEquals(Action.RIGHT_CLICK_BLOCK, interactEvent.getAction());
        assertTrue(interactEvent.hasItem());
        assertEquals(sword, interactEvent.getItem());
        assertTrue(interactEvent.hasBlock());
        assertNotNull(interactEvent.getClickedBlock());
        assertFalse(interactEvent.isCancelled());
        interactEvent.setCancelled(true);
        assertTrue(interactEvent.isCancelled());

        EntityDamageEvent damageEvent = new EntityDamageEvent(
                player.getUniqueId(),
                DamageCause.ENTITY_ATTACK,
                7.5D);
        assertEquals(player.getUniqueId(), damageEvent.getEntityUuid());
        assertEquals(DamageCause.ENTITY_ATTACK, damageEvent.getCause());
        assertEquals(7.5D, damageEvent.getDamage());
        assertFalse(damageEvent.isCancelled());
        damageEvent.setDamage(4.0D);
        damageEvent.setCancelled(true);
        assertEquals(4.0D, damageEvent.getDamage());
        assertTrue(damageEvent.isCancelled());
    }
}
