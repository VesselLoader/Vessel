package dev.vessel.kernel.vstm.engine;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable transmutation mapping record representing a single field transformation entry.
 *
 * <p>Preserves field owner context and descriptor to prevent signature collisions
 * in shadowed or overloaded field members (Catalog ref: MAP-0112).</p>
 *
 * @param signatureHash    the 64-bit composite hash of owner + sourceFieldName + sourceDescriptor
 * @param ownerClass       the internal name of the declaring class
 * @param sourceFieldName  the original name of the field
 * @param sourceDescriptor the field descriptor (e.g., "Ljava/lang/String;")
 * @param targetFieldName  the remapped field name
 * @param dialectMask      the dialect validity bitmask
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public record VstmFieldTransmutationEntry(
        long signatureHash,
        String ownerClass,
        String sourceFieldName,
        String sourceDescriptor,
        String targetFieldName,
        int dialectMask
) implements Serializable, Comparable<VstmFieldTransmutationEntry> {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor validating invariants.
     */
    public VstmFieldTransmutationEntry {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(sourceFieldName, "sourceFieldName must not be null");
        Objects.requireNonNull(sourceDescriptor, "sourceDescriptor must not be null");
        Objects.requireNonNull(targetFieldName, "targetFieldName must not be null");
    }

    /**
     * Convenience factory automatically calculating the compound signature hash.
     *
     * @param ownerClass       declaring class internal name
     * @param sourceFieldName  original field name
     * @param sourceDescriptor field type descriptor
     * @param targetFieldName  remapped field name
     * @param dialectMask      dialect bitmask
     * @return immutable field entry
     */
    public static VstmFieldTransmutationEntry of(
            String ownerClass,
            String sourceFieldName,
            String sourceDescriptor,
            String targetFieldName,
            int dialectMask
    ) {
        long hash = VstmSimdHasher.hashSignature64(ownerClass, sourceFieldName, sourceDescriptor);
        return new VstmFieldTransmutationEntry(hash, ownerClass, sourceFieldName, sourceDescriptor, targetFieldName, dialectMask);
    }

    @Override
    public int compareTo(VstmFieldTransmutationEntry o) {
        int cmp = Long.compare(this.signatureHash, o.signatureHash);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.ownerClass.compareTo(o.ownerClass);
        if (cmp != 0) {
            return cmp;
        }
        return this.sourceFieldName.compareTo(o.sourceFieldName);
    }
}
