package dev.vessel.kernel.testmatrix;

/**
 * Enumeration of packet types simulated during extreme mod concurrency stress load testing.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum VirtualBotPacketType {

    /** Player movement coordinate update (x, y, z, pitch, yaw, onGround). */
    POSITION_AND_LOOK(1),

    /** Keep-alive ping acknowledgment packet. */
    KEEP_ALIVE(2),

    /** Entity interaction and block usage packet. */
    PLAYER_ACTION(3),

    /** Chat message and command execution packet. */
    CHAT_COMMAND(4),

    /** Inventory click and container manipulation packet. */
    CONTAINER_CLICK(5);

    private final int opcode;

    VirtualBotPacketType(int opcode) {
        this.opcode = opcode;
    }

    public int opcode() {
        return opcode;
    }
}
