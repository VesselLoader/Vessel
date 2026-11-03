package org.bukkit;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

/**
 * Runtime representation of a Minecraft world/dimension ({@code org.bukkit.World}) on the VESSEL
 * Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.3
 */
public final class World {

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
    private final WorldBorder worldBorder = new WorldBorder();

    /**
     * Constructs a new {@code World} with an explicit name and unique identifier.
     *
     * @param name world name
     * @param uid  world UUID
     */
    public World(String name, UUID uid) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.uid = Objects.requireNonNull(uid, "uid must not be null");
    }

    /**
     * Constructs a new {@code World} with a deterministic UUID derived from its name.
     *
     * @param name world name
     */
    public World(String name) {
        this(
                Objects.requireNonNull(name, "name must not be null"),
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Returns the name of this world.
     *
     * @return world name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the unique identifier of this world.
     *
     * @return world UUID
     */
    public UUID getUID() {
        return uid;
    }

    /**
     * Returns the relative in-game daytime of this world (`0..24000`).
     *
     * @return world time in ticks
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the relative in-game daytime of this world.
     *
     * @param time world time in ticks
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * Returns the minimum build height of this world.
     *
     * @return minimum build height (`-64`)
     */
    public int getMinHeight() {
        return -64;
    }

    /**
     * Returns the maximum build height of this world.
     *
     * @return maximum build height (`320`)
     */
    public int getMaxHeight() {
        return 320;
    }

    /**
     * Returns the {@link Block} at the specified coordinates, initializing a default
     * {@code "minecraft:stone"} block state if not previously accessed.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return block handle at {@code (x, y, z)}
     */
    public Block getBlockAt(int x, int y, int z) {
        String key = x + ":" + y + ":" + z;
        return modifiedBlocks.computeIfAbsent(key, k -> new Block(this, x, y, z, "minecraft:stone"));
    }

    public void playSound(Location location, Sound sound, float volume, float pitch) {
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(sound, "sound must not be null");
        playedSoundLog.add(
                sound.name() + "@" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
    }

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

    public List<String> getPlayedSoundLog() {
        return Collections.unmodifiableList(playedSoundLog);
    }

    public List<String> getSpawnedParticleLog() {
        return Collections.unmodifiableList(spawnedParticleLog);
    }

    public Chunk getChunkAt(int x, int z) {
        String key = x + ":" + z;
        Chunk existing = loadedChunks.get(key);
        if (existing == null) {
            Chunk created = new Chunk(this, x, z);
            existing = loadedChunks.putIfAbsent(key, created);
            if (existing == null) {
                existing = created;
            }
        }
        return existing;
    }

    public Entity spawnEntity(Location location, EntityType type) {
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Entity entity = new Entity(UUID.randomUUID(), type, location);
        entities.add(entity);
        return entity;
    }

    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    public boolean hasStorm() {
        return storm;
    }

    public void setStorm(boolean hasStorm) {
        this.storm = hasStorm;
    }

    public boolean isThundering() {
        return thundering;
    }

    public void setThundering(boolean thundering) {
        this.thundering = thundering;
    }

    public WorldBorder getWorldBorder() {
        return worldBorder;
    }
}
