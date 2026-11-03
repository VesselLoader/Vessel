package net.minecraft.server.level;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;

/**
 * Server-side Minecraft player entity representation ({@code net.minecraft.server.level.ServerPlayer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ServerPlayer {

    private final UUID uuid;
    private final String name;
    private volatile ServerLevel level;
    private volatile BlockPos blockPos;

    /**
     * Constructs a new {@code ServerPlayer} with UUID, name, level, and position.
     *
     * @param uuid     player UUID
     * @param name     player username
     * @param level    active server level
     * @param blockPos block position
     */
    public ServerPlayer(UUID uuid, String name, ServerLevel level, BlockPos blockPos) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.level = level;
        this.blockPos = blockPos != null ? blockPos : new BlockPos(0, 64, 0);
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getGameProfileName() {
        return name;
    }

    public ServerLevel serverLevel() {
        return level;
    }

    public void setServerLevel(ServerLevel level) {
        this.level = level;
    }

    public BlockPos blockPosition() {
        return blockPos;
    }

    public void setBlockPosition(BlockPos pos) {
        if (pos != null) {
            this.blockPos = pos;
        }
    }
}
