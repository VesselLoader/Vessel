package dev.vessel.kernel.vstm.binary;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Maps .vstmb binary files directly into off-heap memory using Java 21 {@link MemorySegment}
 * and {@link Arena#ofAuto()} memory management.
 *
 * <p>Enables zero-copy, zero-allocation memory-mapped file access, completely bypassing
 * the Java GC heap. Enforces strict pre-flight validation on magic headers, version,
 * and header boundary alignment (Catalog refs: ZER-0010, VOL7-0100, ERR-0003).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbMemorySegmentMapper {

    /**
     * Maps a .vstmb file into a read-only off-heap memory segment with an automatic GC-linked Arena.
     *
     * @param filePath the path to the .vstmb file
     * @return the mapped MemorySegment
     * @throws IOException                 if an I/O error occurs opening the channel
     * @throws VstmbBinaryCorruptException if the header or file structure is corrupt
     */
    public static MemorySegment mapReadOnly(Path filePath) throws IOException {
        return mapReadOnly(filePath, Arena.ofAuto());
    }

    /**
     * Maps a .vstmb file into a read-only off-heap memory segment with a specified caller Arena.
     *
     * @param filePath the path to the .vstmb file
     * @param arena    the memory arena managing lifetime
     * @return the mapped MemorySegment
     * @throws IOException                 if an I/O error occurs opening the channel
     * @throws VstmbBinaryCorruptException if the header or file structure is corrupt
     */
    public static MemorySegment mapReadOnly(Path filePath, Arena arena) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        Objects.requireNonNull(arena, "arena must not be null");

        if (!Files.exists(filePath)) {
            throw new VstmbBinaryCorruptException("ERR-VSTM-NOT-FOUND", "VSTM binary file does not exist: " + filePath);
        }
        if (!Files.isRegularFile(filePath)) {
            throw new VstmbBinaryCorruptException("ERR-VSTM-NOT-FILE", "VSTM path is not a regular file: " + filePath);
        }

        long fileSize = Files.size(filePath);
        if (fileSize < VstmbCacheLineAligner.HEADER_SIZE) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-TRUNCATED",
                    "VSTM binary file truncated: size " + fileSize + " bytes (minimum header: "
                            + VstmbCacheLineAligner.HEADER_SIZE + " bytes)"
            );
        }

        MemorySegment segment;
        try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
            segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize, arena);
        }

        validateHeader(segment);
        return segment;
    }

    /**
     * Creates an in-memory segment from a byte array (used in unit tests and dynamic memory pipelines).
     *
     * @param data the byte array containing the .vstmb binary
     * @return a read-only MemorySegment representing the data
     */
    public static MemorySegment mapFromByteArray(byte[] data) {
        Objects.requireNonNull(data, "data must not be null");
        if (data.length < VstmbCacheLineAligner.HEADER_SIZE) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-TRUNCATED",
                    "VSTM binary buffer truncated: " + data.length + " bytes"
            );
        }

        MemorySegment segment = MemorySegment.ofArray(data);
        validateHeader(segment);
        return segment;
    }

    /**
     * Validates the 64-byte canonical header of a mapped VSTM binary segment.
     *
     * @param segment the memory segment to validate
     */
    private static void validateHeader(MemorySegment segment) {
        int magic = VstmbOffHeapByteReader.readInt(segment, 0);
        VstmbHeaderMagic.validate(magic);

        short version = VstmbOffHeapByteReader.readShort(segment, 4);
        VstmbFormatVersion.validate(version);
    }

    private VstmbMemorySegmentMapper() {
        // Factory utility class
    }
}
