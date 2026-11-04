package dev.vessel.kernel.network.payload;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate-limiting filter and sanitizer protecting against malicious custom payload packet floods.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PayloadRateLimitAndSanitizer {

    private static final int MAX_PACKETS_PER_SECOND = 1000;
    private static final int MAX_PAYLOAD_BYTES = 1024 * 1024; // 1 MB limit

    private final Map<String, AtomicInteger> clientPacketCounts;

    /**
     * Constructs a payload sanitizer.
     */
    public PayloadRateLimitAndSanitizer() {
        this.clientPacketCounts = new ConcurrentHashMap<>();
    }

    /**
     * Validates whether a custom packet payload is legitimate and within safe thresholds.
     *
     * @param clientIdentifier client IP or UUID string
     * @param payloadSize      payload size in bytes
     * @return true if packet is accepted; false if rejected/flooded
     */
    public boolean checkAndSanitize(String clientIdentifier, int payloadSize) {
        Objects.requireNonNull(clientIdentifier, "clientIdentifier cannot be null");

        if (payloadSize < 0 || payloadSize > MAX_PAYLOAD_BYTES) {
            return false;
        }

        AtomicInteger counter = clientPacketCounts.computeIfAbsent(clientIdentifier, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        return current <= MAX_PACKETS_PER_SECOND;
    }

    /**
     * Resets rate-limit buckets periodically (e.g. per second).
     */
    public void resetWindow() {
        clientPacketCounts.clear();
    }
}
