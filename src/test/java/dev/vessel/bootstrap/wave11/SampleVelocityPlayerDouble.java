package dev.vessel.bootstrap.wave11;

import com.velocitypowered.api.proxy.Player;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Test double implementation of Velocity {@link Player}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleVelocityPlayerDouble implements Player {

    private final UUID uuid;
    private final String username;

    public SampleVelocityPlayerDouble(UUID uuid, String username) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void sendMessage(Component component) {
    }
}
