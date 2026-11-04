package dev.vessel.kernel.resourcepack.http;

import java.util.Objects;

/**
 * Immutable specification representing a satisfiable HTTP byte range request.
 * <p>
 * Encapsulates the inclusive start and end byte offsets together with the total length
 * of the underlying resource, computing canonical {@code Content-Range} headers and content lengths.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @param start inclusive start byte offset (0-indexed)
 * @param end inclusive end byte offset (0-indexed)
 * @param totalLength total size of the resource in bytes
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VesselByteRangeSpec(
        long start,
        long end,
        long totalLength
) {

    public VesselByteRangeSpec {
        if (totalLength < 0) {
            throw new IllegalArgumentException("totalLength cannot be negative: " + totalLength);
        }
        if (start < 0) {
            throw new IllegalArgumentException("start offset cannot be negative: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("end offset (" + end + ") cannot be less than start (" + start + ")");
        }
        if (totalLength > 0 && end >= totalLength) {
            throw new IllegalArgumentException("end offset (" + end + ") exceeds total length (" + totalLength + ")");
        }
    }

    /**
     * Calculates the number of bytes contained in this byte range.
     *
     * @return number of bytes in range ({@code end - start + 1})
     */
    public long contentLength() {
        return end - start + 1;
    }

    /**
     * Formats the standard HTTP {@code Content-Range} header value.
     *
     * @return header value formatted as {@code "bytes <start>-<end>/<totalLength>"}
     */
    public String toContentRangeHeader() {
        return "bytes " + start + "-" + end + "/" + totalLength;
    }

    /**
     * Formats the standard HTTP {@code Content-Range} header value for unsatisfiable requests.
     *
     * @param totalLength total resource size
     * @return header value formatted as {@code "bytes * / <totalLength>"}
     */
    public static String toUnsatisfiableHeader(long totalLength) {
        return "bytes */" + totalLength;
    }
}
