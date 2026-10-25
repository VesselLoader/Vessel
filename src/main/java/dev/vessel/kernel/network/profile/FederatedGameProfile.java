package dev.vessel.kernel.network.profile;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable federated game profile representing a player session across Java and Bedrock clients.
 *
 * @param id         the unique identifier of the player
 * @param name       the player's username
 * @param properties list of associated profile properties (skins, capes)
 * @param isBedrock  whether this profile originated from Bedrock/Floodgate
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FederatedGameProfile(
        UUID id,
        String name,
        List<ProfileProperty> properties,
        boolean isBedrock
) {

    /**
     * Compact constructor validating non-null requirements and unmodifiable list creation.
     */
    public FederatedGameProfile {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        properties = properties != null ? List.copyOf(properties) : Collections.emptyList();
    }

    /**
     * Creates a simple federated profile without properties.
     *
     * @param id        player UUID
     * @param name      player name
     * @param isBedrock whether Bedrock
     * @return new federated game profile
     */
    public static FederatedGameProfile of(UUID id, String name, boolean isBedrock) {
        return new FederatedGameProfile(id, name, Collections.emptyList(), isBedrock);
    }
}
