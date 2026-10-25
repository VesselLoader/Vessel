package dev.vessel.kernel.vstm.builder;

import dev.vessel.kernel.vstm.binary.VstmbCacheLineAligner;
import dev.vessel.kernel.vstm.binary.VstmbFormatVersion;
import dev.vessel.kernel.vstm.binary.VstmbHeaderChecksumValidator;
import dev.vessel.kernel.vstm.binary.VstmbHeaderMagic;
import dev.vessel.kernel.vstm.binary.VstmbOffHeapByteReader;
import dev.vessel.kernel.vstm.binary.VstmbStringPoolIndex;
import dev.vessel.kernel.vstm.engine.VstmClassTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmFieldTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmMethodTransmutationEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Serializes transmutation matrix records into canonical .vstmb binary format.
 *
 * <p>Constructs 64-byte cache-line aligned headers, optimized String Pools,
 * and power-of-two open-addressed hash tables for classes, fields, and methods (Catalog ref: STO-0012).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbBinaryWriter {

    private static final int CLASS_SLOT_SIZE = 32;
    private static final int FIELD_SLOT_SIZE = 32;
    private static final int METHOD_SLOT_SIZE = 32;

    /**
     * Serializes transmutation entries and writes the complete .vstmb file to disk.
     *
     * @param destinationPath target output path
     * @param targetProtocol  Minecraft protocol version ID (e.g., 767)
     * @param dialectBitmask  composite dialect mask
     * @param classes         class transmutation entries
     * @param fields          field transmutation entries
     * @param methods         method transmutation entries
     * @throws IOException if writing to disk fails
     */
    public static void writeToPath(
            Path destinationPath,
            short targetProtocol,
            int dialectBitmask,
            List<VstmClassTransmutationEntry> classes,
            List<VstmFieldTransmutationEntry> fields,
            List<VstmMethodTransmutationEntry> methods
    ) throws IOException {
        Objects.requireNonNull(destinationPath, "destinationPath must not be null");
        byte[] binaryData = writeToByteArray(targetProtocol, dialectBitmask, classes, fields, methods);
        if (destinationPath.getParent() != null) {
            Files.createDirectories(destinationPath.getParent());
        }
        Files.write(destinationPath, binaryData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /**
     * Serializes transmutation entries into an in-memory .vstmb byte array.
     *
     * @param targetProtocol Minecraft protocol version ID
     * @param dialectBitmask composite dialect mask
     * @param classes        class transmutation entries
     * @param fields         field transmutation entries
     * @param methods        method transmutation entries
     * @return raw .vstmb binary byte array
     */
    public static byte[] writeToByteArray(
            short targetProtocol,
            int dialectBitmask,
            List<VstmClassTransmutationEntry> classes,
            List<VstmFieldTransmutationEntry> fields,
            List<VstmMethodTransmutationEntry> methods
    ) {
        List<VstmClassTransmutationEntry> safeClasses = classes != null ? classes : List.of();
        List<VstmFieldTransmutationEntry> safeFields = fields != null ? fields : List.of();
        List<VstmMethodTransmutationEntry> safeMethods = methods != null ? methods : List.of();

        // 1. Build string pool table
        Map<String, Integer> stringToIndex = new LinkedHashMap<>();
        List<byte[]> stringBytesList = new ArrayList<>();

        for (VstmClassTransmutationEntry entry : safeClasses) {
            registerString(entry.sourceClass(), stringToIndex, stringBytesList);
            registerString(entry.targetClass(), stringToIndex, stringBytesList);
        }
        for (VstmFieldTransmutationEntry entry : safeFields) {
            registerString(entry.ownerClass(), stringToIndex, stringBytesList);
            registerString(entry.sourceFieldName(), stringToIndex, stringBytesList);
            registerString(entry.sourceDescriptor(), stringToIndex, stringBytesList);
            registerString(entry.targetFieldName(), stringToIndex, stringBytesList);
        }
        for (VstmMethodTransmutationEntry entry : safeMethods) {
            registerString(entry.ownerClass(), stringToIndex, stringBytesList);
            registerString(entry.sourceMethodName(), stringToIndex, stringBytesList);
            registerString(entry.sourceDescriptor(), stringToIndex, stringBytesList);
            registerString(entry.targetMethodName(), stringToIndex, stringBytesList);
            registerString(entry.targetDescriptor(), stringToIndex, stringBytesList);
        }

        // 2. Compute string pool bytes
        int stringCount = stringBytesList.size();
        ByteArrayOutputStream stringDataOut = new ByteArrayOutputStream();
        int[] stringOffsets = new int[stringCount];
        int currentRelativeOffset = 0;

        for (int i = 0; i < stringCount; i++) {
            byte[] bytes = stringBytesList.get(i);
            stringOffsets[i] = currentRelativeOffset;
            stringDataOut.write(bytes.length & 0xff);
            stringDataOut.write((bytes.length >> 8) & 0xff);
            stringDataOut.writeBytes(bytes);
            currentRelativeOffset += (2 + bytes.length);
        }
        byte[] stringDataBytes = stringDataOut.toByteArray();

        // 3. Layout segment offsets
        long headerSize = VstmbCacheLineAligner.HEADER_SIZE;
        long stringPoolOffset = headerSize;
        long stringIndexTableSize = 4L + ((long) stringCount * 4L);
        long rawStringDataBase = stringPoolOffset + stringIndexTableSize;
        long stringDataBaseOffset = VstmbCacheLineAligner.align64(rawStringDataBase);
        long stringPoolTotalSize = (stringDataBaseOffset - stringPoolOffset) + stringDataBytes.length;

        long classTableOffset = VstmbCacheLineAligner.align64(stringPoolOffset + stringPoolTotalSize);
        int classCapacity = calculateCapacity(safeClasses.size());
        long classTableSize = 4L + ((long) classCapacity * CLASS_SLOT_SIZE);

        long fieldTableOffset = VstmbCacheLineAligner.align64(classTableOffset + classTableSize);
        int fieldCapacity = calculateCapacity(safeFields.size());
        long fieldTableSize = 4L + ((long) fieldCapacity * FIELD_SLOT_SIZE);

        long methodTableOffset = VstmbCacheLineAligner.align64(fieldTableOffset + fieldTableSize);
        int methodCapacity = calculateCapacity(safeMethods.size());
        long methodTableSize = 4L + ((long) methodCapacity * METHOD_SLOT_SIZE);

        long totalFileSize = VstmbCacheLineAligner.align64(methodTableOffset + methodTableSize);

        // 4. Allocate memory buffer
        byte[] buffer = new byte[(int) totalFileSize];
        MemorySegment segment = MemorySegment.ofArray(buffer);

        // 5. Populate Header (64 bytes)
        segment.set(ValueLayout.JAVA_BYTE, 0, (byte) 0x56);
        segment.set(ValueLayout.JAVA_BYTE, 1, (byte) 0x53);
        segment.set(ValueLayout.JAVA_BYTE, 2, (byte) 0x54);
        segment.set(ValueLayout.JAVA_BYTE, 3, (byte) 0x4D);
        segment.set(VstmbOffHeapByteReader.SHORT_LAYOUT, 4, VstmbFormatVersion.CURRENT_VERSION);
        segment.set(VstmbOffHeapByteReader.SHORT_LAYOUT, 6, targetProtocol);
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, 8, dialectBitmask);
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, 12, safeClasses.size());
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, 16, safeMethods.size());
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, 20, safeFields.size());
        segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, 24, stringPoolOffset);
        segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, 32, classTableOffset);
        segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, 40, fieldTableOffset);
        segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, 48, methodTableOffset);

        // 6. Populate String Pool
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, stringPoolOffset, stringCount);
        for (int i = 0; i < stringCount; i++) {
            segment.set(VstmbOffHeapByteReader.INT_LAYOUT, stringPoolOffset + 4L + ((long) i * 4L), stringOffsets[i]);
        }
        for (int i = 0; i < stringDataBytes.length; i++) {
            segment.set(ValueLayout.JAVA_BYTE, stringDataBaseOffset + i, stringDataBytes[i]);
        }

        // 7. Populate Class Hash Table
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, classTableOffset, classCapacity);
        int classMask = classCapacity - 1;
        for (VstmClassTransmutationEntry entry : safeClasses) {
            int slot = (int) (entry.sourceHash() & (long) classMask);
            while (true) {
                long slotOffset = classTableOffset + 4L + ((long) slot * CLASS_SLOT_SIZE);
                long existingHash = segment.get(VstmbOffHeapByteReader.LONG_LAYOUT, slotOffset);
                if (existingHash == 0L) {
                    segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, slotOffset, entry.sourceHash());
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 8L, stringToIndex.get(entry.sourceClass()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 12L, stringToIndex.get(entry.targetClass()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 16L, entry.dialectMask());
                    break;
                }
                slot = (slot + 1) & classMask;
            }
        }

        // 8. Populate Field Hash Table
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, fieldTableOffset, fieldCapacity);
        int fieldMask = fieldCapacity - 1;
        for (VstmFieldTransmutationEntry entry : safeFields) {
            int slot = (int) (entry.signatureHash() & (long) fieldMask);
            while (true) {
                long slotOffset = fieldTableOffset + 4L + ((long) slot * FIELD_SLOT_SIZE);
                long existingHash = segment.get(VstmbOffHeapByteReader.LONG_LAYOUT, slotOffset);
                if (existingHash == 0L) {
                    segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, slotOffset, entry.signatureHash());
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 8L, stringToIndex.get(entry.ownerClass()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 12L, stringToIndex.get(entry.sourceFieldName()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 16L, stringToIndex.get(entry.sourceDescriptor()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 20L, stringToIndex.get(entry.targetFieldName()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 24L, entry.dialectMask());
                    break;
                }
                slot = (slot + 1) & fieldMask;
            }
        }

        // 9. Populate Method Hash Table
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, methodTableOffset, methodCapacity);
        int methodMask = methodCapacity - 1;
        for (VstmMethodTransmutationEntry entry : safeMethods) {
            int slot = (int) (entry.signatureHash() & (long) methodMask);
            while (true) {
                long slotOffset = methodTableOffset + 4L + ((long) slot * METHOD_SLOT_SIZE);
                long existingHash = segment.get(VstmbOffHeapByteReader.LONG_LAYOUT, slotOffset);
                if (existingHash == 0L) {
                    segment.set(VstmbOffHeapByteReader.LONG_LAYOUT, slotOffset, entry.signatureHash());
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 8L, stringToIndex.get(entry.ownerClass()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 12L, stringToIndex.get(entry.sourceMethodName()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 16L, stringToIndex.get(entry.sourceDescriptor()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 20L, stringToIndex.get(entry.targetMethodName()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 24L, stringToIndex.get(entry.targetDescriptor()));
                    segment.set(VstmbOffHeapByteReader.INT_LAYOUT, slotOffset + 28L, entry.dialectMask());
                    break;
                }
                slot = (slot + 1) & methodMask;
            }
        }

        // 10. Compute CRC32C of data payload and store in header (offset 56)
        int crc = VstmbHeaderChecksumValidator.computeCrc32c(buffer, VstmbCacheLineAligner.HEADER_SIZE, buffer.length - VstmbCacheLineAligner.HEADER_SIZE);
        segment.set(VstmbOffHeapByteReader.INT_LAYOUT, 56, crc);

        return buffer;
    }

    private static void registerString(String str, Map<String, Integer> stringToIndex, List<byte[]> stringBytesList) {
        if (!stringToIndex.containsKey(str)) {
            int id = stringBytesList.size();
            stringToIndex.put(str, id);
            stringBytesList.add(str.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static int calculateCapacity(int count) {
        int min = Math.max(16, count * 2);
        int capacity = 16;
        while (capacity < min) {
            capacity <<= 1;
        }
        return capacity;
    }

    private VstmbBinaryWriter() {
        // Binary writer utility
    }
}
