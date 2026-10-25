package dev.vessel.kernel.identity;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Ultra-fast zero-heap-allocation hexadecimal UUID encoder and parser.
 * Formats 128-bit UUID primitives to dashed and undashed hex character buffers
 * without string concatenation, boxing, or intermediate allocations (ERR-29, OPT-001..OPT-010).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastUuidHexFormatter {

    public static final int DASHED_LENGTH = 36;
    public static final int UNDASHED_LENGTH = 32;

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final byte[] HEX_VALUES = new byte[256];

    static {
        Arrays.fill(HEX_VALUES, (byte) -1);
        for (int i = 0; i <= 9; i++) {
            HEX_VALUES['0' + i] = (byte) i;
        }
        for (int i = 0; i < 6; i++) {
            HEX_VALUES['a' + i] = (byte) (10 + i);
            HEX_VALUES['A' + i] = (byte) (10 + i);
        }
    }

    private FastUuidHexFormatter() {
    }

    /**
     * Formats a UUID into a standard 36-character dashed hex string.
     *
     * @param uuid source UUID
     * @return standard dashed string representation
     */
    public static String format(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return format(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Formats most and least significant bits into a standard 36-character dashed hex string.
     *
     * @param mostSigBits high 64 bits
     * @param leastSigBits low 64 bits
     * @return standard dashed string representation
     */
    public static String format(long mostSigBits, long leastSigBits) {
        char[] chars = new char[DASHED_LENGTH];
        formatTo(mostSigBits, leastSigBits, chars, 0);
        return new String(chars);
    }

    /**
     * Formats a UUID into a target char array at the given offset with 0 heap allocation.
     *
     * @param uuid source UUID
     * @param dest destination char buffer
     * @param offset starting index in destination
     */
    public static void formatTo(UUID uuid, char[] dest, int offset) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        formatTo(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits(), dest, offset);
    }

    /**
     * Formats 64-bit UUID primitive pairs into a target char array with 0 heap allocation.
     *
     * @param mostSigBits high 64 bits
     * @param leastSigBits low 64 bits
     * @param dest destination char buffer
     * @param offset starting index in destination
     */
    public static void formatTo(long mostSigBits, long leastSigBits, char[] dest, int offset) {
        Objects.requireNonNull(dest, "dest cannot be null");
        if (offset < 0 || offset + DASHED_LENGTH > dest.length) {
            throw new IndexOutOfBoundsException("Buffer capacity insufficient for 36-char UUID format");
        }

        // 8 chars: bits 63..32 of mostSigBits
        formatNibbles(mostSigBits >>> 32, dest, offset, 8);
        dest[offset + 8] = '-';

        // 4 chars: bits 31..16 of mostSigBits
        formatNibbles(mostSigBits >>> 16, dest, offset + 9, 4);
        dest[offset + 13] = '-';

        // 4 chars: bits 15..0 of mostSigBits
        formatNibbles(mostSigBits, dest, offset + 14, 4);
        dest[offset + 18] = '-';

        // 4 chars: bits 63..48 of leastSigBits
        formatNibbles(leastSigBits >>> 48, dest, offset + 19, 4);
        dest[offset + 23] = '-';

        // 12 chars: bits 47..0 of leastSigBits
        formatNibbles(leastSigBits, dest, offset + 24, 12);
    }

    /**
     * Formats a UUID into a 32-character compact undashed hex string.
     *
     * @param uuid source UUID
     * @return undashed hex string
     */
    public static String formatUndashed(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return formatUndashed(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Formats most and least significant bits into a 32-character compact undashed hex string.
     *
     * @param mostSigBits high 64 bits
     * @param leastSigBits low 64 bits
     * @return undashed hex string
     */
    public static String formatUndashed(long mostSigBits, long leastSigBits) {
        char[] chars = new char[UNDASHED_LENGTH];
        formatUndashedTo(mostSigBits, leastSigBits, chars, 0);
        return new String(chars);
    }

    /**
     * Formats a UUID into an undashed char buffer with 0 heap allocation.
     *
     * @param uuid source UUID
     * @param dest destination char buffer
     * @param offset starting index
     */
    public static void formatUndashedTo(UUID uuid, char[] dest, int offset) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        formatUndashedTo(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits(), dest, offset);
    }

    /**
     * Formats primitive 64-bit UUID components into an undashed char buffer with 0 heap allocation.
     *
     * @param mostSigBits high 64 bits
     * @param leastSigBits low 64 bits
     * @param dest destination char buffer
     * @param offset starting index
     */
    public static void formatUndashedTo(long mostSigBits, long leastSigBits, char[] dest, int offset) {
        Objects.requireNonNull(dest, "dest cannot be null");
        if (offset < 0 || offset + UNDASHED_LENGTH > dest.length) {
            throw new IndexOutOfBoundsException("Buffer capacity insufficient for 32-char UUID format");
        }

        formatNibbles(mostSigBits, dest, offset, 16);
        formatNibbles(leastSigBits, dest, offset + 16, 16);
    }

    private static void formatNibbles(long value, char[] dest, int offset, int count) {
        for (int i = count - 1; i >= 0; i--) {
            dest[offset + i] = HEX_DIGITS[(int) (value & 0x0F)];
            value >>>= 4;
        }
    }

    /**
     * Parses a UUID from a CharSequence (dashed or undashed) without intermediate string allocation.
     *
     * @param input character sequence containing UUID
     * @return parsed UUID
     */
    public static UUID parse(CharSequence input) {
        Objects.requireNonNull(input, "input cannot be null");
        int length = input.length();
        if (length == DASHED_LENGTH) {
            return parseDashed(input, 0);
        } else if (length == UNDASHED_LENGTH) {
            return parseUndashed(input, 0);
        }
        throw new IllegalArgumentException("Invalid UUID string length: " + length + ", expected 36 or 32");
    }

    /**
     * Parses a UUID from a char array slice without intermediate allocations.
     *
     * @param chars source char array
     * @param offset starting index
     * @param length length of segment (36 or 32)
     * @return parsed UUID
     */
    public static UUID parse(char[] chars, int offset, int length) {
        Objects.requireNonNull(chars, "chars cannot be null");
        if (length == DASHED_LENGTH) {
            return parseDashed(chars, offset);
        } else if (length == UNDASHED_LENGTH) {
            return parseUndashed(chars, offset);
        }
        throw new IllegalArgumentException("Invalid UUID char slice length: " + length + ", expected 36 or 32");
    }

    private static UUID parseDashed(CharSequence s, int off) {
        if (s.charAt(off + 8) != '-' || s.charAt(off + 13) != '-'
                || s.charAt(off + 18) != '-' || s.charAt(off + 23) != '-') {
            throw new IllegalArgumentException("Invalid dashed UUID format: missing hyphen delimiter");
        }

        long mostSig = parseHexSub(s, off, 8);
        mostSig = (mostSig << 16) | parseHexSub(s, off + 9, 4);
        mostSig = (mostSig << 16) | parseHexSub(s, off + 14, 4);

        long leastSig = parseHexSub(s, off + 19, 4);
        leastSig = (leastSig << 48) | parseHexSub(s, off + 24, 12);

        return new UUID(mostSig, leastSig);
    }

    private static UUID parseDashed(char[] s, int off) {
        if (s[off + 8] != '-' || s[off + 13] != '-' || s[off + 18] != '-' || s[off + 23] != '-') {
            throw new IllegalArgumentException("Invalid dashed UUID format: missing hyphen delimiter");
        }

        long mostSig = parseHexSub(s, off, 8);
        mostSig = (mostSig << 16) | parseHexSub(s, off + 9, 4);
        mostSig = (mostSig << 16) | parseHexSub(s, off + 14, 4);

        long leastSig = parseHexSub(s, off + 19, 4);
        leastSig = (leastSig << 48) | parseHexSub(s, off + 24, 12);

        return new UUID(mostSig, leastSig);
    }

    private static UUID parseUndashed(CharSequence s, int off) {
        long mostSig = parseHexSub(s, off, 16);
        long leastSig = parseHexSub(s, off + 16, 16);
        return new UUID(mostSig, leastSig);
    }

    private static UUID parseUndashed(char[] s, int off) {
        long mostSig = parseHexSub(s, off, 16);
        long leastSig = parseHexSub(s, off + 16, 16);
        return new UUID(mostSig, leastSig);
    }

    private static long parseHexSub(CharSequence s, int start, int count) {
        long result = 0;
        for (int i = 0; i < count; i++) {
            char c = s.charAt(start + i);
            if (c >= 256) {
                throw new IllegalArgumentException("Non-ASCII character in hex UUID at index " + (start + i));
            }
            int val = HEX_VALUES[c];
            if (val < 0) {
                throw new IllegalArgumentException("Invalid hex character '" + c + "' at index " + (start + i));
            }
            result = (result << 4) | val;
        }
        return result;
    }

    private static long parseHexSub(char[] s, int start, int count) {
        long result = 0;
        for (int i = 0; i < count; i++) {
            char c = s[start + i];
            if (c >= 256) {
                throw new IllegalArgumentException("Non-ASCII character in hex UUID at index " + (start + i));
            }
            int val = HEX_VALUES[c];
            if (val < 0) {
                throw new IllegalArgumentException("Invalid hex character '" + c + "' at index " + (start + i));
            }
            result = (result << 4) | val;
        }
        return result;
    }
}
