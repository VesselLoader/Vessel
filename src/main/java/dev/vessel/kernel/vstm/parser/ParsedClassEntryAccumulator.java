package dev.vessel.kernel.vstm.parser;

/**
 * Mutable accumulator for harmonizing class mappings and merging dialect masks.
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class ParsedClassEntryAccumulator {

    private final String sourceClass;
    private final String targetClass;
    private int dialectMask;

    public ParsedClassEntryAccumulator(String sourceClass, String targetClass, int dialectMask) {
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.dialectMask = dialectMask;
    }

    public void mergeMask(int additionalMask) {
        this.dialectMask |= additionalMask;
    }

    public String sourceClass() {
        return sourceClass;
    }

    public String targetClass() {
        return targetClass;
    }

    public int dialectMask() {
        return dialectMask;
    }
}
