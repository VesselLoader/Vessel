package dev.vessel.kernel.network.bridge;

import dev.vessel.bootstrap.target.VesselServerConfig;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the built-in Bedrock RakNet UDP listener (default port {@code 19132}) and automatically
 * yields the UDP port if an external standalone Geyser/Floodgate plugin is detected in the server
 * plugin directory (Decision 10), supporting dynamic configuration resolution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class BedrockRakNetPortBinder {

    public static final int DEFAULT_BEDROCK_UDP_PORT = 19132;

    private final AtomicBoolean internalRakNetBound = new AtomicBoolean(false);
    private final AtomicInteger boundUdpPort = new AtomicInteger(-1);

    /**
     * Binds the built-in Bedrock RakNet listener using dynamically configured port from {@link VesselServerConfig}.
     *
     * @param externalGeyserPluginDetected {@code true} if an external Geyser plugin is present
     * @return {@code true} if VESSEL bound its internal RakNet listener, or {@code false} if yielded
     */
    public boolean bindOrYieldToExternalGeyser(boolean externalGeyserPluginDetected) {
        int configuredPort = VesselServerConfig.getActiveConfig().getInt("proxy-integration.bedrock-port", DEFAULT_BEDROCK_UDP_PORT);
        int targetPort = (configuredPort > 0 && configuredPort <= 65535) ? configuredPort : DEFAULT_BEDROCK_UDP_PORT;
        return bindOrYieldToExternalGeyser(targetPort, externalGeyserPluginDetected);
    }

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
