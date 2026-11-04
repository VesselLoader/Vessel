package dev.vessel.kernel.network.payload;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Emulation router for MinecraftForge 1.16 - 1.20.1 SimpleChannel network architecture.
 *
 * <p>Enables classic Forge mods to register integer packet discrimination indices and route
 * custom packets transparently into {@link UniversalCustomPayloadRegistrar}.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ForgeSimpleChannelEmulationRouter {

    private final String channelName;
    private final UniversalCustomPayloadRegistrar registrar;
    private final Map<Integer, BiConsumer<Object, byte[]>> indexReceivers;

    /**
     * Constructs a SimpleChannel emulation router for a given channel name.
     *
     * @param channelName channel identifier (e.g. {@code "mymod:network"})
     * @param registrar   universal payload registrar
     */
    public ForgeSimpleChannelEmulationRouter(String channelName, UniversalCustomPayloadRegistrar registrar) {
        this.channelName = Objects.requireNonNull(channelName, "channelName cannot be null");
        this.registrar = Objects.requireNonNull(registrar, "registrar cannot be null");
        this.indexReceivers = new ConcurrentHashMap<>();

        // Register router into universal registrar
        this.registrar.registerReceiver(channelName, this::handleChannelBytes);
    }

    /**
     * Registers a packet discriminator index with its execution handler.
     *
     * @param index   packet index (0 to 255)
     * @param handler packet consumer
     */
    public void registerMessage(int index, BiConsumer<Object, byte[]> handler) {
        Objects.requireNonNull(handler, "handler cannot be null");
        indexReceivers.put(index, handler);
    }

    private void handleChannelBytes(Object player, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        int discriminator = bytes[0] & 0xFF;
        BiConsumer<Object, byte[]> handler = indexReceivers.get(discriminator);
        if (handler != null) {
            byte[] payload = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, payload, 0, payload.length);
            handler.accept(player, payload);
        }
    }

    /**
     * Resolves the channel name.
     *
     * @return channel name
     */
    public String getChannelName() {
        return channelName;
    }
}
