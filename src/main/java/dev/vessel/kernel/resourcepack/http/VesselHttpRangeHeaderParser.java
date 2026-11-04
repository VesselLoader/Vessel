package dev.vessel.kernel.resourcepack.http;

import java.util.Objects;

/**
 * Parser for HTTP {@code Range} request headers conforming to RFC 7233 / RFC 9110.
 * <p>
 * Evaluates byte-range expressions including closed ranges ({@code bytes=0-1024}),
 * open-ended ranges ({@code bytes=2048-}), and suffix ranges ({@code bytes=-512}).
 * Throws {@link VesselRangeNotSatisfiableException} when the requested range cannot be satisfied.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselHttpRangeHeaderParser {

    private static final String BYTES_PREFIX = "bytes=";

    /**
     * Parses an HTTP {@code Range} header string against the known resource byte length.
     *
     * @param rangeHeader raw HTTP {@code Range} header value (e.g. {@code "bytes=0-499"})
     * @param totalLength total size of the resource in bytes
     * @return parsed {@link VesselByteRangeSpec}, or {@code null} if no Range header was supplied
     * @throws VesselRangeNotSatisfiableException if the range is unsatisfiable (HTTP 416)
     */
    public VesselByteRangeSpec parse(String rangeHeader, long totalLength) {
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return null;
        }

        String trimmed = rangeHeader.trim();
        if (!trimmed.toLowerCase(java.util.Locale.ROOT).startsWith(BYTES_PREFIX)) {
            return null; // Not a byte range request, ignore and serve 200 OK
        }

        if (totalLength <= 0) {
            throw new VesselRangeNotSatisfiableException("Resource is empty, cannot satisfy range: " + trimmed, totalLength);
        }

        String spec = trimmed.substring(BYTES_PREFIX.length()).trim();
        // If multiple comma-separated ranges are present, evaluate the first range
        int commaIdx = spec.indexOf(',');
        if (commaIdx >= 0) {
            spec = spec.substring(0, commaIdx).trim();
        }

        int dashIdx = spec.indexOf('-');
        if (dashIdx < 0) {
            throw new VesselRangeNotSatisfiableException("Malformed range specification (missing dash): " + spec, totalLength);
        }

        String startStr = spec.substring(0, dashIdx).trim();
        String endStr = spec.substring(dashIdx + 1).trim();

        long start;
        long end;

        try {
            if (startStr.isEmpty()) {
                // Suffix range: bytes=-500 (last 500 bytes)
                if (endStr.isEmpty()) {
                    throw new VesselRangeNotSatisfiableException("Empty range specification: " + spec, totalLength);
                }
                long suffixLength = Long.parseLong(endStr);
                if (suffixLength <= 0) {
                    throw new VesselRangeNotSatisfiableException("Invalid suffix range length <= 0: " + suffixLength, totalLength);
                }
                start = Math.max(0, totalLength - suffixLength);
                end = totalLength - 1;
            } else if (endStr.isEmpty()) {
                // Open-ended range: bytes=1000- (from 1000 to EOF)
                start = Long.parseLong(startStr);
                if (start < 0 || start >= totalLength) {
                    throw new VesselRangeNotSatisfiableException("Start offset out of bounds: " + start + " for size " + totalLength, totalLength);
                }
                end = totalLength - 1;
            } else {
                // Closed range: bytes=0-1000
                start = Long.parseLong(startStr);
                end = Long.parseLong(endStr);
                if (start < 0 || start >= totalLength || start > end) {
                    throw new VesselRangeNotSatisfiableException("Invalid closed range [" + start + "-" + end + "] for size " + totalLength, totalLength);
                }
                // RFC 7233: if end >= totalLength, truncate to totalLength - 1
                if (end >= totalLength) {
                    end = totalLength - 1;
                }
            }
        } catch (NumberFormatException nfe) {
            throw new VesselRangeNotSatisfiableException("Non-numeric byte range: " + spec, totalLength);
        }

        return new VesselByteRangeSpec(start, end, totalLength);
    }
}
