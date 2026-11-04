package net.minecraftforge.entity;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Universal multi-era capability interface for Forge entities that synchronize custom network
 * spawn data beyond vanilla trackers.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface IEntityAdditionalSpawnData {

    /**
     * Called on server when entity is spawned to write custom payload to the client.
     *
     * @param buffer network byte buffer
     */
    void writeSpawnData(FriendlyByteBuf buffer);

    /**
     * Called on client when entity packet is received to reconstruct custom payload.
     *
     * @param additionalData network byte buffer
     */
    void readSpawnData(FriendlyByteBuf additionalData);
}
