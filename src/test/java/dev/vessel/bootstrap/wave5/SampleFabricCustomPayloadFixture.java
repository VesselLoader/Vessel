package dev.vessel.bootstrap.wave5;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.CustomPayload;
import net.fabricmc.fabric.api.networking.v1.CustomPayloadId;

/**
 * Sample Fabric {@link CustomPayload} record fixture used in Wave 5 networking tests.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param message UTF-8 payload string
 * @version 1.0.0
 */
public record SampleFabricCustomPayloadFixture(String message) implements CustomPayload {

    public static final CustomPayloadId<SampleFabricCustomPayloadFixture> ID =
            CustomPayloadId.of("vessel:wave5_ping");

    public SampleFabricCustomPayloadFixture {
        Objects.requireNonNull(message, "message must not be null");
    }

    @Override
    public CustomPayloadId<? extends CustomPayload> getId() {
        return ID;
    }

    @Override
    public byte[] encodePayloadBytes() {
        return message.getBytes(StandardCharsets.UTF_8);
    }
}
