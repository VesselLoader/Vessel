package dev.vessel.kernel.vstm.binary;

import java.util.Objects;

/**
 * Validates and exposes the canonical 4-byte Magic Header for .vstmb binary files.
 *
 * <p>Canonical Magic Bytes: {@code 0x56, 0x53, 0x54, 0x4D} (ASCII: {@code "VSTM"}).</p>
 * <p>Canonical Magic Integer (Big-Endian): {@code 0x5653544D}.</p>
 *
 * <p>Protects against structural corruptions, non-VSTM payload injection,
 * and format collision (Catalog refs: UFE-0012, MAP-0045).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmbHeaderMagic {

    /**
     * Canonical 4-byte Magic sequence in ASCII ("VSTM").
     */
    public static final byte[] MAGIC_BYTES = new byte[]{0x56, 0x53, 0x54, 0x4D};

    /**
     * Big-endian 32-bit integer representation of "VSTM" (0x5653544D).
     */
    public static final int MAGIC_INT = 0x5653544D;

    /**
     * Little-endian 32-bit integer representation of "VSTM" (0x4D545356).
     */
    public static final int MAGIC_INT_LE = 0x4D545356;

    /**
     * Header magic length in bytes.
     */
    public static final int MAGIC_LENGTH = 4;

    /**
     * Checks if the provided 32-bit integer matches the canonical VSTM magic header (in either endianness).
     *
     * @param magic the integer value to test
     * @return true if valid, false otherwise
     */
    public static boolean isValid(int magic) {
        return magic == MAGIC_INT || magic == MAGIC_INT_LE;
    }

    /**
     * Checks if the provided byte array starts with the canonical VSTM magic sequence.
     *
     * @param bytes the byte buffer to inspect
     * @return true if valid, false otherwise
     */
    public static boolean isValid(byte[] bytes) {
        if (bytes == null || bytes.length < MAGIC_LENGTH) {
            return false;
        }
        return bytes[0] == MAGIC_BYTES[0]
                && bytes[1] == MAGIC_BYTES[1]
                && bytes[2] == MAGIC_BYTES[2]
                && bytes[3] == MAGIC_BYTES[3];
    }

    /**
     * Enforces that the integer matches the canonical VSTM magic header.
     *
     * @param magic the integer value to validate
     * @throws VstmbBinaryCorruptException if the magic integer is invalid
     */
    public static void validate(int magic) {
        if (!isValid(magic)) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-MAGIC",
                    "Invalid .vstmb magic header: 0x" + Integer.toHexString(magic)
                            + " (expected 0x" + Integer.toHexString(MAGIC_INT) + " / 'VSTM')"
            );
        }
    }

    /**
     * Enforces that the byte array starts with the canonical VSTM magic sequence.
     *
     * @param bytes the byte buffer to validate
     * @throws VstmbBinaryCorruptException if the magic bytes are invalid or buffer is too short
     */
    public static void validate(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes buffer must not be null");
        if (!isValid(bytes)) {
            throw new VstmbBinaryCorruptException(
                    "ERR-VSTM-MAGIC",
                    "Invalid .vstmb magic bytes header (expected 'VSTM')"
            );
        }
    }

    private VstmbHeaderMagic() {
        // Utility / validation class - no instantiation permitted
    }
}
