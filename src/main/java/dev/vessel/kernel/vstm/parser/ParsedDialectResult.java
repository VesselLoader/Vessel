package dev.vessel.kernel.vstm.parser;

import java.io.Serializable;
import java.util.List;

/**
 * Immutable container encapsulating the complete parsed mapping output from a single dialect source.
 *
 * @param classes     list of parsed class mappings
 * @param fields      list of parsed field mappings
 * @param methods     list of parsed method mappings
 * @param dialectMask the dialect bitmask associated with this parse result
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public record ParsedDialectResult(
        List<ParsedClassMapping> classes,
        List<ParsedFieldMapping> fields,
        List<ParsedMethodMapping> methods,
        int dialectMask
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor creating defensive unmodifiable copies of collection entries.
     */
    public ParsedDialectResult {
        classes = classes != null ? List.copyOf(classes) : List.of();
        fields = fields != null ? List.copyOf(fields) : List.of();
        methods = methods != null ? List.copyOf(methods) : List.of();
    }

    /**
     * Creates an empty dialect result.
     *
     * @param dialectMask dialect identifier
     * @return empty result
     */
    public static ParsedDialectResult empty(int dialectMask) {
        return new ParsedDialectResult(List.of(), List.of(), List.of(), dialectMask);
    }

    /**
     * Returns total number of parsed elements in this result.
     *
     * @return total count
     */
    public int totalEntryCount() {
        return classes.size() + fields.size() + methods.size();
    }
}
