package dev.vessel.kernel.scene;

import java.util.Arrays;
import java.util.Objects;

/**
 * 64-bit primitive spatial delta ring buffer for dynamic multi-block contraptions (Create/Valkyrien).
 * <p>
 * Implements Architecture Decision 11:
 * <ul>
 *   <li>Stores a circular history of up to 128 ticks of high-frequency spatial and structural deltas.</li>
 *   <li>Automatically coalesces multiple sub-tick movements, rotations, and block updates within the same tick
 *       into a single atomic frame.</li>
 *   <li>Guarantees zero heap allocation per recorded tick frame using primitive array buffers.</li>
 *   <li>Provides instant one-time full synchronization for late-joining players and smooth client-side interpolation.</li>
 * </ul>
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ContraptionDeltaRingBuffer {

    public static final int DEFAULT_CAPACITY = 128; // Power of two (6.4 seconds of history at 20 TPS)

    private final long contraptionId;
    private final int capacity;
    private final int mask;

    // Flat primitive storage
    private final long[] ringTicks;
    private final double[] ringPosX;
    private final double[] ringPosY;
    private final double[] ringPosZ;
    private final float[] ringYaw;
    private final float[] ringPitch;
    private final float[] ringRoll;
    private final long[] ringStructureVersion;
    private final int[] ringDirtyMask;
    private final long[] ringPackedDeltas;

    private volatile long latestTick = -1L;
    private int recordedCount = 0;

    /**
     * Constructs a ContraptionDeltaRingBuffer with default capacity (128 ticks).
     *
     * @param contraptionId unique contraption identifier
     */
    public ContraptionDeltaRingBuffer(long contraptionId) {
        this(contraptionId, DEFAULT_CAPACITY);
    }

    /**
     * Constructs a ContraptionDeltaRingBuffer with specified power-of-two capacity.
     *
     * @param contraptionId unique contraption identifier
     * @param capacity      number of history ticks (must be a power of two)
     */
    public ContraptionDeltaRingBuffer(long contraptionId, int capacity) {
        if (capacity < 2 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity must be a power of two >= 2: " + capacity);
        }

        this.contraptionId = contraptionId;
        this.capacity = capacity;
        this.mask = capacity - 1;

        this.ringTicks = new long[capacity];
        Arrays.fill(this.ringTicks, -1L);

        this.ringPosX = new double[capacity];
        this.ringPosY = new double[capacity];
        this.ringPosZ = new double[capacity];
        this.ringYaw = new float[capacity];
        this.ringPitch = new float[capacity];
        this.ringRoll = new float[capacity];
        this.ringStructureVersion = new long[capacity];
        this.ringDirtyMask = new int[capacity];
        this.ringPackedDeltas = new long[capacity];
    }

    /**
     * Records a spatial and structural mutation for a given tick.
     * <p>
     * If multiple mutations occur in the same tick, they are atomically coalesced
     * into the existing tick slot, combining dirty bitmasks.
     * </p>
     *
     * @param tick             game tick index
     * @param posX             X coordinate
     * @param posY             Y coordinate
     * @param posZ             Z coordinate
     * @param yaw              rotation yaw
     * @param pitch            rotation pitch
     * @param roll             rotation roll
     * @param structureVersion structure mutation sequence number
     * @param dirtyMask        dirty bitmask flags (e.g. DIRTY_POS, DIRTY_ROT, DIRTY_STRUCTURE)
     */
    public synchronized void recordFrame(
            long tick,
            double posX, double posY, double posZ,
            float yaw, float pitch, float roll,
            long structureVersion,
            int dirtyMask
    ) {
        if (tick < 0) {
            throw new IllegalArgumentException("Tick must be non-negative: " + tick);
        }

        int slot = (int) (tick & mask);

        if (ringTicks[slot] == tick) {
            // Coalesce within the same tick
            ringPosX[slot] = posX;
            ringPosY[slot] = posY;
            ringPosZ[slot] = posZ;
            ringYaw[slot] = yaw;
            ringPitch[slot] = pitch;
            ringRoll[slot] = roll;
            ringStructureVersion[slot] = structureVersion;
            ringDirtyMask[slot] |= dirtyMask;
            ringPackedDeltas[slot] = packDelta64(posX, posY, posZ);
        } else {
            // New tick entry
            ringTicks[slot] = tick;
            ringPosX[slot] = posX;
            ringPosY[slot] = posY;
            ringPosZ[slot] = posZ;
            ringYaw[slot] = yaw;
            ringPitch[slot] = pitch;
            ringRoll[slot] = roll;
            ringStructureVersion[slot] = structureVersion;
            ringDirtyMask[slot] = dirtyMask;
            ringPackedDeltas[slot] = packDelta64(posX, posY, posZ);

            if (tick > latestTick) {
                latestTick = tick;
            }
            if (recordedCount < capacity) {
                recordedCount++;
            }
        }
    }

    /**
     * Convenience method to record frame from a {@link DynamicContraptionState}.
     *
     * @param state contraption state
     * @param tick  game tick index
     */
    public void recordFrame(DynamicContraptionState state, long tick) {
        Objects.requireNonNull(state, "state cannot be null");
        recordFrame(
                tick,
                state.posX(), state.posY(), state.posZ(),
                state.yaw(), state.pitch(), state.roll(),
                state.structureVersion(),
                state.dirtyMask()
        );
    }

    /**
     * Retrieves the contraption state at a specific historical tick.
     *
     * @param tick target tick
     * @return state snapshot or null if tick is outside the available ring window
     */
    public synchronized DynamicContraptionState getFrameAt(long tick) {
        if (!hasFrame(tick)) {
            return null;
        }

        int slot = (int) (tick & mask);
        return new DynamicContraptionState(
                contraptionId,
                ringPosX[slot], ringPosY[slot], ringPosZ[slot],
                ringYaw[slot], ringPitch[slot], ringRoll[slot],
                ringStructureVersion[slot],
                ringDirtyMask[slot]
        );
    }

    /**
     * Retrieves the latest recorded frame.
     *
     * @return latest state or null if buffer is empty
     */
    public synchronized DynamicContraptionState getLatestFrame() {
        if (latestTick < 0) {
            return null;
        }
        return getFrameAt(latestTick);
    }

    /**
     * Checks if a frame for the specified tick is present in the circular buffer.
     *
     * @param tick query tick
     * @return true if tick is valid and within buffer window
     */
    public synchronized boolean hasFrame(long tick) {
        if (tick < 0 || latestTick < 0) {
            return false;
        }
        if (tick > latestTick || tick <= latestTick - capacity) {
            return false;
        }
        int slot = (int) (tick & mask);
        return ringTicks[slot] == tick;
    }

    /**
     * Interpolates smoothly between two historical ticks for client rendering and late-join catchup.
     *
     * @param fromTick starting tick
     * @param toTick   target tick
     * @param alpha    interpolation factor [0.0, 1.0]
     * @return interpolated contraption state, or closest available frame if out of bounds
     */
    public synchronized DynamicContraptionState interpolate(long fromTick, long toTick, float alpha) {
        DynamicContraptionState s1 = getFrameAt(fromTick);
        DynamicContraptionState s2 = getFrameAt(toTick);

        if (s1 == null && s2 == null) {
            return getLatestFrame();
        }
        if (s1 == null) {
            return s2;
        }
        if (s2 == null) {
            return s1;
        }

        float t = Math.max(0.0f, Math.min(1.0f, alpha));
        double posX = s1.posX() + (s2.posX() - s1.posX()) * t;
        double posY = s1.posY() + (s2.posY() - s1.posY()) * t;
        double posZ = s1.posZ() + (s2.posZ() - s1.posZ()) * t;
        float yaw = s1.yaw() + (s2.yaw() - s1.yaw()) * t;
        float pitch = s1.pitch() + (s2.pitch() - s1.pitch()) * t;
        float roll = s1.roll() + (s2.roll() - s1.roll()) * t;

        return new DynamicContraptionState(
                contraptionId,
                posX, posY, posZ,
                yaw, pitch, roll,
                s2.structureVersion(),
                s1.dirtyMask() | s2.dirtyMask()
        );
    }

    /**
     * Returns the 64-bit packed spatial delta for the specified tick.
     *
     * @param tick query tick
     * @return packed 64-bit delta or 0L if not found
     */
    public synchronized long getPackedDelta(long tick) {
        if (!hasFrame(tick)) {
            return 0L;
        }
        int slot = (int) (tick & mask);
        return ringPackedDeltas[slot];
    }

    /**
     * Packs three coordinate values into a single 64-bit long delta (21 bits per component).
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return 64-bit packed delta
     */
    public static long packDelta64(double x, double y, double z) {
        long px = ((long) (x * 4096.0)) & 0x1FFFFF; // 21 bits
        long py = ((long) (y * 4096.0)) & 0x1FFFFF; // 21 bits
        long pz = ((long) (z * 4096.0)) & 0x1FFFFF; // 21 bits
        return (px << 42) | (py << 21) | pz;
    }

    public long getContraptionId() {
        return contraptionId;
    }

    public int getCapacity() {
        return capacity;
    }

    public synchronized long getLatestTick() {
        return latestTick;
    }

    public synchronized long getOldestAvailableTick() {
        if (latestTick < 0) {
            return -1L;
        }
        long oldestCandidate = latestTick - capacity + 1;
        return Math.max(0L, oldestCandidate);
    }

    public synchronized int getAvailableFramesCount() {
        return recordedCount;
    }

    /**
     * Clears all recorded frames and resets ring buffer state.
     */
    public synchronized void clear() {
        Arrays.fill(ringTicks, -1L);
        Arrays.fill(ringDirtyMask, 0);
        Arrays.fill(ringPackedDeltas, 0L);
        latestTick = -1L;
        recordedCount = 0;
    }
}
