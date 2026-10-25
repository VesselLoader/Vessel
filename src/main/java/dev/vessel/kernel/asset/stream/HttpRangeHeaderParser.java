package dev.vessel.kernel.asset.stream;

import java.util.Objects;
import java.util.Optional;

/**
 * High-performance RFC 7233 / RFC 9110 HTTP Range header parser.
 * Parses single-range specifications to enable parallel non-blocking streaming.
 * Extinguishes AST-NET.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class HttpRangeHeaderParser {

    public static final String BYTES_PREFIX = "bytes=";
    public static final char RANGE_SEPARATOR = '-';

    /**
     * Parses the Range header into a ByteRangeSpec against a known total length.
     *
     * @param rangeHeaderValue raw HTTP Range header value (e.g. "bytes=0-1023")
     * @param totalLength total size of the requested asset in bytes
     * @return Optional containing ByteRangeSpec if valid and satisfiable, empty otherwise
     */
    public Optional<ByteRangeSpec> parseRange(String rangeHeaderValue, long totalLength) {
        if (rangeHeaderValue == null || rangeHeaderValue.isBlank() || totalLength <= 0) {
            return Optional.empty();
        }

        String trimmed = rangeHeaderValue.trim();
        if (!trimmed.startsWith(BYTES_PREFIX)) {
            return Optional.empty();
        }

        String spec = trimmed.substring(BYTES_PREFIX.length()).trim();
        int dashIndex = spec.indexOf(RANGE_SEPARATOR);
        if (dashIndex < 0) {
            return Optional.empty();
        }

        try {
            if (dashIndex == 0) {
                // Suffix byte range: e.g. "bytes=-500" -> last 500 bytes
                String suffixStr = spec.substring(1).trim();
                if (suffixStr.isEmpty()) {
                    return Optional.empty();
                }
                long suffixLength = Long.parseLong(suffixStr);
                if (suffixLength <= 0) {
                    return Optional.empty();
                }
                long start = Math.max(0, totalLength - suffixLength);
                long end = totalLength - 1;
                return Optional.of(new ByteRangeSpec(start, end, totalLength));
            }

            String startStr = spec.substring(0, dashIndex).trim();
            long start = Long.parseLong(startStr);
            if (start < 0 || start >= totalLength) {
                return Optional.empty();
            }

            if (dashIndex == spec.length() - 1) {
                // Open-ended range: e.g. "bytes=1000-" -> from 1000 to EOF
                return Optional.of(new ByteRangeSpec(start, totalLength - 1, totalLength));
            }

            String endStr = spec.substring(dashIndex + 1).trim();
            long end = Long.parseLong(endStr);
            if (end < start) {
                return Optional.empty();
            }

            // Cap the end at EOF if client requested beyond EOF
            if (end >= totalLength) {
                end = totalLength - 1;
            }

            return Optional.of(new ByteRangeSpec(start, end, totalLength));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
