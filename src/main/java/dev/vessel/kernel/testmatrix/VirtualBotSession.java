package dev.vessel.kernel.testmatrix;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates the runtime state and simulated networking telemetry for an individual virtual bot.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VirtualBotSession {

    private final String botId;
    private final int botIndex;
    private double posX;
    private double posY;
    private double posZ;
    private float yaw;
    private float pitch;
    private final AtomicLong packetsTransmitted = new AtomicLong(0);

    public VirtualBotSession(String botId, int botIndex, double initialX, double initialY, double initialZ) {
        this.botId = Objects.requireNonNull(botId, "botId must not be null");
        this.botIndex = botIndex;
        this.posX = initialX;
        this.posY = initialY;
        this.posZ = initialZ;
        this.yaw = 0.0f;
        this.pitch = 0.0f;
    }

    public String getBotId() {
        return botId;
    }

    public int getBotIndex() {
        return botIndex;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public long getPacketsTransmitted() {
        return packetsTransmitted.get();
    }

    /**
     * Advances bot location simulating movement vector displacement.
     *
     * @param deltaX displacement along X axis
     * @param deltaY displacement along Y axis
     * @param deltaZ displacement along Z axis
     * @param newYaw updated yaw angle
     * @param newPitch updated pitch angle
     */
    public void stepMovement(double deltaX, double deltaY, double deltaZ, float newYaw, float newPitch) {
        this.posX += deltaX;
        this.posY += deltaY;
        this.posZ += deltaZ;
        this.yaw = newYaw;
        this.pitch = newPitch;
        this.packetsTransmitted.incrementAndGet();
    }

    /**
     * Records transmission of a non-movement action packet.
     */
    public void recordActionPacket() {
        this.packetsTransmitted.incrementAndGet();
    }
}
