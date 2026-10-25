package dev.vessel.kernel.asset.stream;

import java.util.Objects;

/**
 * Immutable specification of a contiguous HTTP byte range request.
 * Conforms to RFC 7233 / RFC 9110 HTTP Range specifications.
 * Extinguishes AST-NET by formalizing granular chunk boundaries.
 *
 * @param start inclusive starting byte index (zero-based)
 * @param end inclusive ending byte index
 * @param totalLength total length of the asset in bytes
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ByteRangeSpec(long start, long end, long totalLength) {

    public ByteRangeSpec {
        if (start < 0) {
            throw new IllegalArgumentException("Byte range start cannot be negative: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("Byte range end (" + end + ") cannot be smaller than start (" + start + ")");
        }
        if (totalLength < 0) {
            throw new IllegalArgumentException("Total asset length cannot be negative: " + totalLength);
        }
    }

    /**
     * Calculates the number of bytes encompassed by this range slice.
     *
     * @return byte count in this range
     */
    public long contentLength() {
        return (end - start) + 1L;
    }

    /**
     * Checks if this byte range falls strictly within the asset boundaries.
     *
     * @return true if satisfiable, false otherwise
     */
    public boolean isSatisfiable() {
        return totalLength > 0 && start < totalLength && end < totalLength;
    }

    /**
     * Formats the HTTP Content-Range header value.
     * Example: "bytes 0-1023/4096"
     *
     * @return Content-Range header string
     */
    public String toContentRangeHeader() {
        return "bytes " + start + "-" + end + "/" + totalLength;
    }

    /**
     * Formats the unsatisfiable HTTP Content-Range header value.
     * Example: "bytes *\/4096"
     *
     * @param total total length in bytes
     * @return unsatisfied Content-Range header string
     */
    public static String toUnsatisfiableRangeHeader(long total) {
        return "bytes */" + total;
    }
}
