package dev.vessel.kernel.asset.stream;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable memory representation of a streamable server resource asset.
 * Contains byte content, MIME type, SHA-1 checksum, and timestamp.
 * Extinguishes AST-NET by providing atomic byte access for streaming.
 *
 * @param resourcePath virtual URI path (e.g. "/resourcepack.zip")
 * @param mimeType MIME content type (e.g. "application/zip")
 * @param data byte array of asset payload
 * @param sha1Hex SHA-1 hexadecimal hash string for client verification
 * @param lastModifiedEpochMs last modification timestamp in epoch milliseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record StreamableAssetResource(
        String resourcePath,
        String mimeType,
        byte[] data,
        String sha1Hex,
        long lastModifiedEpochMs
) {

    public StreamableAssetResource {
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        Objects.requireNonNull(mimeType, "mimeType cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        Objects.requireNonNull(sha1Hex, "sha1Hex cannot be null");
    }

    /**
     * Gets the full length of the asset payload in bytes.
     *
     * @return byte count
     */
    public long contentLength() {
        return data.length;
    }

    /**
     * Reads a slice of the byte array within the specified range.
     *
     * @param range valid byte range specification
     * @return byte array slice
     */
    public byte[] readRangeSlice(ByteRangeSpec range) {
        Objects.requireNonNull(range, "range cannot be null");
        int start = (int) range.start();
        int end = (int) range.end();
        int length = (end - start) + 1;
        byte[] slice = new byte[length];
        System.arraycopy(data, start, slice, 0, length);
        return slice;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StreamableAssetResource other)) return false;
        return lastModifiedEpochMs == other.lastModifiedEpochMs
                && resourcePath.equals(other.resourcePath)
                && mimeType.equals(other.mimeType)
                && sha1Hex.equals(other.sha1Hex)
                && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(resourcePath, mimeType, sha1Hex, lastModifiedEpochMs);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }
}
