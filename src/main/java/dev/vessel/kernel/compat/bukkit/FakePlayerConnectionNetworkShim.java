package dev.vessel.kernel.compat.bukkit;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides a synthetic no-op network connection shim for NPC fake players (Citizens2, Sentinel, Carpet)
 * preventing NullPointerExceptions during packet broadcast.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class FakePlayerConnectionNetworkShim {

    private final ConcurrentMap<UUID, Object> activeFakeConnections = new ConcurrentHashMap<>();

    public void registerFakeConnection(UUID npcUuid, Object connectionInstance) {
        Objects.requireNonNull(npcUuid, "npcUuid cannot be null");
        Objects.requireNonNull(connectionInstance, "connectionInstance cannot be null");
        activeFakeConnections.put(npcUuid, connectionInstance);
    }

    public boolean isFakePlayer(UUID entityUuid) {
        return entityUuid != null && activeFakeConnections.containsKey(entityUuid);
    }

    public boolean safelyHandleOutboundPacket(UUID npcUuid, Object packetInstance) {
        if (!isFakePlayer(npcUuid)) {
            return false;
        }
        // Safely swallow outbound packet for fake NPC
        return true;
    }

    public void unregisterFakeConnection(UUID npcUuid) {
        if (npcUuid != null) {
            activeFakeConnections.remove(npcUuid);
        }
    }

    public int getActiveNpcCount() {
        return activeFakeConnections.size();
    }
}
