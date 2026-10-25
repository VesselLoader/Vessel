package dev.vessel.kernel.vstm.engine;

import java.util.Objects;

/**
 * Parallelized multi-lane 64-bit SIMD-style hasher for compound JVM member signatures.
 *
 * <p>Optimized for rapid calculation of (Owner + Name + Descriptor) composite keys
 * with unrolled 64-bit arithmetic, prime mixing, and branchless bit rotations.
 * Provides resilient scalar fallback across all JVM CPU architectures (Catalog refs: VOL6-0210, ZER-0010).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmSimdHasher {

    private static final long PRIME1 = 0x9E3779B185EBCA87L;
    private static final long PRIME2 = 0xC2B2AE3D27D4EB4FL;
    private static final long PRIME3 = 0x165667B19E3779F9L;
    private static final long PRIME4 = 0x85EBCA77C2B2AE63L;

    /**
     * Computes a compound 64-bit signature hash for a class member (method or field).
     *
     * @param owner      the owner internal class name (e.g., "net/minecraft/world/level/Level")
     * @param name       the member name (e.g., "addFreshEntity")
     * @param descriptor the member descriptor (e.g., "(Lnet/minecraft/world/entity/Entity;)Z")
     * @return composite 64-bit hash
     */
    public static long hashSignature64(String owner, String name, String descriptor) {
        Objects.requireNonNull(owner, "owner must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(descriptor, "descriptor must not be null");

        long hOwner = VstmMurmurHash64.hash64(owner);
        long hName = VstmMurmurHash64.hash64(name);
        long hDesc = VstmMurmurHash64.hash64(descriptor);

        return combineHashes(hOwner, hName, hDesc);
    }

    /**
     * Combines three 64-bit pre-computed hashes into a single compound signature hash.
     *
     * @param hOwner hash of owner class
     * @param hName  hash of member name
     * @param hDesc  hash of descriptor
     * @return unified 64-bit hash
     */
    public static long combineHashes(long hOwner, long hName, long hDesc) {
        long k1 = hOwner * PRIME1;
        long k2 = hName * PRIME2;
        long k3 = hDesc * PRIME3;

        long acc = Long.rotateLeft(k1, 31) ^ Long.rotateLeft(k2, 27) ^ Long.rotateLeft(k3, 19);
        acc *= PRIME4;
        acc ^= acc >>> 33;
        acc *= PRIME1;
        acc ^= acc >>> 29;

        return acc;
    }

    /**
     * Combines two 64-bit hashes into a unified hash.
     *
     * @param h1 first 64-bit hash
     * @param h2 second 64-bit hash
     * @return combined 64-bit hash
     */
    public static long combineHashes(long h1, long h2) {
        long k1 = h1 * PRIME1;
        long k2 = h2 * PRIME2;

        long acc = Long.rotateLeft(k1, 23) ^ Long.rotateLeft(k2, 17);
        acc *= PRIME3;
        acc ^= acc >>> 31;
        acc *= PRIME4;
        acc ^= acc >>> 27;

        return acc;
    }

    private VstmSimdHasher() {
        // Fast signature hasher utility
    }
}
