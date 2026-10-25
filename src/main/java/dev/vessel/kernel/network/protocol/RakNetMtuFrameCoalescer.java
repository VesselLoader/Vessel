package dev.vessel.kernel.network.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coalesces and fragments RakNet MTU frames to prevent dropping oversized UDP packets
 * on mobile devices, Nintendo Switch, PlayStation, and Xbox Bedrock clients.
 *
 * <p>Responsibility: MTU framing, packet slicing, and out-of-order reassembly.
 * Eliminates: ERR-77, NET-081..100.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class RakNetMtuFrameCoalescer {

    /**
     * Canonical safe MTU payload size for Bedrock RakNet UDP transmission (1400 bytes).
     */
    public static final int DEFAULT_MAX_MTU_PAYLOAD = 1400;

    /**
     * Minimum valid MTU size allowable on standard internet routing.
     */
    public static final int MIN_MTU_PAYLOAD_SIZE = 512;

    /**
     * Maximum allowed pending compounds held in memory to guard against DoS starvation.
     */
    public static final int MAX_PENDING_COMPOUNDS = 1024;

    private static final byte[] EMPTY_BYTES = new byte[0];

    private final int configuredMaxMtu;
    private final Map<Integer, Map<Integer, byte[]>> pendingCompounds = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> expectedSplitCounts = new ConcurrentHashMap<>();
    private final AtomicInteger compoundIdSequence = new AtomicInteger(1);

    /**
     * Constructs a RakNetMtuFrameCoalescer with default MTU payload size (1400 bytes).
     */
    public RakNetMtuFrameCoalescer() {
        this(DEFAULT_MAX_MTU_PAYLOAD);
    }

    /**
     * Constructs a RakNetMtuFrameCoalescer with custom MTU payload size.
     *
     * @param maxMtuPayload maximum payload bytes per frame
     */
    public RakNetMtuFrameCoalescer(int maxMtuPayload) {
        if (maxMtuPayload < MIN_MTU_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("MTU payload size must be >= " + MIN_MTU_PAYLOAD_SIZE + ": " + maxMtuPayload);
        }
        this.configuredMaxMtu = maxMtuPayload;
    }

    /**
     * Generates a monotonically increasing compound ID for splitting an oversized packet.
     *
     * @return next compound identifier
     */
    public int nextCompoundId() {
        return compoundIdSequence.getAndIncrement() & 0x7FFFFFFF;
    }

    /**
     * Fragments a large payload into multiple MTU-compliant RakNet frames.
     *
     * @param compoundId compound ID identifying the split packet
     * @param payload    the raw payload bytes to fragment
     * @return ordered list of RakNetMtuFrame fragments
     */
    public List<RakNetMtuFrame> fragmentPayload(int compoundId, byte[] payload) {
        Objects.requireNonNull(payload, "payload must not be null");
        if (payload.length == 0) {
            return List.of(new RakNetMtuFrame(compoundId, 0, 1, EMPTY_BYTES));
        }

        int totalLength = payload.length;
        int splitCount = (totalLength + configuredMaxMtu - 1) / configuredMaxMtu;
        List<RakNetMtuFrame> frames = new ArrayList<>(splitCount);

        for (int i = 0; i < splitCount; i++) {
            int offset = i * configuredMaxMtu;
            int length = Math.min(configuredMaxMtu, totalLength - offset);
            byte[] slice = new byte[length];
            System.arraycopy(payload, offset, slice, 0, length);
            frames.add(new RakNetMtuFrame(compoundId, i, splitCount, slice));
        }

        return Collections.unmodifiableList(frames);
    }

    /**
     * Ingests a received RakNet frame and reassembles the full packet once all fragments have arrived,
     * supporting out-of-order and interleaved fragment delivery.
     *
     * @param frame incoming fragment frame
     * @return coalesced result indicating completion status and reconstructed payload
     */
    public CoalescedMtuResult ingestFrame(RakNetMtuFrame frame) {
        Objects.requireNonNull(frame, "frame must not be null");

        // Single unfragmented frame optimization fast-path
        if (frame.splitCount() == 1) {
            return new CoalescedMtuResult(true, frame.payload(), frame.compoundId(), 1, 1);
        }

        int compoundId = frame.compoundId();

        // Enforce maximum pending compounds limit
        if (pendingCompounds.size() >= MAX_PENDING_COMPOUNDS && !pendingCompounds.containsKey(compoundId)) {
            // Evict oldest or reject to protect server memory
            pendingCompounds.clear();
            expectedSplitCounts.clear();
        }

        Map<Integer, byte[]> compoundFragments = pendingCompounds.computeIfAbsent(
                compoundId,
                ignored -> new ConcurrentHashMap<>()
        );
        expectedSplitCounts.putIfAbsent(compoundId, frame.splitCount());

        compoundFragments.put(frame.splitIndex(), frame.payload());
        int receivedCount = compoundFragments.size();
        int expectedCount = expectedSplitCounts.get(compoundId);

        if (receivedCount < expectedCount) {
            return new CoalescedMtuResult(false, EMPTY_BYTES, compoundId, receivedCount, expectedCount);
        }

        // All fragments have arrived - reassemble in exact index order
        int totalPayloadBytes = 0;
        for (int i = 0; i < expectedCount; i++) {
            byte[] frag = compoundFragments.get(i);
            if (frag == null) {
                // Inconsistency guard
                return new CoalescedMtuResult(false, EMPTY_BYTES, compoundId, receivedCount, expectedCount);
            }
            totalPayloadBytes += frag.length;
        }

        byte[] coalesced = new byte[totalPayloadBytes];
        int writePos = 0;
        for (int i = 0; i < expectedCount; i++) {
            byte[] frag = compoundFragments.get(i);
            System.arraycopy(frag, 0, coalesced, writePos, frag.length);
            writePos += frag.length;
        }

        // Clean up completed compound state
        pendingCompounds.remove(compoundId);
        expectedSplitCounts.remove(compoundId);

        return new CoalescedMtuResult(true, coalesced, compoundId, expectedCount, expectedCount);
    }

    /**
     * Clears pending state for an aborted or timed-out compound.
     *
     * @param compoundId compound ID to clear
     */
    public void clearPendingCompound(int compoundId) {
        pendingCompounds.remove(compoundId);
        expectedSplitCounts.remove(compoundId);
    }

    /**
     * Returns the count of compounds currently awaiting reassembly.
     *
     * @return pending compounds count
     */
    public int getPendingCompoundsCount() {
        return pendingCompounds.size();
    }

    /**
     * Returns the configured MTU payload size.
     *
     * @return configured max MTU
     */
    public int getConfiguredMaxMtu() {
        return configuredMaxMtu;
    }
}
