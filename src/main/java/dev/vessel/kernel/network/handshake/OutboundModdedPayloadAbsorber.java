package dev.vessel.kernel.network.handshake;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Intercepts and absorbs 100% of outbound modded handshake packets (FML, NeoForge, Fabric)
 * emitted from the server towards vanilla Java and Bedrock clients (0 bytes onto the wire),
 * eliminating vanilla client disconnects and Netty buffer copy overhead.
 *
 * <p>Responsibility: Suppress modded handshake payloads for unmodded clients.
 * Eliminates: ERR-11, NET-001..020.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class OutboundModdedPayloadAbsorber {

    /**
     * Canonical empty byte array returned when a payload is absorbed (0 bytes to wire).
     */
    public static final byte[] ZERO_BYTES_ABSORBED = new byte[0];

    /**
     * Set of well-known modded handshake channel identifiers.
     */
    public static final Set<String> DEFAULT_MODDED_HANDSHAKE_CHANNELS = Set.of(
            "fml:handshake",
            "fml:loginwrapper",
            "neoforge:handshake",
            "neoforge:registry_data",
            "forge:tier_sorting",
            "forge:split",
            "fabric:registry_sync",
            "fabric:channel_register"
    );

    /**
     * Known prefixes for modded handshake channels.
     */
    public static final String FML_CHANNEL_PREFIX = "fml:";
    public static final String NEOFORGE_CHANNEL_PREFIX = "neoforge:";
    public static final String FABRIC_REGISTRY_PREFIX = "fabric:registry";

    private final Set<String> registeredModdedChannels = ConcurrentHashMap.newKeySet();
    private final AtomicLong absorbedPacketCount = new AtomicLong(0L);
    private final AtomicLong absorbedByteCount = new AtomicLong(0L);

    /**
     * Constructs a new OutboundModdedPayloadAbsorber seeded with canonical modded handshake channels.
     */
    public OutboundModdedPayloadAbsorber() {
        registeredModdedChannels.addAll(DEFAULT_MODDED_HANDSHAKE_CHANNELS);
    }

    /**
     * Registers an additional custom modded channel to be absorbed.
     *
     * @param channel the channel identifier to register
     */
    public void registerModdedChannel(String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        registeredModdedChannels.add(channel);
    }

    /**
     * Unregisters a channel from the absorber.
     *
     * @param channel the channel identifier to unregister
     * @return {@code true} if the channel was removed, {@code false} otherwise
     */
    public boolean unregisterModdedChannel(String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        return registeredModdedChannels.remove(channel);
    }

    /**
     * Checks if a channel identifier is identified as a modded handshake channel.
     *
     * @param channel the namespaced channel to test
     * @return {@code true} if modded handshake channel
     */
    public boolean isModdedHandshakeChannel(String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        if (registeredModdedChannels.contains(channel)) {
            return true;
        }
        return channel.startsWith(FML_CHANNEL_PREFIX)
                || channel.startsWith(NEOFORGE_CHANNEL_PREFIX)
                || channel.startsWith(FABRIC_REGISTRY_PREFIX);
    }

    /**
     * Determines whether the given outbound channel packet must be absorbed for this client.
     *
     * @param channel        the namespaced channel identifier
     * @param isModdedClient {@code true} if the destination client is a modded Forge/Fabric client
     * @return {@code true} if the packet must be absorbed (0 bytes to network)
     */
    public boolean shouldAbsorb(String channel, boolean isModdedClient) {
        Objects.requireNonNull(channel, "channel must not be null");
        if (isModdedClient) {
            return false;
        }
        return isModdedHandshakeChannel(channel);
    }

    /**
     * Intercepts an outbound payload. If the destination client is unmodded and the channel
     * is a modded handshake channel, absorbs the payload completely and returns a zero-byte array.
     * Otherwise, returns the unmodified payload.
     *
     * @param channel        the channel identifier
     * @param rawPayload     the raw outbound packet payload bytes
     * @param isModdedClient {@code true} if destination is a modded client
     * @return {@code ZERO_BYTES_ABSORBED} if absorbed, or original payload otherwise
     */
    public byte[] processOutboundPayload(String channel, byte[] rawPayload, boolean isModdedClient) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(rawPayload, "rawPayload must not be null");

        if (shouldAbsorb(channel, isModdedClient)) {
            absorbedPacketCount.incrementAndGet();
            absorbedByteCount.addAndGet(rawPayload.length);
            return ZERO_BYTES_ABSORBED;
        }

        return rawPayload;
    }

    /**
     * Returns the total number of modded packets absorbed so far.
     *
     * @return total absorbed packet count
     */
    public long getAbsorbedPacketCount() {
        return absorbedPacketCount.get();
    }

    /**
     * Returns the total number of payload bytes absorbed and prevented from wire transmission.
     *
     * @return total absorbed byte count
     */
    public long getAbsorbedByteCount() {
        return absorbedByteCount.get();
    }

    /**
     * Returns an unmodifiable snapshot view of the registered modded channels.
     *
     * @return set of registered modded channels
     */
    public Set<String> getRegisteredModdedChannels() {
        return Collections.unmodifiableSet(new HashSet<>(registeredModdedChannels));
    }

    /**
     * Resets packet and byte absorption telemetry metrics.
     */
    public void resetMetrics() {
        absorbedPacketCount.set(0L);
        absorbedByteCount.set(0L);
    }
}
