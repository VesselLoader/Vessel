package dev.vessel.kernel.adapter.forge;

/**
 * Functional interface implemented by Forge custom payload network channel subscribers.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface ForgeChannelHandler {

    /**
     * Dispatches an incoming or outgoing channel packet through the subscriber pipeline.
     *
     * @param message the payload message
     * @throws Exception if handler processing fails
     */
    void handlePacket(ForgeChannelMessage message) throws Exception;
}
