package dev.vessel.bootstrap.wave11;

import java.util.Objects;
import java.util.UUID;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Test double implementation of BungeeCord {@link ProxiedPlayer}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleBungeePlayerDouble implements ProxiedPlayer {

    private final UUID uuid;
    private final String name;

    public SampleBungeePlayerDouble(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void sendMessage(String message) {
    }
}
