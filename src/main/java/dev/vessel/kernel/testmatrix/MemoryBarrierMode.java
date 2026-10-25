package dev.vessel.kernel.testmatrix;

/**
 * Memory ordering modes for CPU hardware cache synchronizations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum MemoryBarrierMode {

    /** Lightweight unidirectional Acquire-Release ordering avoiding full hardware bus locks. */
    ACQUIRE_RELEASE,

    /** Bi-directional full memory barrier (equivalent to MFENCE / LOCK CMPXCHG). */
    FULL_FENCE,

    /** Opaque memory access preventing compiler reordering without CPU hardware fence overhead. */
    OPAQUE,

    /** Plain unordered read/write without memory barriers. */
    PLAIN
}
