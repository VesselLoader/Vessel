package dev.vessel.kernel.vstm.engine;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable transmutation mapping record representing a single class transformation entry.
 *
 * <p>Directly models the bijection between a source JVM class name (e.g. Obfuscated or Intermediary)
 * and its target canonical representation (e.g. MojMap or Spigot NMS) across specific dialects.</p>
 *
 * @param sourceHash   the 64-bit MurmurHash of the source class internal name
 * @param sourceClass  the source class internal name (e.g., "net/minecraft/src/C_1234_")
 * @param targetClass  the target transformed class internal name (e.g., "net/minecraft/world/level/Level")
 * @param dialectMask  the bitmask indicating dialect validity for this entry
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public record VstmClassTransmutationEntry(
        long sourceHash,
        String sourceClass,
        String targetClass,
        int dialectMask
) implements Serializable, Comparable<VstmClassTransmutationEntry> {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor enforcing non-nullity and normalization.
     */
    public VstmClassTransmutationEntry {
        Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");
    }

    /**
     * Convenience factory creating an entry with an automatically computed source hash.
     *
     * @param sourceClass the source internal name
     * @param targetClass the target internal name
     * @param dialectMask dialect validity mask
     * @return newly constructed immutable entry
     */
    public static VstmClassTransmutationEntry of(String sourceClass, String targetClass, int dialectMask) {
        long hash = VstmMurmurHash64.hash64(sourceClass);
        return new VstmClassTransmutationEntry(hash, sourceClass, targetClass, dialectMask);
    }

    @Override
    public int compareTo(VstmClassTransmutationEntry o) {
        int cmp = Long.compare(this.sourceHash, o.sourceHash);
        if (cmp != 0) {
            return cmp;
        }
        return this.sourceClass.compareTo(o.sourceClass);
    }
}
