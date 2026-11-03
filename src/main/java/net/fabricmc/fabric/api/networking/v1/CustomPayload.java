package net.fabricmc.fabric.api.networking.v1;

/**
 * Canonical Fabric Networking API v1 typed custom packet payload contract
 * ({@code net.fabricmc.fabric.api.networking.v1.CustomPayload}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface CustomPayload {

    /**
     * Returns the typed {@link CustomPayloadId} identifying this payload's channel.
     *
     * @return non-null payload identifier
     */
    CustomPayloadId<? extends CustomPayload> getId();

    /**
     * Encodes this custom payload into its raw wire byte representation.
     *
     * @return encoded payload bytes (defaults to an empty array)
     */
    default byte[] encodePayloadBytes() {
        return new byte[0];
    }
}
