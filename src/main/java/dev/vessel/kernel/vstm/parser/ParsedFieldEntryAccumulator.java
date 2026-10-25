package dev.vessel.kernel.vstm.parser;

/**
 * Mutable accumulator for harmonizing field mappings and merging dialect masks.
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class ParsedFieldEntryAccumulator {

    private final String ownerClass;
    private final String sourceFieldName;
    private final String sourceDescriptor;
    private final String targetFieldName;
    private int dialectMask;

    public ParsedFieldEntryAccumulator(
            String ownerClass,
            String sourceFieldName,
            String sourceDescriptor,
            String targetFieldName,
            int dialectMask
    ) {
        this.ownerClass = ownerClass;
        this.sourceFieldName = sourceFieldName;
        this.sourceDescriptor = sourceDescriptor;
        this.targetFieldName = targetFieldName;
        this.dialectMask = dialectMask;
    }

    public void mergeMask(int additionalMask) {
        this.dialectMask |= additionalMask;
    }

    public String ownerClass() {
        return ownerClass;
    }

    public String sourceFieldName() {
        return sourceFieldName;
    }

    public String sourceDescriptor() {
        return sourceDescriptor;
    }

    public String targetFieldName() {
        return targetFieldName;
    }

    public int dialectMask() {
        return dialectMask;
    }
}
