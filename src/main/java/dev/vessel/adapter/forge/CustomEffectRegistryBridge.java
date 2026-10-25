package dev.vessel.adapter.forge;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges custom modded Enchantments and MobEffects (PotionEffects) into the Bukkit
 * registry view so plugins (EssentialsX, Vault, WorldGuard, ShopGUI+) can inspect
 * and apply modded effects without throwing {@code IllegalArgumentException}.
 *
 * @version 1.0.0
 */
public final class CustomEffectRegistryBridge {

    private final Map<String, Integer> bridgedEnchantments = new ConcurrentHashMap<>();
    private final Map<String, Boolean> bridgedPotionEffects = new ConcurrentHashMap<>();

    /**
     * Creates a new custom enchantment and potion effect registry bridge.
     */
    public CustomEffectRegistryBridge() {
    }

    /**
     * Registers a modded enchantment into the bridged Bukkit enchantment registry.
     *
     * @param namespacedKey canonical namespaced enchantment key (e.g., {@code "apotheosis:capturing"})
     * @param maxLevel      maximum enchantment level
     */
    public void registerModdedEnchantment(String namespacedKey, int maxLevel) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        if (maxLevel < 1) {
            throw new IllegalArgumentException("maxLevel must be >= 1");
        }
        bridgedEnchantments.put(namespacedKey.trim().toLowerCase(Locale.ROOT), maxLevel);
    }

    /**
     * Registers a modded potion effect (MobEffect) into the bridged Bukkit potion effect registry.
     *
     * @param namespacedKey canonical namespaced effect key (e.g., {@code "ars_nouveau:mana_regen"})
     * @param isBeneficial  whether the effect is beneficial to the entity
     */
    public void registerModdedPotionEffect(String namespacedKey, boolean isBeneficial) {
        Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        bridgedPotionEffects.put(namespacedKey.trim().toLowerCase(Locale.ROOT), isBeneficial);
    }

    /**
     * Checks whether the specified namespaced enchantment key has been bridged to Bukkit.
     *
     * @param namespacedKey namespaced enchantment key
     * @return {@code true} if registered
     */
    public boolean isRegisteredBukkitEnchantment(String namespacedKey) {
        if (namespacedKey == null || namespacedKey.isBlank()) {
            return false;
        }
        return bridgedEnchantments.containsKey(namespacedKey.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Checks whether the specified namespaced potion effect key has been bridged to Bukkit.
     *
     * @param namespacedKey namespaced potion effect key
     * @return {@code true} if registered
     */
    public boolean isRegisteredBukkitPotionEffect(String namespacedKey) {
        if (namespacedKey == null || namespacedKey.isBlank()) {
            return false;
        }
        return bridgedPotionEffects.containsKey(namespacedKey.trim().toLowerCase(Locale.ROOT));
    }
}
