package dev.vessel.kernel.vstm.binary;

import dev.vessel.kernel.vstm.builder.VstmbBinaryWriter;
import dev.vessel.kernel.vstm.engine.VstmClassTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmDialectMask;
import dev.vessel.kernel.vstm.engine.VstmFieldTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmMethodTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmTransmutationFastLookup;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests validating MemorySegment mapping, zero-copy off-heap reads,
 * string pool index lookups, and checksum validation.
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbMemoryMappingTest {

    @Test
    @DisplayName("Verify memory-mapped reading and string pool resolution from disk file")
    void testMemoryMappingFromDisk(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("test-matrix.vstmb");

        List<VstmClassTransmutationEntry> classes = List.of(
                VstmClassTransmutationEntry.of("net/minecraft/src/C_1234_", "net/minecraft/world/level/Level", VstmDialectMask.MOJMAP),
                VstmClassTransmutationEntry.of("net/minecraft/src/C_5678_", "net/minecraft/world/entity/Entity", VstmDialectMask.MOJMAP)
        );

        List<VstmFieldTransmutationEntry> fields = List.of(
                VstmFieldTransmutationEntry.of("net/minecraft/world/level/Level", "f_123_", "Z", "isClientSide", VstmDialectMask.MOJMAP)
        );

        List<VstmMethodTransmutationEntry> methods = List.of(
                VstmMethodTransmutationEntry.of("net/minecraft/world/level/Level", "m_456_", "(Lnet/minecraft/world/entity/Entity;)Z", "addFreshEntity", "(Lnet/minecraft/world/entity/Entity;)Z", VstmDialectMask.MOJMAP)
        );

        VstmbBinaryWriter.writeToPath(file, (short) 767, VstmDialectMask.MOJMAP, classes, fields, methods);

        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.size(file)).isGreaterThanOrEqualTo(VstmbCacheLineAligner.HEADER_SIZE);

        try (Arena confinedArena = Arena.ofConfined()) {
            MemorySegment segment = VstmbMemorySegmentMapper.mapReadOnly(file, confinedArena);
            assertThat(segment.byteSize()).isEqualTo(Files.size(file));

            int magic = VstmbOffHeapByteReader.readInt(segment, 0);
            assertThat(VstmbHeaderMagic.isValid(magic)).isTrue();

            short version = VstmbOffHeapByteReader.readShort(segment, 4);
            assertThat(version).isEqualTo(VstmbFormatVersion.CURRENT_VERSION);

            short protocol = VstmbOffHeapByteReader.readShort(segment, 6);
            assertThat(protocol).isEqualTo((short) 767);

            int dialect = VstmbOffHeapByteReader.readInt(segment, 8);
            assertThat(dialect).isEqualTo(VstmDialectMask.MOJMAP);

            long stringPoolOffset = VstmbOffHeapByteReader.readLong(segment, 24);
            int stringCount = VstmbStringPoolIndex.getStringCount(segment, stringPoolOffset);
            assertThat(stringCount).isGreaterThan(0);

            String firstStr = VstmbStringPoolIndex.getString(segment, stringPoolOffset, 0);
            assertThat(firstStr).isEqualTo("net/minecraft/src/C_1234_");

            byte[] dest = new byte[64];
            int bytesRead = VstmbStringPoolIndex.readUtf8Bytes(segment, stringPoolOffset, 0, dest, 0);
            assertThat(new String(dest, 0, bytesRead)).isEqualTo("net/minecraft/src/C_1234_");
        }
    }

    @Test
    @DisplayName("Verify truncated binary buffer rejection with domain exception")
    void testTruncatedBufferRejection() {
        byte[] truncated = new byte[32]; // Less than 64-byte header
        truncated[0] = 0x56;
        truncated[1] = 0x53;
        truncated[2] = 0x54;
        truncated[3] = 0x4D;

        assertThatThrownBy(() -> VstmbMemorySegmentMapper.mapFromByteArray(truncated))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("truncated");
    }

    @Test
    @DisplayName("Verify CRC32C checksum calculation and validation")
    void testChecksumValidation() {
        byte[] payload = "VESSEL_TRANSMUTATION_MATRIX_DATA_PAYLOAD".getBytes();
        int crc = VstmbHeaderChecksumValidator.computeCrc32c(payload, 0, payload.length);
        assertThat(crc).isNotZero();

        VstmbHeaderChecksumValidator.validateCrc32c(crc, crc);

        assertThatThrownBy(() -> VstmbHeaderChecksumValidator.validateCrc32c(crc, crc + 1))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("CRC32C checksum mismatch");
    }

    @Test
    @DisplayName("Verify memory mapping and fast lookup on real compiled MC 1.21.1 matrix")
    void testReal1211CompiledBinaryMapping() throws IOException {
        Path realBinary = Path.of("vstm-distribution/data/binary/vstm-1.21.1.vstmb");
        if (!Files.exists(realBinary)) {
            return;
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment segment = VstmbMemorySegmentMapper.mapReadOnly(realBinary, arena);
            assertThat(segment.byteSize()).isGreaterThan(30L * 1024 * 1024);

            int magic = VstmbOffHeapByteReader.readInt(segment, 0);
            assertThat(VstmbHeaderMagic.isValid(magic)).isTrue();

            short version = VstmbOffHeapByteReader.readShort(segment, 4);
            assertThat(version).isEqualTo(VstmbFormatVersion.CURRENT_VERSION);

            short protocol = VstmbOffHeapByteReader.readShort(segment, 6);
            assertThat(protocol).isEqualTo((short) 767);

            int classCount = VstmbOffHeapByteReader.readInt(segment, 12);
            assertThat(classCount).isGreaterThan(50_000);

            VstmTransmutationFastLookup lookup = new VstmTransmutationFastLookup(segment);
            String mojClass = lookup.lookupClass("net/minecraft/class_1937", VstmDialectMask.INTERMEDIARY, VstmDialectMask.MOJMAP);
            assertThat(mojClass).isEqualTo("net/minecraft/world/level/Level");
        }
    }
}
