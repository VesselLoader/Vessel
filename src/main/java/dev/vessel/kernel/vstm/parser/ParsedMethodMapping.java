package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.engine.VstmMethodTransmutationEntry;
import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable intermediate record representing a parsed method mapping definition.
 *
 * <p>Retains method ownership, exact JVM parameter descriptor, and return types
 * across method transmutation boundaries (Catalog ref: MAP-0014).</p>
 *
 * @param ownerClass        internal JVM name of declaring class
 * @param sourceMethodName  original obfuscated or intermediary method name
 * @param sourceDescriptor  original JVM method descriptor
 * @param targetMethodName  canonical remapped method name
 * @param targetDescriptor  canonical remapped method descriptor
 * @param dialectMask       the dialect validity bitmask
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public record ParsedMethodMapping(
        String ownerClass,
        String sourceMethodName,
        String sourceDescriptor,
        String targetMethodName,
        String targetDescriptor,
        int dialectMask
) implements Serializable, Comparable<ParsedMethodMapping> {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor validating method invariants and canonicalizing types.
     */
    public ParsedMethodMapping {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(sourceMethodName, "sourceMethodName must not be null");
        Objects.requireNonNull(sourceDescriptor, "sourceDescriptor must not be null");
        ownerClass = ownerClass.trim().replace('.', '/');
        sourceMethodName = sourceMethodName.trim();
        sourceDescriptor = sourceDescriptor.trim();
        targetMethodName = (targetMethodName != null && !targetMethodName.isBlank()) ? targetMethodName.trim() : sourceMethodName;
        targetDescriptor = (targetDescriptor != null && !targetDescriptor.isBlank()) ? targetDescriptor.trim() : sourceDescriptor;
    }

    /**
     * Converts this parsed method mapping into an immutable engine transmutation entry.
     *
     * @return a new VstmMethodTransmutationEntry
     */
    public VstmMethodTransmutationEntry toTransmutationEntry() {
        return VstmMethodTransmutationEntry.of(
                ownerClass,
                sourceMethodName,
                sourceDescriptor,
                targetMethodName,
                targetDescriptor,
                dialectMask
        );
    }

    @Override
    public int compareTo(ParsedMethodMapping o) {
        int cmp = this.ownerClass.compareTo(o.ownerClass);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.sourceMethodName.compareTo(o.sourceMethodName);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.sourceDescriptor.compareTo(o.sourceDescriptor);
        if (cmp != 0) {
            return cmp;
        }
        return this.targetMethodName.compareTo(o.targetMethodName);
    }
}
