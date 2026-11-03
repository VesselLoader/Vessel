package org.bukkit.entity;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.Permission;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Runtime representation of a connected player entity ({@code org.bukkit.entity.Player}) on the
 * VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.4
 */
public final class Player extends Entity implements CommandSender {

    private final String name;
    private volatile String displayName;
    private volatile boolean op;
    private final List<String> receivedMessages = new CopyOnWriteArrayList<>();
    private final PlayerInventory inventory = new PlayerInventory();
    private final Map<String, Boolean> permissions = new ConcurrentHashMap<>();
    private final Map<PotionEffectType, PotionEffect> activePotions = new ConcurrentHashMap<>();
    private volatile GameMode gameMode = GameMode.SURVIVAL;
    private volatile double health = 20.0D;
    private volatile int foodLevel = 20;
    private final Map<Attribute, AttributeInstance> attributes = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code Player} with an explicit name, UUID, and initial location.
     *
     * @param name     player username
     * @param uniqueId player UUID
     * @param location initial spatial location
     */
    public Player(String name, UUID uniqueId, Location location) {
        super(uniqueId, EntityType.PLAYER, location);
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.displayName = name;
    }

    /**
     * Constructs a new {@code Player} with a deterministic UUID derived from its username.
     *
     * @param name     player username
     * @param location initial spatial location
     */
    public Player(String name, Location location) {
        this(
                Objects.requireNonNull(name, "name must not be null"),
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),
                location);
    }

    @Override
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
    }

    @Override
    public void sendMessage(String message) {
        if (message != null) {
            receivedMessages.add(message);
        }
    }

    /**
     * Returns an unmodifiable view of all chat/feedback messages sent to this player.
     *
     * @return received messages list
     */
    public List<String> getReceivedMessages() {
        return Collections.unmodifiableList(receivedMessages);
    }

    public void setPermission(String name, boolean value) {
        Objects.requireNonNull(name, "name must not be null");
        permissions.put(name.trim().toLowerCase(Locale.ROOT), value);
    }

    public void unsetPermission(String name) {
        if (name != null) {
            permissions.remove(name.trim().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public boolean isPermissionSet(String name) {
        return name != null && permissions.containsKey(name.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission == null) {
            return false;
        }
        Boolean explicit = permissions.get(permission.trim().toLowerCase(Locale.ROOT));
        return explicit != null ? explicit : true;
    }

    @Override
    public boolean hasPermission(Permission perm) {
        if (perm == null) {
            return false;
        }
        if (isPermissionSet(perm.getName())) {
            return hasPermission(perm.getName());
        }
        return perm.getDefault().getValue(this.op);
    }

    @Override
    public boolean isOp() {
        return op;
    }

    @Override
    public void setOp(boolean op) {
        this.op = op;
    }

    public boolean isOnline() {
        return true;
    }

    public PlayerInventory getInventory() {
        return inventory;
    }

    public void sendMessage(Component component) {
        if (component != null) {
            sendMessage(component.toPlainText());
        }
    }

    public Component displayName() {
        return Component.text(displayName);
    }

    public void displayName(Component component) {
        this.displayName = component != null ? component.toPlainText() : this.name;
    }

    public boolean addPotionEffect(PotionEffect effect) {
        Objects.requireNonNull(effect, "effect must not be null");
        activePotions.put(effect.getType(), effect);
        return true;
    }

    public void removePotionEffect(PotionEffectType type) {
        if (type != null) {
            activePotions.remove(type);
        }
    }

    public boolean hasPotionEffect(PotionEffectType type) {
        return type != null && activePotions.containsKey(type);
    }

    public PotionEffect getPotionEffect(PotionEffectType type) {
        return type != null ? activePotions.get(type) : null;
    }

    public Collection<PotionEffect> getActivePotionEffects() {
        return Collections.unmodifiableCollection(activePotions.values());
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode mode) {
        this.gameMode = Objects.requireNonNull(mode, "mode must not be null");
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0.0D, health);
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int value) {
        this.foodLevel = Math.max(0, value);
    }

    public AttributeInstance getAttribute(Attribute attribute) {
        Objects.requireNonNull(attribute, "attribute must not be null");
        AttributeInstance existing = attributes.get(attribute);
        if (existing == null) {
            AttributeInstance created = new AttributeInstance(attribute, attribute.getDefaultValue());
            existing = attributes.putIfAbsent(attribute, created);
            if (existing == null) {
                existing = created;
            }
        }
        return existing;
    }
}
