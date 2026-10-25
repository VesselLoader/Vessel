package dev.vessel.kernel.vstm.binary;

import java.lang.foreign.MemorySegment;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.zip.CRC32C;

/**
 * Validates structural integrity and payload consistency of .vstmb binary files
 * using hardware-accelerated CRC32C and SHA-256 digests.
 *
 * <p>Guards against bit-rot, incomplete file transfers, truncated mappings,
 * and malicious binary tampering (Catalog refs: STO-0089, UFE-0012).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbHeaderChecksumValidator {

    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Computes the 32-bit hardware-accelerated CRC32C checksum of a byte array range.
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @param length the number of bytes to include
     * @return the computed CRC32C integer
     */
    public static int computeCrc32c(byte[] data, int offset, int length) {
        Objects.requireNonNull(data, "data buffer must not be null");
        CRC32C crc = new CRC32C();
        crc.update(data, offset, length);
        return (int) crc.getValue();
    }

    /**
     * Computes the 32-bit CRC32C checksum of an off-heap {@link MemorySegment} slice.
     *
     * @param segment the memory segment
     * @param offset  the starting byte offset
     * @param length  the length in bytes
     * @return the computed CRC32C integer
     */
    public static int computeCrc32c(MemorySegment segment, long offset, long length) {
        VstmbOffHeapByteReader.checkBounds(segment, offset, length);
        CRC32C crc = new CRC32C();
        byte[] buffer = new byte[BUFFER_SIZE];
        long remaining = length;
        long currentOffset = offset;
        while (remaining > 0) {
            int chunk = (int) Math.min(remaining, BUFFER_SIZE);
            VstmbOffHeapByteReader.readBytes(segment, currentOffset, buffer, 0, chunk);
            crc.update(buffer, 0, chunk);
            currentOffset += chunk;
            remaining -= chunk;
        }
        return (int) crc.getValue();
    }

    /**
     * Computes the 256-bit SHA-256 cryptographic digest of an off-heap slice.
     *
     * @param segment the memory segment
     * @param offset  the starting byte offset
     * @param length  the length in bytes
     * @return 32-byte digest array
     */
    public static byte[] computeSha256(MemorySegment segment, long offset, long length) {
        VstmbOffHeapByteReader.checkBounds(segment, offset, length);
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = length;
            long currentOffset = offset;
            while (remaining > 0) {
                int chunk = (int) Math.min(remaining, BUFFER_SIZE);
                VstmbOffHeapByteReader.readBytes(segment, currentOffset, buffer, 0, chunk);
                digest.update(buffer, 0, chunk);
                currentOffset += chunk;
                remaining -= chunk;
            }
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", e);
        }
    }

    /**
     * Validates that the off-heap slice matches the expected SHA-256 checksum.
     *
     * @param segment        the memory segment
     * @param offset         the starting byte offset
     * @param length         the length in bytes
     * @param expectedSha256 the 32-byte expected checksum
     * @throws VstmbBinaryCorruptException if checksum verification fails
     */
    public static void validateSha256(MemorySegment segment, long offset, long length, byte[] expectedSha256) {
        Objects.requireNonNull(expectedSha256, "expectedSha256 must not be null");
        byte[] actual = computeSha256(segment, offset, length);
        if (!Arrays.equals(actual, expectedSha256)) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-CHECKSUM",
                    "SHA-256 checksum mismatch on .vstmb payload: expected=" + Arrays.toString(expectedSha256)
                            + ", actual=" + Arrays.toString(actual)
            );
        }
    }

    /**
     * Validates CRC32C value against expected value.
     *
     * @param actualCrc   the calculated CRC32C
     * @param expectedCrc the expected CRC32C
     * @throws VstmbBinaryCorruptException if values do not match
     */
    public static void validateCrc32c(int actualCrc, int expectedCrc) {
        if (actualCrc != expectedCrc) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-CRC",
                    "CRC32C checksum mismatch: expected=" + expectedCrc + ", actual=" + actualCrc
            );
        }
    }

    private VstmbHeaderChecksumValidator() {
        // Integrity validator utility
    }
}
