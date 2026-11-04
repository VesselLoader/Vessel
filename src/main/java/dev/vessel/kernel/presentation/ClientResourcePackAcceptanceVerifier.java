package dev.vessel.kernel.presentation;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Verifies client resource pack acceptance states (SUCCESSFULLY_LOADED, DECLINED, FAILED_DOWNLOAD).
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class ClientResourcePackAcceptanceVerifier {

    public static final String STATUS_SUCCESSFULLY_LOADED = "SUCCESSFULLY_LOADED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_FAILED_DOWNLOAD = "FAILED_DOWNLOAD";
    public static final String STATUS_ACCEPTED = "ACCEPTED";

    private final ConcurrentMap<UUID, String> clientPackStatuses = new ConcurrentHashMap<>();

    public void recordStatus(UUID clientUuid, String status) {
        Objects.requireNonNull(clientUuid, "clientUuid cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        clientPackStatuses.put(clientUuid, status);
    }

    public String getStatus(UUID clientUuid) {
        if (clientUuid == null) {
            return null;
        }
        return clientPackStatuses.get(clientUuid);
    }

    public boolean hasClientLoadedPack(UUID clientUuid) {
        if (clientUuid == null) {
            return false;
        }
        return STATUS_SUCCESSFULLY_LOADED.equals(clientPackStatuses.get(clientUuid));
    }

    public void clearStatus(UUID clientUuid) {
        if (clientUuid != null) {
            clientPackStatuses.remove(clientUuid);
        }
    }
}
