package org.bukkit.plugin.messaging;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Runtime implementation of Bukkit's custom plugin messaging channel router
 * ({@code org.bukkit.plugin.messaging.Messenger}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Messenger {

    private final Set<String> outgoingChannels = ConcurrentHashMap.newKeySet();
    private final Map<String, List<PluginMessageListener>> incomingListeners =
            new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code Messenger}.
     */
    public Messenger() {
    }

    /**
     * Registers an outgoing plugin messaging channel for the given plugin.
     *
     * @param plugin  owning plugin (nullable in unit tests)
     * @param channel non-null channel identifier
     */
    public void registerOutgoingPluginChannel(Plugin plugin, String channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        outgoingChannels.add(channel);
    }

    /**
     * Registers an incoming plugin messaging channel listener for the given plugin.
     *
     * @param plugin   owning plugin (nullable in unit tests)
     * @param channel  non-null channel identifier
     * @param listener non-null message callback
     */
    public void registerIncomingPluginChannel(
            Plugin plugin,
            String channel,
            PluginMessageListener listener) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        incomingListeners
                .computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    /**
     * Checks whether the specified outgoing channel is registered.
     *
     * @param channel channel identifier
     * @return {@code true} if registered
     */
    public boolean isOutgoingChannelRegistered(String channel) {
        return channel != null && outgoingChannels.contains(channel);
    }

    /**
     * Checks whether at least one listener is registered for the specified incoming channel.
     *
     * @param channel channel identifier
     * @return {@code true} if registered
     */
    public boolean isIncomingChannelRegistered(String channel) {
        if (channel == null) {
            return false;
        }
        List<PluginMessageListener> listeners = incomingListeners.get(channel);
        return listeners != null && !listeners.isEmpty();
    }

    /**
     * Dispatches an incoming plugin message to all listeners registered on {@code channel}.
     *
     * @param channel non-null channel identifier
     * @param player  player connection context
     * @param message raw payload bytes
     * @return number of listeners invoked
     */
    public int dispatchIncomingMessage(String channel, Player player, byte[] message) {
        Objects.requireNonNull(channel, "channel must not be null");
        List<PluginMessageListener> listeners = incomingListeners.get(channel);
        if (listeners == null || listeners.isEmpty()) {
            return 0;
        }
        byte[] payload = message != null ? message : new byte[0];
        int invoked = 0;
        for (PluginMessageListener listener : listeners) {
            listener.onPluginMessageReceived(channel, player, payload);
            invoked++;
        }
        return invoked;
    }

    /**
     * Clears all outgoing and incoming channel registrations.
     */
    public void clear() {
        outgoingChannels.clear();
        incomingListeners.clear();
    }
}
