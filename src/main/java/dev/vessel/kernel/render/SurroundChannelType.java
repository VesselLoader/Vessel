package dev.vessel.kernel.render;

/**
 * Standard speaker channels in multichannel 5.1 / 7.1 surround sound audio configurations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum SurroundChannelType {

    /**
     * Front Left channel.
     */
    FRONT_LEFT(0),

    /**
     * Front Right channel.
     */
    FRONT_RIGHT(1),

    /**
     * Center dialog/vocal channel.
     */
    CENTER(2),

    /**
     * Low-Frequency Effects / Subwoofer channel.
     */
    LFE(3),

    /**
     * Surround Left / Side Left channel.
     */
    SURROUND_LEFT(4),

    /**
     * Surround Right / Side Right channel.
     */
    SURROUND_RIGHT(5),

    /**
     * Rear Left channel (7.1 only).
     */
    BACK_LEFT(6),

    /**
     * Rear Right channel (7.1 only).
     */
    BACK_RIGHT(7);

    private final int channelIndex;

    SurroundChannelType(int channelIndex) {
        this.channelIndex = channelIndex;
    }

    public int getChannelIndex() {
        return channelIndex;
    }
}
