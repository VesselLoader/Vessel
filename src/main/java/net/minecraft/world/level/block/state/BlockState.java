package net.minecraft.world.level.block.state;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Minecraft runtime block state contract ({@code net.minecraft.world.level.block.state.BlockState}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class BlockState {

    private final String blockId;
    private final Map<String, String> properties;
    private final boolean air;
    private final boolean solid;

    /**
     * Constructs a new {@code BlockState} with an identifier and property map.
     *
     * @param blockId    namespaced block ID (e.g. "minecraft:stone")
     * @param properties block state property map
     * @param air        whether this block state is air
     * @param solid      whether this block state is solid
     */
    public BlockState(String blockId, Map<String, String> properties, boolean air, boolean solid) {
        this.blockId = Objects.requireNonNull(blockId, "blockId must not be null").trim();
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
        this.air = air;
        this.solid = solid;
    }

    /**
     * Constructs a solid non-air {@code BlockState} with no additional properties.
     *
     * @param blockId namespaced block ID
     */
    public BlockState(String blockId) {
        this(blockId, Map.of(), "minecraft:air".equals(blockId) || "air".equals(blockId), true);
    }

    public String getBlockId() {
        return blockId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public boolean isAir() {
        return air;
    }

    public boolean isSolid() {
        return solid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockState that = (BlockState) o;
        return air == that.air && solid == that.solid && blockId.equals(that.blockId) && properties.equals(that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockId, properties, air, solid);
    }

    @Override
    public String toString() {
        return "BlockState{" + blockId + properties + "}";
    }
}
