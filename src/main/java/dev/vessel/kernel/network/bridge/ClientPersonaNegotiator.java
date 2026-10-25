package dev.vessel.kernel.network.bridge;

import java.util.Objects;
import java.util.Set;

/**
 * Negotiates the client network persona during login handshake (Decision 11) so the server knows
 * whether to stream raw modded packets ({@code MODDED_NATIVE}), virtualized Display Entity +
 * resource pack states ({@code VANILLA_DISPLAY_ENTITY}), or Bedrock RakNet packets
 * ({@code BEDROCK_RAKNET}).
 *
 * @version 1.0.0
 */
public final class ClientPersonaNegotiator {

    /**
     * Determines the canonical client persona for a connecting player session.
     *
     * @param clientInstalledModChannels the set of custom mod channels advertised by the client
     * @param isBedrockSession           whether the player connected through the Bedrock RakNet bridge
     * @return {@code "BEDROCK_RAKNET"}, {@code "MODDED_NATIVE"}, or
     *         {@code "VANILLA_DISPLAY_ENTITY"}
     */
    public String negotiateClientPersona(
            Set<String> clientInstalledModChannels,
            boolean isBedrockSession) {
        Objects.requireNonNull(
                clientInstalledModChannels, "clientInstalledModChannels must not be null");
        if (isBedrockSession) {
            return "BEDROCK_RAKNET";
        }
        for (String channel : clientInstalledModChannels) {
            if (channel != null
                    && !channel.isBlank()
                    && !channel.startsWith("minecraft:")) {
                return "MODDED_NATIVE";
            }
        }
        return "VANILLA_DISPLAY_ENTITY";
    }
}
