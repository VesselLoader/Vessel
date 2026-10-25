package dev.vessel.kernel.network.handshake;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Synthesizes instantaneous (< 0.1ms, 0 ticks) loopback client acknowledgment packets
 * to satisfy server-side modded state machines (FML, NeoForge, Fabric) without requiring
 * client-side mod support.
 *
 * <p>Responsibility: Generate synthetic handshake ACK packets.
 * Eliminates: ERR-13, NET-021..040.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SyntheticClientAckPacketFactory {

    /**
     * Canonical channel identifier for FML handshake packets.
     */
    public static final String FML_HANDSHAKE_CHANNEL = "fml:handshake";

    /**
     * Canonical channel identifier for NeoForge handshake packets.
     */
    public static final String NEOFORGE_HANDSHAKE_CHANNEL = "neoforge:handshake";

    /**
     * Canonical channel identifier for Fabric registry synchronization packets.
     */
    public static final String FABRIC_REGISTRY_SYNC_CHANNEL = "fabric:registry_sync";

    /**
     * Canonical channel identifier for Fabric channel registration packets.
     */
    public static final String FABRIC_CHANNEL_REGISTER_CHANNEL = "minecraft:register";

    /**
     * Discriminator byte for FML 2 client ACK: Mods received.
     */
    public static final byte FML_DISCRIMINATOR_MOD_LIST_ACK = 0x02;

    /**
     * Discriminator byte for FML 2 client ACK: Registries synchronized.
     */
    public static final byte FML_DISCRIMINATOR_REGISTRY_ACK = 0x03;

    /**
     * Discriminator byte for FML 2 client ACK: Handshake complete / connection accepted.
     */
    public static final byte FML_DISCRIMINATOR_COMPLETE_ACK = -0x01; // 0xFF

    /**
     * Discriminator byte for NeoForge handshake ACK.
     */
    public static final byte NEOFORGE_DISCRIMINATOR_ACK = 0x01;

    /**
     * Discriminator byte for Fabric registry sync complete.
     */
    public static final byte FABRIC_DISCRIMINATOR_SYNC_COMPLETE = 0x01;

    /**
     * Default transaction token used when no specific token is provided.
     */
    public static final int DEFAULT_TRANSACTION_TOKEN = 1;

    /**
     * Constructs a new SyntheticClientAckPacketFactory.
     */
    public SyntheticClientAckPacketFactory() {
    }

    /**
     * Synthesizes an FML mod list receipt acknowledgment packet.
     *
     * @param transactionToken the sequence or token identifier
     * @return synthetic ACK response
     */
    public SyntheticAckResponse createFmlModListAck(int transactionToken) {
        long startNanos = System.nanoTime();
        ByteBuffer buf = ByteBuffer.allocate(5);
        buf.put(FML_DISCRIMINATOR_MOD_LIST_ACK);
        buf.putInt(transactionToken);
        long duration = System.nanoTime() - startNanos;
        return new SyntheticAckResponse(
                FML_HANDSHAKE_CHANNEL,
                buf.array(),
                SyntheticHandshakePhase.FML_HANDSHAKE_RECEIVED_MODS,
                duration
        );
    }

    /**
     * Synthesizes an FML registry synchronization receipt acknowledgment packet.
     *
     * @param transactionToken the sequence or token identifier
     * @return synthetic ACK response
     */
    public SyntheticAckResponse createFmlRegistrySyncAck(int transactionToken) {
        long startNanos = System.nanoTime();
        ByteBuffer buf = ByteBuffer.allocate(5);
        buf.put(FML_DISCRIMINATOR_REGISTRY_ACK);
        buf.putInt(transactionToken);
        long duration = System.nanoTime() - startNanos;
        return new SyntheticAckResponse(
                FML_HANDSHAKE_CHANNEL,
                buf.array(),
                SyntheticHandshakePhase.FML_HANDSHAKE_SYNCED_REGISTRIES,
                duration
        );
    }

    /**
     * Synthesizes an FML connection acknowledgment packet confirming handshake completion.
     *
     * @return synthetic ACK response
     */
    public SyntheticAckResponse createFmlConnectionAck() {
        long startNanos = System.nanoTime();
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(FML_DISCRIMINATOR_COMPLETE_ACK);
        long duration = System.nanoTime() - startNanos;
        return new SyntheticAckResponse(
                FML_HANDSHAKE_CHANNEL,
                buf.array(),
                SyntheticHandshakePhase.FML_HANDSHAKE_ACK_CONNECTION,
                duration
        );
    }

    /**
     * Synthesizes a NeoForge handshake acknowledgment packet.
     *
     * @param transactionToken the transaction or channel sequence token
     * @return synthetic ACK response
     */
    public SyntheticAckResponse createNeoForgeHandshakeAck(int transactionToken) {
        long startNanos = System.nanoTime();
        ByteBuffer buf = ByteBuffer.allocate(5);
        buf.put(NEOFORGE_DISCRIMINATOR_ACK);
        buf.putInt(transactionToken);
        long duration = System.nanoTime() - startNanos;
        return new SyntheticAckResponse(
                NEOFORGE_HANDSHAKE_CHANNEL,
                buf.array(),
                SyntheticHandshakePhase.NEOFORGE_HANDSHAKE_ACK,
                duration
        );
    }

    /**
     * Synthesizes a Fabric registry synchronization complete packet.
     *
     * @return synthetic ACK response
     */
    public SyntheticAckResponse createFabricRegistrySyncComplete() {
        long startNanos = System.nanoTime();
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(FABRIC_DISCRIMINATOR_SYNC_COMPLETE);
        long duration = System.nanoTime() - startNanos;
        return new SyntheticAckResponse(
                FABRIC_REGISTRY_SYNC_CHANNEL,
                buf.array(),
                SyntheticHandshakePhase.FABRIC_REGISTRY_SYNC_COMPLETE,
                duration
        );
    }

    /**
     * Synthesizes a Fabric channel registration acknowledgment packet.
     *
     * @param channels comma-separated list of recognized channels
     * @return synthetic ACK response
     */
    public SyntheticAckResponse createFabricChannelRegisterAck(String channels) {
        Objects.requireNonNull(channels, "channels must not be null");
        long startNanos = System.nanoTime();
        byte[] channelBytes = channels.getBytes(StandardCharsets.UTF_8);
        long duration = System.nanoTime() - startNanos;
        return new SyntheticAckResponse(
                FABRIC_CHANNEL_REGISTER_CHANNEL,
                channelBytes,
                SyntheticHandshakePhase.FABRIC_CHANNEL_REGISTER_ACK,
                duration
        );
    }

    /**
     * Automatically inspects the incoming server modded payload and synthesizes an immediate
     * matching ACK packet.
     *
     * @param serverChannel the channel received from the server
     * @param serverPayload the binary payload emitted by the server
     * @return synthetic ACK response, or {@code null} if channel is unhandled
     */
    public SyntheticAckResponse generateAutomaticAck(String serverChannel, byte[] serverPayload) {
        Objects.requireNonNull(serverChannel, "serverChannel must not be null");
        Objects.requireNonNull(serverPayload, "serverPayload must not be null");

        if (serverChannel.equals(FML_HANDSHAKE_CHANNEL)) {
            if (serverPayload.length > 0) {
                byte discriminator = serverPayload[0];
                if (discriminator == 0x01) {
                    // Server ModList -> Client Ack
                    return createFmlModListAck(DEFAULT_TRANSACTION_TOKEN);
                } else if (discriminator == 0x02) {
                    // Server RegistryData -> Client Ack
                    return createFmlRegistrySyncAck(DEFAULT_TRANSACTION_TOKEN);
                }
            }
            return createFmlConnectionAck();
        }

        if (serverChannel.equals(NEOFORGE_HANDSHAKE_CHANNEL) || serverChannel.startsWith("neoforge:")) {
            return createNeoForgeHandshakeAck(DEFAULT_TRANSACTION_TOKEN);
        }

        if (serverChannel.equals(FABRIC_REGISTRY_SYNC_CHANNEL)) {
            return createFabricRegistrySyncComplete();
        }

        return null;
    }
}
