package dev.vessel.kernel.scene;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * High-performance Create mod contraption delta ring buffer and batch coordinator.
 * <p>
 * Implements Architecture Decision 11:
 * Coalesces up to 128 continuous game ticks of complex multi-block contraption movement into a single
 * unified delta broadcast packet, mitigating ERR-22 and resolving IND-0001 through IND-0200.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CreateContraptionDeltaRingBuffer {

    public static final int DEFAULT_CAPACITY = 128;
    public static final int MAGIC_HEADER = 0x43525450; // "CRTP"
    public static final int PACKET_HEADER_SIZE = 72;

    private final long contraptionId;
    private final ContraptionDeltaRingBuffer underlyingBuffer;

    /**
     * Constructs a CreateContraptionDeltaRingBuffer with default 128-tick capacity.
     *
     * @param contraptionId unique contraption entity identifier
     */
    public CreateContraptionDeltaRingBuffer(long contraptionId) {
        this(contraptionId, DEFAULT_CAPACITY);
    }

    /**
     * Constructs a CreateContraptionDeltaRingBuffer with specified power-of-two capacity.
     *
     * @param contraptionId unique contraption entity identifier
     * @param capacity      history window size (must be power of two)
     */
    public CreateContraptionDeltaRingBuffer(long contraptionId, int capacity) {
        this.contraptionId = contraptionId;
        this.underlyingBuffer = new ContraptionDeltaRingBuffer(contraptionId, capacity);
    }

    /**
     * Records a spatial and structural mutation for a single tick.
     *
     * @param tick             game tick index
     * @param posX             world coordinate X
     * @param posY             world coordinate Y
     * @param posZ             world coordinate Z
     * @param yaw              rotation yaw
     * @param pitch            rotation pitch
     * @param roll             rotation roll
     * @param structureVersion structure mutation revision
     * @param dirtyMask        dirty bitmask flags
     */
    public void recordFrame(
            long tick,
            double posX, double posY, double posZ,
            float yaw, float pitch, float roll,
            long structureVersion,
            int dirtyMask
    ) {
        underlyingBuffer.recordFrame(tick, posX, posY, posZ, yaw, pitch, roll, structureVersion, dirtyMask);
    }

    /**
     * Records a frame from an existing {@link DynamicContraptionState}.
     *
     * @param state contraption state
     * @param tick  game tick index
     */
    public void recordFrame(DynamicContraptionState state, long tick) {
        Objects.requireNonNull(state, "state cannot be null");
        underlyingBuffer.recordFrame(state, tick);
    }

    /**
     * Retrieves the contraption state at a specific historical tick.
     *
     * @param tick target game tick
     * @return contraption state snapshot or null if unavailable
     */
    public DynamicContraptionState getFrameAt(long tick) {
        return underlyingBuffer.getFrameAt(tick);
    }

    /**
     * Retrieves the latest recorded frame.
     *
     * @return latest state snapshot or null if empty
     */
    public DynamicContraptionState getLatestFrame() {
        return underlyingBuffer.getLatestFrame();
    }

    /**
     * Coalesces up to 128 historical movement frames into a single atomic batch synchronization packet.
     *
     * @param fromTick starting tick (inclusive)
     * @param toTick   ending tick (inclusive)
     * @return consolidated delta batch packet
     */
    public synchronized CreateContraptionBatchPacket buildConsolidatedBatch(long fromTick, long toTick) {
        if (fromTick < 0 || toTick < fromTick) {
            throw new IllegalArgumentException("Invalid tick range: from " + fromTick + " to " + toTick);
        }

        DynamicContraptionState startState = underlyingBuffer.getFrameAt(fromTick);
        DynamicContraptionState endState = underlyingBuffer.getFrameAt(toTick);

        if (startState == null && endState == null) {
            // Buffer empty or range out of bounds, construct empty batch packet
            byte[] emptyPayload = serializePayload(contraptionId, fromTick, toTick, 0, 0, 0, 0, 0, 0, 0, 0);
            return new CreateContraptionBatchPacket(
                    contraptionId, fromTick, toTick, 0,
                    0.0, 0.0, 0.0, 0.0f, 0.0f, 0.0f, 0, emptyPayload
            );
        }

        if (startState == null) {
            startState = endState;
        }
        if (endState == null) {
            endState = startState;
        }

        int frameCount = 0;
        int combinedDirtyMask = 0;

        for (long t = fromTick; t <= toTick; t++) {
            DynamicContraptionState frame = underlyingBuffer.getFrameAt(t);
            if (frame != null) {
                frameCount++;
                combinedDirtyMask |= frame.dirtyMask();
            }
        }

        double deltaX = endState.posX() - startState.posX();
        double deltaY = endState.posY() - startState.posY();
        double deltaZ = endState.posZ() - startState.posZ();
        float deltaYaw = endState.yaw() - startState.yaw();
        float deltaPitch = endState.pitch() - startState.pitch();
        float deltaRoll = endState.roll() - startState.roll();

        byte[] payload = serializePayload(
                contraptionId, fromTick, toTick, frameCount,
                deltaX, deltaY, deltaZ,
                deltaYaw, deltaPitch, deltaRoll,
                combinedDirtyMask
        );

        return new CreateContraptionBatchPacket(
                contraptionId,
                fromTick,
                toTick,
                frameCount,
                deltaX,
                deltaY,
                deltaZ,
                deltaYaw,
                deltaPitch,
                deltaRoll,
                combinedDirtyMask,
                payload
        );
    }

    /**
     * Builds a consolidated delta batch for the most recent 128-tick sliding window.
     *
     * @return consolidated delta batch packet
     */
    public synchronized CreateContraptionBatchPacket buildLatestBatch() {
        long latest = underlyingBuffer.getLatestTick();
        if (latest < 0) {
            return buildConsolidatedBatch(0L, 0L);
        }
        long oldest = underlyingBuffer.getOldestAvailableTick();
        return buildConsolidatedBatch(oldest, latest);
    }

    /**
     * Serializes delta batch components into a fixed 68-byte binary header.
     */
    private static byte[] serializePayload(
            long contraptionId,
            long startTick,
            long endTick,
            int frameCount,
            double deltaX,
            double deltaY,
            double deltaZ,
            float deltaYaw,
            float deltaPitch,
            float deltaRoll,
            int combinedDirtyMask
    ) {
        ByteBuffer buf = ByteBuffer.allocate(PACKET_HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(MAGIC_HEADER);
        buf.putLong(contraptionId);
        buf.putLong(startTick);
        buf.putLong(endTick);
        buf.putInt(frameCount);
        buf.putDouble(deltaX);
        buf.putDouble(deltaY);
        buf.putDouble(deltaZ);
        buf.putFloat(deltaYaw);
        buf.putFloat(deltaPitch);
        buf.putFloat(deltaRoll);
        buf.putInt(combinedDirtyMask);
        return buf.array();
    }

    public long getContraptionId() {
        return contraptionId;
    }

    public int getCapacity() {
        return underlyingBuffer.getCapacity();
    }

    public long getLatestTick() {
        return underlyingBuffer.getLatestTick();
    }

    public void clear() {
        underlyingBuffer.clear();
    }
}
