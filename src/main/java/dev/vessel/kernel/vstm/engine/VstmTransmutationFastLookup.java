package dev.vessel.kernel.vstm.engine;

import dev.vessel.kernel.vstm.binary.VstmbCacheLineAligner;
import dev.vessel.kernel.vstm.binary.VstmbOffHeapByteReader;
import dev.vessel.kernel.vstm.binary.VstmbStringPoolIndex;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * High-speed $O(1)$ zero-allocation transmutation lookup engine.
 *
 * <p>Directly inspects 64-byte cache-line aligned off-heap hash tables from mapped .vstmb binaries,
 * avoiding object creation and GC pauses during the hot-path 50ms tick cycle (Catalog refs: ZER-0001, ZER-0010).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmTransmutationFastLookup {

    private static final int CLASS_SLOT_SIZE = 32;
    private static final int FIELD_SLOT_SIZE = 32;
    private static final int METHOD_SLOT_SIZE = 32;
    private static final int MAX_PROBE_DEPTH = 32;

    private final MemorySegment segment;
    private final long stringPoolOffset;
    private final long classTableOffset;
    private final long fieldTableOffset;
    private final long methodTableOffset;
    private final int classTableCapacity;
    private final int fieldTableCapacity;
    private final int methodTableCapacity;

    /**
     * Constructs a fast lookup engine bound to a memory-mapped .vstmb segment.
     *
     * @param segment the memory segment containing the mapped .vstmb file
     */
    public VstmTransmutationFastLookup(MemorySegment segment) {
        this.segment = Objects.requireNonNull(segment, "segment must not be null");

        // Read offsets from 64-byte header
        this.stringPoolOffset = VstmbOffHeapByteReader.readLong(segment, 24);
        this.classTableOffset = VstmbOffHeapByteReader.readLong(segment, 32);
        this.fieldTableOffset = VstmbOffHeapByteReader.readLong(segment, 40);
        this.methodTableOffset = VstmbOffHeapByteReader.readLong(segment, 48);

        this.classTableCapacity = classTableOffset > 0 ? VstmbOffHeapByteReader.readInt(segment, classTableOffset) : 0;
        this.fieldTableCapacity = fieldTableOffset > 0 ? VstmbOffHeapByteReader.readInt(segment, fieldTableOffset) : 0;
        this.methodTableCapacity = methodTableOffset > 0 ? VstmbOffHeapByteReader.readInt(segment, methodTableOffset) : 0;
    }

    /**
     * Looks up the remapped class internal name for a given source class name.
     *
     * @param sourceClass   the source class internal name (e.g., "net/minecraft/src/C_1234_")
     * @param sourceDialect the source dialect flag (e.g., VstmDialectMask.INTERMEDIARY)
     * @param targetDialect the target dialect flag (e.g., VstmDialectMask.MOJMAP)
     * @return the remapped target class name, or null if no mapping is recorded
     */
    public String lookupClass(String sourceClass, int sourceDialect, int targetDialect) {
        if (classTableCapacity == 0 || sourceClass == null || sourceClass.isEmpty()) {
            return null;
        }

        long hash = VstmMurmurHash64.hash64(sourceClass);
        int mask = classTableCapacity - 1;
        int initialSlot = (int) (hash & (long) mask);

        for (int i = 0; i < MAX_PROBE_DEPTH; i++) {
            int slot = (initialSlot + i) & mask;
            long slotOffset = classTableOffset + 4L + ((long) slot * CLASS_SLOT_SIZE);
            long storedHash = VstmbOffHeapByteReader.readLong(segment, slotOffset);

            if (storedHash == 0L) {
                return null; // Empty slot, key does not exist
            }

            if (storedHash == hash) {
                int dialectMask = VstmbOffHeapByteReader.readInt(segment, slotOffset + 16L);
                if ((dialectMask & targetDialect) != 0 || targetDialect == 0) {
                    int targetClassStrId = VstmbOffHeapByteReader.readInt(segment, slotOffset + 12L);
                    return VstmbStringPoolIndex.getString(segment, stringPoolOffset, targetClassStrId);
                }
            }
        }
        return null;
    }

    /**
     * Looks up the remapped field name for a given owner, field name, and descriptor.
     *
     * @param ownerClass       the declaring class internal name
     * @param sourceFieldName  the original field name
     * @param sourceDescriptor the field descriptor
     * @param targetDialect    the requested target dialect
     * @return the remapped field name, or null if not found
     */
    public String lookupField(String ownerClass, String sourceFieldName, String sourceDescriptor, int targetDialect) {
        if (fieldTableCapacity == 0 || ownerClass == null || sourceFieldName == null || sourceDescriptor == null) {
            return null;
        }

        long hash = VstmSimdHasher.hashSignature64(ownerClass, sourceFieldName, sourceDescriptor);
        int mask = fieldTableCapacity - 1;
        int initialSlot = (int) (hash & (long) mask);

        for (int i = 0; i < MAX_PROBE_DEPTH; i++) {
            int slot = (initialSlot + i) & mask;
            long slotOffset = fieldTableOffset + 4L + ((long) slot * FIELD_SLOT_SIZE);
            long storedHash = VstmbOffHeapByteReader.readLong(segment, slotOffset);

            if (storedHash == 0L) {
                return null;
            }

            if (storedHash == hash) {
                int dialectMask = VstmbOffHeapByteReader.readInt(segment, slotOffset + 24L);
                if ((dialectMask & targetDialect) != 0 || targetDialect == 0) {
                    int targetNameStrId = VstmbOffHeapByteReader.readInt(segment, slotOffset + 20L);
                    return VstmbStringPoolIndex.getString(segment, stringPoolOffset, targetNameStrId);
                }
            }
        }
        return null;
    }

    /**
     * Looks up the remapped method name for a given owner, method name, and descriptor.
     *
     * @param ownerClass        the declaring class internal name
     * @param sourceMethodName  the original method name
     * @param sourceDescriptor  the method descriptor
     * @param targetDialect     the requested target dialect
     * @return the remapped method name, or null if not found
     */
    public String lookupMethod(String ownerClass, String sourceMethodName, String sourceDescriptor, int targetDialect) {
        if (methodTableCapacity == 0 || ownerClass == null || sourceMethodName == null || sourceDescriptor == null) {
            return null;
        }

        long hash = VstmSimdHasher.hashSignature64(ownerClass, sourceMethodName, sourceDescriptor);
        int mask = methodTableCapacity - 1;
        int initialSlot = (int) (hash & (long) mask);

        for (int i = 0; i < MAX_PROBE_DEPTH; i++) {
            int slot = (initialSlot + i) & mask;
            long slotOffset = methodTableOffset + 4L + ((long) slot * METHOD_SLOT_SIZE);
            long storedHash = VstmbOffHeapByteReader.readLong(segment, slotOffset);

            if (storedHash == 0L) {
                return null;
            }

            if (storedHash == hash) {
                int dialectMask = VstmbOffHeapByteReader.readInt(segment, slotOffset + 28L);
                if ((dialectMask & targetDialect) != 0 || targetDialect == 0) {
                    int targetNameStrId = VstmbOffHeapByteReader.readInt(segment, slotOffset + 20L);
                    return VstmbStringPoolIndex.getString(segment, stringPoolOffset, targetNameStrId);
                }
            }
        }
        return null;
    }

    /**
     * Looks up the remapped method descriptor.
     *
     * @param ownerClass       the declaring class internal name
     * @param sourceMethodName the original method name
     * @param sourceDescriptor the original method descriptor
     * @param targetDialect    the requested target dialect
     * @return the remapped method descriptor, or null if not found
     */
    public String lookupMethodDescriptor(String ownerClass, String sourceMethodName, String sourceDescriptor, int targetDialect) {
        if (methodTableCapacity == 0 || ownerClass == null || sourceMethodName == null || sourceDescriptor == null) {
            return null;
        }

        long hash = VstmSimdHasher.hashSignature64(ownerClass, sourceMethodName, sourceDescriptor);
        int mask = methodTableCapacity - 1;
        int initialSlot = (int) (hash & (long) mask);

        for (int i = 0; i < MAX_PROBE_DEPTH; i++) {
            int slot = (initialSlot + i) & mask;
            long slotOffset = methodTableOffset + 4L + ((long) slot * METHOD_SLOT_SIZE);
            long storedHash = VstmbOffHeapByteReader.readLong(segment, slotOffset);

            if (storedHash == 0L) {
                return null;
            }

            if (storedHash == hash) {
                int dialectMask = VstmbOffHeapByteReader.readInt(segment, slotOffset + 28L);
                if ((dialectMask & targetDialect) != 0 || targetDialect == 0) {
                    int targetDescStrId = VstmbOffHeapByteReader.readInt(segment, slotOffset + 24L);
                    return VstmbStringPoolIndex.getString(segment, stringPoolOffset, targetDescStrId);
                }
            }
        }
        return null;
    }
}
