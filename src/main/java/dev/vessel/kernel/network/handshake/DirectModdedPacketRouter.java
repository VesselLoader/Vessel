package dev.vessel.kernel.network.handshake;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Direct native packet router facilitating zero-overhead pass-through of raw modded network
 * payloads for connections verified under {@link ClientPresentationMode#STRICT_CLIENT_MODDED}.
 *
 * <p>Bypasses server-side virtual display transformation pipelines, font glyph synthesizers,
 * and intermediate serializer adaptations when symmetrical client mods are active.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DirectModdedPacketRouter {

    /**
     * Well-known canonical modded channel namespaces routed directly without intermediate transformations.
     */
    public static final Set<String> DEFAULT_DIRECT_CHANNEL_NAMESPACES = Set.of(
            "create:",
            "mekanism:",
            "irons_spellbooks:",
            "appliedenergistics2:",
            "ae2:",
            "botania:",
            "thermal:",
            "immersiveengineering:",
            "refinedstorage:",
            "twilightforest:"
    );

    private final Set<String> directChannels = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, ClientPresentationMode> connectionModes = new ConcurrentHashMap<>();
    private final Set<String> verifiedConnections = ConcurrentHashMap.newKeySet();

    private final AtomicLong routedPacketCount = new AtomicLong(0L);
    private final AtomicLong routedByteCount = new AtomicLong(0L);

    /**
     * Constructs a new DirectModdedPacketRouter seeded with default direct namespaces.
     */
    public DirectModdedPacketRouter() {
        directChannels.addAll(DEFAULT_DIRECT_CHANNEL_NAMESPACES);
    }

    /**
     * Registers an individual custom mod channel or namespace prefix for direct routing.
     *
     * @param channel the channel identifier or namespace prefix (e.g. "my_mod:sync")
     */
    public void registerDirectChannel(String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        directChannels.add(channel);
    }

    /**
     * Unregisters a channel or prefix from direct routing.
     *
     * @param channel the channel identifier to remove
     * @return {@code true} if removed
     */
    public boolean unregisterDirectChannel(String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        return directChannels.remove(channel);
    }

    /**
     * Determines whether a given channel identifier qualifies for direct routing.
     *
     * @param channel the channel to test
     * @return {@code true} if eligible for direct pass-through
     */
    public boolean isDirectChannel(String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        if (directChannels.contains(channel)) {
            return true;
        }
        for (String prefix : directChannels) {
            if (prefix.endsWith(":") && channel.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Registers a new client connection session and its presentation mode.
     *
     * @param connectionId unique connection identifier
     * @param mode         presentation mode assigned to this session
     */
    public void registerConnection(String connectionId, ClientPresentationMode mode) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        connectionModes.put(connectionId, mode);
    }

    /**
     * Marks a connection session as having successfully completed the mod manifest audit.
     *
     * @param connectionId unique connection identifier
     */
    public void markConnectionVerified(String connectionId) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        verifiedConnections.add(connectionId);
    }

    /**
     * Unregisters a connection session upon termination or disconnect.
     *
     * @param connectionId unique connection identifier
     */
    public void unregisterConnection(String connectionId) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        connectionModes.remove(connectionId);
        verifiedConnections.remove(connectionId);
    }

    /**
     * Checks if a connection session has verified status.
     *
     * @param connectionId connection identifier
     * @return {@code true} if verified
     */
    public boolean isConnectionVerified(String connectionId) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        return verifiedConnections.contains(connectionId);
    }

    /**
     * Retrieves the presentation mode assigned to a connection session.
     *
     * @param connectionId connection identifier
     * @return {@link ClientPresentationMode}, or {@code null} if unassigned
     */
    public ClientPresentationMode getConnectionMode(String connectionId) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        return connectionModes.get(connectionId);
    }

    /**
     * Verifies whether an outbound packet can bypass presentation transformations and route directly.
     *
     * @param connectionId target connection identifier
     * @param channel      packet channel identifier
     * @return {@code true} if direct pass-through is permissible
     */
    public boolean canRouteDirectly(String connectionId, String channel) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");

        ClientPresentationMode mode = connectionModes.get(connectionId);
        if (mode != ClientPresentationMode.STRICT_CLIENT_MODDED) {
            return false;
        }

        if (!verifiedConnections.contains(connectionId)) {
            return false;
        }

        return isDirectChannel(channel);
    }

    /**
     * Routes an outbound packet directly to wire serializer if authorized.
     *
     * @param connectionId  target connection identifier
     * @param channel       packet channel
     * @param rawPacketData raw bytes from native mod serializer
     * @return unchanged raw bytes if directly routed, or {@code null} if presentation transformation required
     */
    public byte[] routeOutboundPacket(String connectionId, String channel, byte[] rawPacketData) {
        Objects.requireNonNull(connectionId, "connectionId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(rawPacketData, "rawPacketData must not be null");

        if (canRouteDirectly(connectionId, channel)) {
            routedPacketCount.incrementAndGet();
            routedByteCount.addAndGet(rawPacketData.length);
            return rawPacketData;
        }

        return null;
    }

    /**
     * Returns the total count of modded packets routed directly.
     *
     * @return routed packet count
     */
    public long getRoutedPacketCount() {
        return routedPacketCount.get();
    }

    /**
     * Returns the total count of bytes routed directly.
     *
     * @return routed byte count
     */
    public long getRoutedByteCount() {
        return routedByteCount.get();
    }

    /**
     * Returns the number of currently tracked connection sessions.
     *
     * @return active connection count
     */
    public int getActiveConnectionCount() {
        return connectionModes.size();
    }

    /**
     * Returns the number of verified modded connection sessions.
     *
     * @return verified connection count
     */
    public int getVerifiedConnectionCount() {
        return verifiedConnections.size();
    }

    /**
     * Returns an unmodifiable snapshot of direct channel identifiers.
     *
     * @return direct channel set
     */
    public Set<String> getDirectChannels() {
        return Collections.unmodifiableSet(new HashSet<>(directChannels));
    }

    /**
     * Resets packet and byte routing telemetry metrics.
     */
    public void resetMetrics() {
        routedPacketCount.set(0L);
        routedByteCount.set(0L);
    }
}
