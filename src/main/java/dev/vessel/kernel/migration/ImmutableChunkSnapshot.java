package dev.vessel.kernel.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of complete chunk data captured at a precise tick instant.
 * <p>
 * Decouples disk serialization and compression from server tick execution,
 * completely eliminating TPS drop and chunk tearing during autosave.
 * </p>
 * <p>
 * Mitigates: <b>ERR-94</b>, <b>ERR-100</b>, <b>STO-021..050</b>.
 * </p>
 *
 * @param chunkX           chunk X coordinate
 * @param chunkZ           chunk Z coordinate
 * @param dimension        dimension namespaced identifier (e.g. "minecraft:overworld")
 * @param worldTick        world tick timestamp at capture
 * @param sections         immutable list of section snapshots
 * @param blockEntitiesNbt map of tile-entity coordinate keys to serialized NBT payloads
 * @param entitiesNbt      list of serialized entity NBT payloads
 * @param heightmaps       bit-packed heightmap array
 * @param captureMode      capture mode that produced this snapshot
 * @param snapshotTimeMs   wall clock capture timestamp
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ImmutableChunkSnapshot(
        int chunkX,
        int chunkZ,
        String dimension,
        long worldTick,
        List<ChunkSectionSnapshot> sections,
        Map<String, byte[]> blockEntitiesNbt,
        List<byte[]> entitiesNbt,
        long[] heightmaps,
        ChunkSnapshotCaptureMode captureMode,
        long snapshotTimeMs
) {

    /**
     * Compact constructor validating invariants and creating deep immutable snapshots.
     */
    public ImmutableChunkSnapshot {
        Objects.requireNonNull(dimension, "dimension cannot be null");
        Objects.requireNonNull(sections, "sections cannot be null");
        Objects.requireNonNull(blockEntitiesNbt, "blockEntitiesNbt cannot be null");
        Objects.requireNonNull(entitiesNbt, "entitiesNbt cannot be null");
        Objects.requireNonNull(captureMode, "captureMode cannot be null");

        sections = List.copyOf(sections);

        Map<String, byte[]> clonedBlockEntities = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : blockEntitiesNbt.entrySet()) {
            clonedBlockEntities.put(entry.getKey(), entry.getValue() != null ? entry.getValue().clone() : new byte[0]);
        }
        blockEntitiesNbt = Collections.unmodifiableMap(clonedBlockEntities);

        List<byte[]> clonedEntities = new ArrayList<>(entitiesNbt.size());
        for (byte[] entityBytes : entitiesNbt) {
            clonedEntities.add(entityBytes != null ? entityBytes.clone() : new byte[0]);
        }
        entitiesNbt = Collections.unmodifiableList(clonedEntities);

        heightmaps = heightmaps != null ? heightmaps.clone() : new long[0];
    }

    /**
     * Returns a unique coordinate key for this chunk in "dimension:chunkX:chunkZ" format.
     *
     * @return unique spatial string key
     */
    public String chunkKey() {
        return dimension + ":" + chunkX + ":" + chunkZ;
    }
}
