package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.engine.VstmFieldTransmutationEntry;
import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable intermediate record representing a parsed field mapping definition.
 *
 * <p>Preserves declaring owner and type descriptor to resolve shadowed or overloaded
 * fields across upstream modding environments (Catalog ref: MAP-0012).</p>
 *
 * @param ownerClass       internal JVM name of the declaring class
 * @param sourceFieldName  original obfuscated or intermediary field name
 * @param sourceDescriptor field type descriptor (e.g., "Ljava/lang/String;" or "" if absent)
 * @param targetFieldName  canonical remapped field name
 * @param dialectMask      the dialect validity bitmask
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public record ParsedFieldMapping(
        String ownerClass,
        String sourceFieldName,
        String sourceDescriptor,
        String targetFieldName,
        int dialectMask
) implements Serializable, Comparable<ParsedFieldMapping> {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor validating field mapping invariants.
     */
    public ParsedFieldMapping {
        Objects.requireNonNull(ownerClass, "ownerClass must not be null");
        Objects.requireNonNull(sourceFieldName, "sourceFieldName must not be null");
        ownerClass = ownerClass.trim().replace('.', '/');
        sourceFieldName = sourceFieldName.trim();
        sourceDescriptor = sourceDescriptor != null ? sourceDescriptor.trim() : "";
        targetFieldName = (targetFieldName != null && !targetFieldName.isBlank()) ? targetFieldName.trim() : sourceFieldName;
    }

    /**
     * Converts this parsed field mapping into an immutable engine transmutation entry.
     *
     * @return a new VstmFieldTransmutationEntry
     */
    public VstmFieldTransmutationEntry toTransmutationEntry() {
        return VstmFieldTransmutationEntry.of(ownerClass, sourceFieldName, sourceDescriptor, targetFieldName, dialectMask);
    }

    @Override
    public int compareTo(ParsedFieldMapping o) {
        int cmp = this.ownerClass.compareTo(o.ownerClass);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.sourceFieldName.compareTo(o.sourceFieldName);
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.sourceDescriptor.compareTo(o.sourceDescriptor);
        if (cmp != 0) {
            return cmp;
        }
        return this.targetFieldName.compareTo(o.targetFieldName);
    }
}
