package dev.vessel.kernel.compat.network;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Bridge mapping legacy Forge 1.12 SPacketUpdateTileEntity to modern ClientboundBlockEntityDataPacket.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class LegacyTileEntityPacketBridge {

    public Object createBlockEntityUpdatePacket(Object blockPosInstance, int packetType, Object compoundTag, ClassLoader loader) {
        Objects.requireNonNull(blockPosInstance, "blockPosInstance cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        try {
            Class<?> modernPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket", true, loader);
            // Search for factory method create(BlockEntity) or constructor
            for (Method m : modernPacketClass.getMethods()) {
                if ("create".equals(m.getName()) && m.getParameterCount() >= 1) {
                    return m;
                }
            }
        } catch (ClassNotFoundException ignored) {
            return null;
        }
        return null;
    }

    public boolean isLegacyTileEntityPacket(String packetClassName) {
        if (packetClassName == null) {
            return false;
        }
        return packetClassName.contains("SPacketUpdateTileEntity")
                || packetClassName.contains("S35PacketUpdateTileEntity");
    }

    public String resolveModernPacketName(String legacyPacketName) {
        if (isLegacyTileEntityPacket(legacyPacketName)) {
            return "net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket";
        }
        return legacyPacketName;
    }
}
