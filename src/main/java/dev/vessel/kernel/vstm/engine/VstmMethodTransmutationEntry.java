package dev.vessel.kernel.vstm.engine;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable transmutation mapping record representing a single method transformation entry.
 *
 * <p>Encompasses method name and descriptor remapping across JVM bytecode calling boundaries,
 * preserving exact return types and argument signatures (Catalog refs: MAP-0001 to MAP-0050).</p>
 *
 * @param signatureHash    the 64-bit composite hash of owner + sourceMethodName + sourceDescriptor
 * @param ownerClass       the internal name of the declaring class
 * @param sourceMethodName the original method name
 * @param sourceDescriptor the original method parameter and return descriptor
 * @param targetMethodName the remapped method name
 * @param targetDescriptor the remapped method parameter and return descriptor
 * @param dialectMask      the dialect validity bitmask
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public record VstmMethodTransmutationEntry(
        long signatureHash,
        String ownerClass,
        String sourceMethodName,
        String sourceDescriptor,
        String targetMethodName,
        String targetDescriptor,
        int dialectMask
) implements Serializable, Comparable<VstmMethodTransmutationEntry> {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor validating invariants.
     */
    public VstmMethodTransmutationEntry {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(sourceMethodName, "sourceMethodName must not be null");
        Objects.requireNonNull(sourceDescriptor, "sourceDescriptor must not be null");
        Objects.requireNonNull(targetMethodName, "targetMethodName must not be null");
        Objects.requireNonNull(targetDescriptor, "targetDescriptor must not be null");
    }

    /**
     * Convenience factory automatically calculating the compound signature hash.
     *
     * @param ownerClass       declaring class internal name
     * @param sourceMethodName original method name
     * @param sourceDescriptor original method descriptor
     * @param targetMethodName remapped method name
     * @param targetDescriptor remapped method descriptor
     * @param dialectMask      dialect bitmask
     * @return immutable method entry
     */
    public static VstmMethodTransmutationEntry of(
            String ownerClass,
            String sourceMethodName,
            String sourceDescriptor,
            String targetMethodName,
            String targetDescriptor,
            int dialectMask
    ) {
        long hash = VstmSimdHasher.hashSignature64(ownerClass, sourceMethodName, sourceDescriptor);
        return new VstmMethodTransmutationEntry(
                hash,
                ownerClass,
                sourceMethodName,
                sourceDescriptor,
                targetMethodName,
                targetDescriptor,
                dialectMask
        );
    }

    @Override
    public int compareTo(VstmMethodTransmutationEntry o) {
        int cmp = Long.compare(this.signatureHash, o.signatureHash);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.ownerClass.compareTo(o.ownerClass);
        if (cmp != 0) {
            return cmp;
        }
        return this.sourceMethodName.compareTo(o.sourceMethodName);
    }
}
