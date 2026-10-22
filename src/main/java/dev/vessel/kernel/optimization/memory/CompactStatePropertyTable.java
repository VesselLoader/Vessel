package dev.vessel.kernel.optimization.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FerriteCore-inspired compact bit-packed property table for block and fluid states.
 * <p>
 * Replaces heavy {@code Map<Property, Comparable>} allocations with bitwise integer packing.
 * Properties are mapped to bit-ranges inside a 64-bit primitive, allowing property lookups
 * and state equality comparisons to execute in single-cycle bit shifts with zero heap allocations.
 *
 * @version 1.0.0
 */
public final class CompactStatePropertyTable {

    private final List<String> propertyNames;
    private final Map<String, Integer> propertyIndexMap;
    private final int[] bitOffsets;
    private final int[] bitMasks;
    private final int[] valueCounts;
    private int totalBitsUsed;

    /**
     * Constructs a compact property schema table.
     *
     * @param properties map of property names to their respective allowed value counts
     */
    public CompactStatePropertyTable(Map<String, Integer> properties) {
        Objects.requireNonNull(properties, "properties cannot be null");
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties map cannot be empty");
        }

        int count = properties.size();
        this.propertyNames = new ArrayList<>(count);
        this.propertyIndexMap = new HashMap<>(count);
        this.bitOffsets = new int[count];
        this.bitMasks = new int[count];
        this.valueCounts = new int[count];

        int currentBitOffset = 0;
        int index = 0;

        for (Map.Entry<String, Integer> entry : properties.entrySet()) {
            String propName = entry.getKey();
            int numValues = entry.getValue();

            if (numValues <= 0) {
                throw new IllegalArgumentException("Property '" + propName + "' must have at least 1 value");
            }

            int bitsNeeded = 32 - Integer.numberOfLeadingZeros(numValues - 1);
            if (bitsNeeded == 0) {
                bitsNeeded = 1;
            }

            if (currentBitOffset + bitsNeeded > 64) {
                throw new IllegalStateException("Exceeded 64-bit limit for compact state property packing");
            }

            propertyNames.add(propName);
            propertyIndexMap.put(propName, index);
            bitOffsets[index] = currentBitOffset;
            bitMasks[index] = (1 << bitsNeeded) - 1;
            valueCounts[index] = numValues;

            currentBitOffset += bitsNeeded;
            index++;
        }

        this.totalBitsUsed = currentBitOffset;
    }

    /**
     * Packs an array of property value indices into a single primitive 64-bit state key.
     *
     * @param valueIndices array of value indices corresponding to registered properties
     * @return bit-packed state key
     */
    public long packState(int[] valueIndices) {
        if (valueIndices.length != propertyNames.size()) {
            throw new IllegalArgumentException("Expected " + propertyNames.size() + " values, got: " + valueIndices.length);
        }

        long packed = 0L;
        for (int i = 0; i < valueIndices.length; i++) {
            int val = valueIndices[i];
            if (val < 0 || val >= valueCounts[i]) {
                throw new IndexOutOfBoundsException("Value index " + val + " out of bounds for property " + propertyNames.get(i));
            }
            packed |= ((long) (val & bitMasks[i])) << bitOffsets[i];
        }
        return packed;
    }

    /**
     * Unpacks the value index of a property from a packed 64-bit state key.
     *
     * @param packedState the bit-packed state key
     * @param propIndex   the index of the property
     * @return the value index of the property
     */
    public int unpackValueIndex(long packedState, int propIndex) {
        if (propIndex < 0 || propIndex >= propertyNames.size()) {
            throw new IndexOutOfBoundsException("Invalid property index: " + propIndex);
        }
        return (int) ((packedState >>> bitOffsets[propIndex]) & bitMasks[propIndex]);
    }

    /**
     * Unpacks the value index of a property by property name.
     *
     * @param packedState  the bit-packed state key
     * @param propertyName name of the property
     * @return the value index
     */
    public int unpackValueIndex(long packedState, String propertyName) {
        Integer idx = propertyIndexMap.get(propertyName);
        if (idx == null) {
            throw new IllegalArgumentException("Unknown property: " + propertyName);
        }
        return unpackValueIndex(packedState, idx);
    }

    /**
     * Modifies a single property in a packed state key, returning the updated packed key.
     *
     * @param packedState the original bit-packed state key
     * @param propIndex   the property index to modify
     * @param newValue    the new value index
     * @return updated bit-packed state key
     */
    public long withValue(long packedState, int propIndex, int newValue) {
        if (propIndex < 0 || propIndex >= propertyNames.size()) {
            throw new IndexOutOfBoundsException("Invalid property index: " + propIndex);
        }
        if (newValue < 0 || newValue >= valueCounts[propIndex]) {
            throw new IndexOutOfBoundsException("New value " + newValue + " out of bounds for property " + propertyNames.get(propIndex));
        }

        long mask = ((long) bitMasks[propIndex]) << bitOffsets[propIndex];
        long cleared = packedState & ~mask;
        return cleared | (((long) (newValue & bitMasks[propIndex])) << bitOffsets[propIndex]);
    }

    /**
     * Returns the total number of bits required to store this state schema.
     *
     * @return number of bits
     */
    public int getTotalBitsUsed() {
        return totalBitsUsed;
    }

    /**
     * Returns the number of distinct properties registered.
     *
     * @return property count
     */
    public int getPropertyCount() {
        return propertyNames.size();
    }

    /**
     * Returns the registered property name at the given index.
     *
     * @param index property index
     * @return property name
     */
    public String getPropertyName(int index) {
        return propertyNames.get(index);
    }
}
