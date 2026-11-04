package org.bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

/**
 * Sovereign implementation of Bukkit's {@link World} representing a Minecraft dimension.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public class VesselWorld implements World {

    private final String name;
    private final UUID uid;
    private volatile long time;
    private final Map<String, Block> modifiedBlocks = new ConcurrentHashMap<>();
    private final List<String> playedSoundLog = new CopyOnWriteArrayList<>();
    private final List<String> spawnedParticleLog = new CopyOnWriteArrayList<>();
    private final Map<String, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final List<Entity> entities = new CopyOnWriteArrayList<>();
    private volatile Difficulty difficulty = Difficulty.NORMAL;
    private volatile Environment environment = Environment.NORMAL;
    private volatile boolean storm;
    private volatile boolean thundering;
    private final WorldBorder worldBorder = new VesselWorldBorder();
    private final dev.vessel.kernel.world.VesselPhysicalChunkIoBridge chunkIoBridge =
            new dev.vessel.kernel.world.VesselPhysicalChunkIoBridge();

    public VesselWorld(String name, UUID uid) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.uid = Objects.requireNonNull(uid, "uid must not be null");
    }

    public VesselWorld(String name) {
        this(
                Objects.requireNonNull(name, "name must not be null"),
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUID() {
        return uid;
    }

    @Override
    public File getWorldFolder() {
        return new File(name);
    }

    @Override
    public NamespacedKey getKey() {
        return NamespacedKey.minecraft(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public Location getSpawnLocation() {
        return new Location(this, 0.0D, 64.0D, 0.0D);
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public int getMinHeight() {
        return -64;
    }

    @Override
    public int getMaxHeight() {
        return 320;
    }

    @Override
    public Block getBlockAt(int x, int y, int z) {
        String key = x + ":" + y + ":" + z;
        return modifiedBlocks.computeIfAbsent(key, k -> new Block(this, x, y, z, "minecraft:stone"));
    }

    @Override
    public void playSound(Location location, Sound sound, float volume, float pitch) {
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(sound, "sound must not be null");
        playedSoundLog.add(
                sound.name() + "@" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
    }

    @Override
    public void spawnParticle(Particle particle, Location location, int count) {
        Objects.requireNonNull(particle, "particle must not be null");
        Objects.requireNonNull(location, "location must not be null");
        spawnedParticleLog.add(
                particle.name()
                        + "x"
                        + count
                        + "@"
                        + location.getBlockX()
                        + ","
                        + location.getBlockY()
                        + ","
                        + location.getBlockZ());
    }

    @Override
    public List<String> getPlayedSoundLog() {
        return Collections.unmodifiableList(playedSoundLog);
    }

    @Override
    public List<String> getSpawnedParticleLog() {
        return Collections.unmodifiableList(spawnedParticleLog);
    }

    @Override
    public Chunk getChunkAt(int x, int z) {
        String key = x + ":" + z;
        Chunk existing = loadedChunks.get(key);
        if (existing == null) {
            Chunk created = new VesselChunk(this, x, z);
            existing = loadedChunks.putIfAbsent(key, created);
            if (existing == null) {
                existing = created;
            }
        }
        return existing;
    }

    @Override
    public Chunk getChunkAt(int x, int z, boolean generate) {
        return getChunkAt(x, z);
    }

    @Override
    public CompletableFuture<Chunk> getChunkAtAsync(int x, int z) {
        return getChunkAtAsync(x, z, true);
    }

    @Override
    public CompletableFuture<Chunk> getChunkAtAsync(int x, int z, boolean generate) {
        return chunkIoBridge.loadPhysicalChunkAsync(this, x, z, generate);
    }

    @Override
    public boolean isChunkGenerated(int x, int z) {
        return true;
    }

    @Override
    public boolean addPluginChunkTicket(int x, int z, Plugin plugin) {
        return true;
    }

    @Override
    public boolean removePluginChunkTicket(int x, int z, Plugin plugin) {
        return true;
    }

    @Override
    public Entity spawnEntity(Location location, EntityType type) {
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Entity entity = new Entity(UUID.randomUUID(), type, location);
        entities.add(entity);
        return entity;
    }

    @Override
    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    @Override
    public Difficulty getDifficulty() {
        return difficulty;
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
    }

    @Override
    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    @Override
    public boolean hasStorm() {
        return storm;
    }

    @Override
    public void setStorm(boolean hasStorm) {
        this.storm = hasStorm;
    }

    @Override
    public boolean isThundering() {
        return thundering;
    }

    @Override
    public void setThundering(boolean thundering) {
        this.thundering = thundering;
    }

    @Override
    public WorldBorder getWorldBorder() {
        return worldBorder;
    }
}
