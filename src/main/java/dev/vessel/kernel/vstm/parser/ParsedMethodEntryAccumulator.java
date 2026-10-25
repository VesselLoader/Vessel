package dev.vessel.kernel.vstm.parser;

/**
 * Mutable accumulator for harmonizing method mappings and merging dialect masks.
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class ParsedMethodEntryAccumulator {

    private final String ownerClass;
    private final String sourceMethodName;
    private final String sourceDescriptor;
    private final String targetMethodName;
    private final String targetDescriptor;
    private int dialectMask;

    public ParsedMethodEntryAccumulator(
            String ownerClass,
            String sourceMethodName,
            String sourceDescriptor,
            String targetMethodName,
            String targetDescriptor,
            int dialectMask
    ) {
        this.ownerClass = ownerClass;
        this.sourceMethodName = sourceMethodName;
        this.sourceDescriptor = sourceDescriptor;
        this.targetMethodName = targetMethodName;
        this.targetDescriptor = targetDescriptor;
        this.dialectMask = dialectMask;
    }

    public void mergeMask(int additionalMask) {
        this.dialectMask |= additionalMask;
    }

    public String ownerClass() {
        return ownerClass;
    }

    public String sourceMethodName() {
        return sourceMethodName;
    }

    public String sourceDescriptor() {
        return sourceDescriptor;
    }

    public String targetMethodName() {
        return targetMethodName;
    }

    public String targetDescriptor() {
        return targetDescriptor;
    }

    public int dialectMask() {
        return dialectMask;
    }
}
