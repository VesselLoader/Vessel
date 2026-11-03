package net.fabricmc.fabric.api.networking.v1;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canonical Fabric Networking API v1 play-phase packet dispatch context
 * ({@code net.fabricmc.fabric.api.networking.v1.PlayPayloadContext}).
 *
 * <p>Exposes the sending player's identity and records outbound response payloads.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayPayloadContext {

    private final UUID playerUuid;
    private final String playerName;
    private final List<CustomPayload> sentResponses = new CopyOnWriteArrayList<>();

    /**
     * Constructs a play-phase payload context for the specified player.
     *
     * @param playerUuid non-null player UUID
     * @param playerName non-null player username
     */
    public PlayPayloadContext(UUID playerUuid, String playerName) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.playerName = Objects.requireNonNull(playerName, "playerName must not be null");
    }

    /**
     * Returns the UUID of the player associated with this packet context.
     *
     * @return player UUID
     */
    public UUID playerUuid() {
        return playerUuid;
    }

    /**
     * Returns the username of the player associated with this packet context.
     *
     * @return player username
     */
    public String playerName() {
        return playerName;
    }

    /**
     * Queues a response {@link CustomPayload} to be transmitted back to the player.
     *
     * @param response non-null response payload
     */
    public void sendResponse(CustomPayload response) {
        Objects.requireNonNull(response, "response must not be null");
        sentResponses.add(response);
    }

    /**
     * Returns an unmodifiable view of all response payloads queued during packet handling.
     *
     * @return unmodifiable list of sent response payloads
     */
    public List<CustomPayload> getSentResponses() {
        return Collections.unmodifiableList(sentResponses);
    }
}
