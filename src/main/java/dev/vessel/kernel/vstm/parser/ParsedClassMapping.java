package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.engine.VstmClassTransmutationEntry;
import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable intermediate record representing a parsed class mapping definition.
 *
 * <p>Enforces forward-slash JVM internal naming standard across all mapping dialects
 * prior to insertion into the off-heap VSTM binary matrix (Catalog ref: MAP-0010).</p>
 *
 * @param sourceClass the source class internal name (e.g., "eza" or "net/minecraft/class_7833")
 * @param targetClass the remapped canonical internal name (e.g., "com/mojang/blaze3d/Blaze3D")
 * @param dialectMask the dialect validity bitmask
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public record ParsedClassMapping(
        String sourceClass,
        String targetClass,
        int dialectMask
) implements Serializable, Comparable<ParsedClassMapping> {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor normalizing class names into canonical JVM internal format.
     */
    public ParsedClassMapping {
        Objects.requireNonNull(sourceClass, "sourceClass must not be null");
        Objects.requireNonNull(targetClass, "targetClass must not be null");
        sourceClass = sourceClass.trim().replace('.', '/');
        targetClass = targetClass.trim().replace('.', '/');
    }

    /**
     * Converts this parsed class mapping into an immutable engine transmutation entry.
     *
     * @return a new VstmClassTransmutationEntry
     */
    public VstmClassTransmutationEntry toTransmutationEntry() {
        return VstmClassTransmutationEntry.of(sourceClass, targetClass, dialectMask);
    }

    @Override
    public int compareTo(ParsedClassMapping o) {
        int cmp = this.sourceClass.compareTo(o.sourceClass);
        if (cmp != 0) {
            return cmp;
        }
        return this.targetClass.compareTo(o.targetClass);
    }
}
