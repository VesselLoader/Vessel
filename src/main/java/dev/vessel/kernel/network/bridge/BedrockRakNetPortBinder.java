package dev.vessel.kernel.network.bridge;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the built-in Bedrock RakNet UDP listener (default port {@code 19132}) and automatically
 * yields the UDP port if an external standalone Geyser/Floodgate plugin is detected in the server
 * plugin directory (Decision 10).
 *
 * @version 1.0.0
 */
public final class BedrockRakNetPortBinder {

    private final AtomicBoolean internalRakNetBound = new AtomicBoolean(false);
    private final AtomicInteger boundUdpPort = new AtomicInteger(-1);

    /**
     * Binds the built-in Bedrock RakNet listener to {@code udpPort} unless an external Geyser
     * plugin is detected, in which case VESSEL yields the port to the external plugin.
     *
     * @param udpPort                      the target UDP port (e.g., {@code 19132})
     * @param externalGeyserPluginDetected {@code true} if an external Geyser plugin is present
     * @return {@code true} if VESSEL bound its internal RakNet listener, or {@code false} if it
     *         yielded to an external Geyser installation
     */
    public boolean bindOrYieldToExternalGeyser(int udpPort, boolean externalGeyserPluginDetected) {
        if (externalGeyserPluginDetected || udpPort <= 0 || udpPort > 65535) {
            internalRakNetBound.set(false);
            boundUdpPort.set(-1);
            return false;
        }
        boundUdpPort.set(udpPort);
        internalRakNetBound.set(true);
        return true;
    }

    /**
     * Returns whether VESSEL's built-in Bedrock RakNet listener is currently bound and active.
     *
     * @return {@code true} if the internal RakNet listener is bound
     */
    public boolean isInternalRakNetBound() {
        return internalRakNetBound.get();
    }

    /**
     * Returns the bound UDP port, or {@code -1} if unbound.
     *
     * @return the bound UDP port
     */
    public int getBoundUdpPort() {
        return boundUdpPort.get();
    }
}
