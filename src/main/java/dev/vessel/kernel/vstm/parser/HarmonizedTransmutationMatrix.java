package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.engine.VstmClassTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmFieldTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmMethodTransmutationEntry;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable aggregated result containing normalized and harmonized transmutation entries
 * ready for direct binary compilation by {@code VstmbBinaryWriter}.
 *
 * @param classes        sorted list of harmonized class transmutation entries
 * @param fields         sorted list of harmonized field transmutation entries
 * @param methods        sorted list of harmonized method transmutation entries
 * @param combinedMask   composite dialect bitmask encompassing all harmonized sources
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public record HarmonizedTransmutationMatrix(
        List<VstmClassTransmutationEntry> classes,
        List<VstmFieldTransmutationEntry> fields,
        List<VstmMethodTransmutationEntry> methods,
        int combinedMask
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor creating immutable unmodifiable defensive copies.
     */
    public HarmonizedTransmutationMatrix {
        classes = classes != null ? List.copyOf(classes) : List.of();
        fields = fields != null ? List.copyOf(fields) : List.of();
        methods = methods != null ? List.copyOf(methods) : List.of();
    }

    /**
     * Total number of transmutation entries contained in the harmonized matrix.
     *
     * @return sum of classes, fields, and methods
     */
    public int totalEntryCount() {
        return classes.size() + fields.size() + methods.size();
    }
}
