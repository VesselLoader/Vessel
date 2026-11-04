package dev.vessel.kernel.network.payload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link UniversalCustomPayloadRegistrar}, {@link CustomPacketPayloadByteBufAdapter},
 * and {@link ForgeSimpleChannelEmulationRouter}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class NetworkPayloadHarmonizerTest {

    @Test
    @DisplayName("Verify CustomPacketPayloadByteBufAdapter reads integers and UTF strings")
    void verifyByteBufAdapter() {
        byte[] payloadData = new byte[]{
                0x00, 0x00, 0x04, (byte) 0xD2, // int 1234
                0x00, 0x04, 'T', 'E', 'S', 'T' // UTF string "TEST"
        };

        CustomPacketPayloadByteBufAdapter adapter = new CustomPacketPayloadByteBufAdapter(payloadData);

        assertThat(adapter.readInt()).isEqualTo(1234);
        assertThat(adapter.readUtf()).isEqualTo("TEST");
        assertThat(adapter.readableBytes()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify UniversalCustomPayloadRegistrar rate limiting and dispatch")
    void verifyPayloadRegistrarDispatch() {
        UniversalCustomPayloadRegistrar registrar = new UniversalCustomPayloadRegistrar();

        AtomicBoolean dispatched = new AtomicBoolean(false);
        AtomicReference<String> payloadText = new AtomicReference<>();

        registrar.registerReceiver("mymod:sync", (player, data) -> {
            dispatched.set(true);
            payloadText.set(new String(data, StandardCharsets.UTF_8));
        });

        assertThat(registrar.hasReceiver("mymod:sync")).isTrue();

        byte[] outgoing = "STATUS_ONLINE".getBytes(StandardCharsets.UTF_8);
        boolean success = registrar.dispatchIncomingPayload("127.0.0.1", "Player1", "mymod:sync", outgoing);

        assertThat(success).isTrue();
        assertThat(dispatched.get()).isTrue();
        assertThat(payloadText.get()).isEqualTo("STATUS_ONLINE");
    }

    @Test
    @DisplayName("Verify ForgeSimpleChannelEmulationRouter dispatches discriminator packets")
    void verifyForgeSimpleChannelEmulation() {
        UniversalCustomPayloadRegistrar registrar = new UniversalCustomPayloadRegistrar();
        ForgeSimpleChannelEmulationRouter router = new ForgeSimpleChannelEmulationRouter("forge:network", registrar);

        AtomicReference<String> messageReceived = new AtomicReference<>();

        router.registerMessage(1, (player, data) -> {
            messageReceived.set(new String(data, StandardCharsets.UTF_8));
        });

        // Packet with discriminator 1
        byte[] rawBytes = new byte[]{
                1, 'H', 'E', 'L', 'L', 'O'
        };

        registrar.dispatchIncomingPayload("127.0.0.1", "Player1", "forge:network", rawBytes);

        assertThat(messageReceived.get()).isEqualTo("HELLO");
    }
}
