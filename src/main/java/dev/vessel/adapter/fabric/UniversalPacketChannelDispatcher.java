package dev.vessel.adapter.fabric;

import dev.vessel.kernel.identity.SourceLoaderKind;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Universal custom payload channel router enforcing unique payload channel ownership
 * (Lesson L29) and filtering advertised {@code minecraft:register} channels based on
 * client persona (Vanilla vs Modded).
 *
 * @version 1.0.0
 */
public final class UniversalPacketChannelDispatcher {

    private static final Set<String> VANILLA_SAFE_NAMESPACE_PREFIXES = Set.of(
            "minecraft:",
            "bungeecord:",
            "velocity:",
            "vessel:"
    );

    private final Map<String, Consumer<byte[]>> channelHandlers = new ConcurrentHashMap<>();
    private final Map<String, SourceLoaderKind> channelOwners = new ConcurrentHashMap<>();

    /**
     * Creates a new universal packet channel dispatcher.
     */
    public UniversalPacketChannelDispatcher() {
    }

    /**
     * Registers a custom payload handler for the given channel ID. Throws an
     * {@link IllegalStateException} if the channel is already registered (Lesson L29).
     *
     * @param channelId      namespaced channel identifier (e.g., {@code "create:contraption_sync"})
     * @param ownerLoader    source loader ecosystem registering the channel
     * @param payloadHandler consumer receiving raw payload bytes
     */
    public void registerChannelHandler(
            String channelId, SourceLoaderKind ownerLoader, Consumer<byte[]> payloadHandler) {
        Objects.requireNonNull(channelId, "channelId must not be null");
        Objects.requireNonNull(ownerLoader, "ownerLoader must not be null");
        Objects.requireNonNull(payloadHandler, "payloadHandler must not be null");
        String normalized = channelId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("channelId must not be empty");
        }
        SourceLoaderKind existingOwner = channelOwners.putIfAbsent(normalized, ownerLoader);
        if (existingOwner != null) {
            throw new IllegalStateException(
                    "Duplicate custom payload channel registration rejected (Lesson L29): '"
                            + normalized + "' already owned by " + existingOwner);
        }
        channelHandlers.put(normalized, payloadHandler);
    }

    /**
     * Dispatches an incoming custom payload packet to its registered handler.
     *
     * @param channelId    namespaced channel identifier
     * @param payloadBytes raw payload byte array
     * @return {@code true} if a registered handler processed the payload
     */
    public boolean dispatchIncomingCustomPayload(String channelId, byte[] payloadBytes) {
        Objects.requireNonNull(channelId, "channelId must not be null");
        Objects.requireNonNull(payloadBytes, "payloadBytes must not be null");
        String normalized = channelId.trim().toLowerCase(Locale.ROOT);
        Consumer<byte[]> handler = channelHandlers.get(normalized);
        if (handler == null) {
            return false;
        }
        handler.accept(payloadBytes);
        return true;
    }

    /**
     * Returns the set of custom payload channels to advertise in the {@code minecraft:register}
     * packet for a connecting client. Vanilla clients only receive standard plugin/brand channels,
     * hiding internal mod network channels.
     *
     * @param isVanillaClient {@code true} if the client has no client-side mod loader
     * @return unmodifiable set of advertised channel identifiers
     */
    public Set<String> getAdvertisedChannelsForClient(boolean isVanillaClient) {
        Set<String> advertised = new LinkedHashSet<>();
        for (Map.Entry<String, SourceLoaderKind> entry : channelOwners.entrySet()) {
            String channel = entry.getKey();
            SourceLoaderKind owner = entry.getValue();
            if (!isVanillaClient) {
                advertised.add(channel);
            } else if (isVanillaCompatibleChannel(channel, owner)) {
                advertised.add(channel);
            }
        }
        return Collections.unmodifiableSet(advertised);
    }

    private static boolean isVanillaCompatibleChannel(String channel, SourceLoaderKind owner) {
        if (owner == SourceLoaderKind.BUKKIT
                || owner == SourceLoaderKind.SPIGOT
                || owner == SourceLoaderKind.PAPER
                || owner == SourceLoaderKind.FOLIA) {
            return true;
        }
        for (String prefix : VANILLA_SAFE_NAMESPACE_PREFIXES) {
            if (channel.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
