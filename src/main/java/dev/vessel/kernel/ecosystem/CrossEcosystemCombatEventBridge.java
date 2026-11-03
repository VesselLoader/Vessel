package dev.vessel.kernel.ecosystem;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Universal cross-ecosystem bridge that synchronizes combat damage calculation and entity death
 * across Forge, NeoForge, and Bukkit.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossEcosystemCombatEventBridge {

    public CrossEcosystemCombatEventBridge() {
    }

    /**
     * Dispatches combat damage across ecosystems.
     *
     * @param target       Bukkit entity receiving damage
     * @param damageSource canonical damage source identifier
     * @param rawDamage    initial damage value
     * @return final calculated damage, or 0.0 if cancelled
     */
    public double dispatchCombatDamage(Entity target, String damageSource, double rawDamage) {
        Objects.requireNonNull(target, "target must not be null");
        UUID uuid = target.getUniqueId();

        // 1. Forge LivingHurtEvent
        LivingHurtEvent forgeHurt = new LivingHurtEvent(uuid, damageSource, (float) rawDamage);
        MinecraftForge.EVENT_BUS.post(forgeHurt);
        if (forgeHurt.isCanceled()) {
            return 0.0;
        }
        float damage = forgeHurt.getAmount();

        // 2. NeoForge LivingIncomingDamageEvent
        LivingIncomingDamageEvent neoHurt = new LivingIncomingDamageEvent(uuid, damageSource, damage);
        NeoForge.EVENT_BUS.post(neoHurt);
        if (neoHurt.isCanceled()) {
            return 0.0;
        }
        damage = neoHurt.getAmount();

        // 3. Bukkit EntityDamageEvent
        EntityDamageEvent bukkitDamage = new EntityDamageEvent(uuid, DamageCause.ENTITY_ATTACK, damage);
        Bukkit.getPluginManager().callEvent(bukkitDamage);
        if (bukkitDamage.isCancelled()) {
            return 0.0;
        }

        return bukkitDamage.getDamage();
    }

    /**
     * Dispatches entity death across ecosystems.
     *
     * @param target       dying Bukkit entity
     * @param damageSource damage source causing death
     * @param drops        loot drops
     * @param exp          dropped experience points
     */
    public void dispatchEntityDeath(Entity target, String damageSource, List<ItemStack> drops, int exp) {
        Objects.requireNonNull(target, "target must not be null");
        UUID uuid = target.getUniqueId();

        // Forge LivingDeathEvent
        LivingDeathEvent forgeDeath = new LivingDeathEvent(uuid, damageSource);
        MinecraftForge.EVENT_BUS.post(forgeDeath);

        // NeoForge LivingDeathEvent
        net.neoforged.neoforge.event.entity.living.LivingDeathEvent neoDeath =
                new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(uuid, damageSource);
        NeoForge.EVENT_BUS.post(neoDeath);

        // Bukkit EntityDeathEvent
        EntityDeathEvent bukkitDeath = new EntityDeathEvent(target, drops, exp);
        Bukkit.getPluginManager().callEvent(bukkitDeath);
    }
}
