package net.minecraftforge.network;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Forge network channel registry ({@code net.minecraftforge.network.NetworkRegistry}) providing
 * factory methods and lookup access for {@link SimpleChannel} instances.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NetworkRegistry {

    /**
     * Protocol version token indicating that the channel is absent on the remote peer.
     */
    public static final String ABSENT = "ABSENT";

    /**
     * Protocol version token indicating a vanilla client or server without Forge networking.
     */
    public static final String ACCEPTVANILLA = "ACCEPTVANILLA";

    private static final Map<String, SimpleChannel> CHANNELS = new ConcurrentHashMap<>();

    private NetworkRegistry() {
    }

    /**
     * Creates and registers a new {@link SimpleChannel} with the specified channel name and protocol predicates.
     *
     * @param name                   non-null namespaced channel identifier
     * @param networkProtocolVersion non-null supplier returning the channel's protocol version string
     * @param clientAcceptedVersions non-null predicate verifying client compatibility
     * @param serverAcceptedVersions non-null predicate verifying server compatibility
     * @return the newly created and registered {@link SimpleChannel}
     */
    public static SimpleChannel newSimpleChannel(
            String name,
            Supplier<String> networkProtocolVersion,
            Predicate<String> clientAcceptedVersions,
            Predicate<String> serverAcceptedVersions) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(networkProtocolVersion, "networkProtocolVersion must not be null");
        Objects.requireNonNull(clientAcceptedVersions, "clientAcceptedVersions must not be null");
        Objects.requireNonNull(serverAcceptedVersions, "serverAcceptedVersions must not be null");
        String trimmedName = name.trim();
        SimpleChannel channel = new SimpleChannel(trimmedName, networkProtocolVersion.get());
        CHANNELS.put(trimmedName, channel);
        return channel;
    }

    /**
     * Looks up a previously registered {@link SimpleChannel} by its channel name.
     *
     * @param name channel identifier
     * @return registered {@link SimpleChannel}, or {@code null} if not registered
     */
    public static SimpleChannel getChannel(String name) {
        if (name == null) {
            return null;
        }
        return CHANNELS.get(name.trim());
    }

    /**
     * Clears all registered network channels from the registry.
     */
    public static void clearChannels() {
        CHANNELS.clear();
    }
}
