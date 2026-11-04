package org.bukkit;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

/**
 * Standard Bukkit {@code World} contract representing a Minecraft world/dimension.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.7
 */
public interface World {

    String getName();

    UUID getUID();

    File getWorldFolder();

    NamespacedKey getKey();

    Location getSpawnLocation();

    int getSeaLevel();

    long getTime();

    void setTime(long time);

    int getMinHeight();

    int getMaxHeight();

    Block getBlockAt(int x, int y, int z);

    void playSound(Location location, Sound sound, float volume, float pitch);

    void spawnParticle(Particle particle, Location location, int count);

    List<String> getPlayedSoundLog();

    List<String> getSpawnedParticleLog();

    Chunk getChunkAt(int x, int z);

    Chunk getChunkAt(int x, int z, boolean generate);

    CompletableFuture<Chunk> getChunkAtAsync(int x, int z);

    default CompletableFuture<Chunk> getChunkAtAsync(int x, int z, boolean generate) {
        return getChunkAtAsync(x, z);
    }

    boolean isChunkGenerated(int x, int z);

    boolean addPluginChunkTicket(int x, int z, Plugin plugin);

    boolean removePluginChunkTicket(int x, int z, Plugin plugin);

    Entity spawnEntity(Location location, EntityType type);

    List<Entity> getEntities();

    Difficulty getDifficulty();

    void setDifficulty(Difficulty difficulty);

    Environment getEnvironment();

    void setEnvironment(Environment environment);

    boolean hasStorm();

    void setStorm(boolean hasStorm);

    boolean isThundering();

    void setThundering(boolean thundering);

    WorldBorder getWorldBorder();
}
