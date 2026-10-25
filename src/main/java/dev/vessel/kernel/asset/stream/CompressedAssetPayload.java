package dev.vessel.kernel.asset.stream;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable container encapsulating compressed asset bytes alongside compression metrics and integrity checks.
 * Extinguishes AST-OPT.
 *
 * @param originalSizeBytes raw size before compression
 * @param compressedSizeBytes compressed payload size
 * @param algorithm algorithm utilized
 * @param compressedData compressed byte array
 * @param crc32Checksum CRC-32 checksum of original uncompressed payload
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CompressedAssetPayload(
        long originalSizeBytes,
        long compressedSizeBytes,
        PackCompressionAlgorithm algorithm,
        byte[] compressedData,
        long crc32Checksum
) {

    public CompressedAssetPayload {
        Objects.requireNonNull(algorithm, "algorithm cannot be null");
        Objects.requireNonNull(compressedData, "compressedData cannot be null");
        if (originalSizeBytes < 0) {
            throw new IllegalArgumentException("originalSizeBytes cannot be negative: " + originalSizeBytes);
        }
        if (compressedSizeBytes < 0) {
            throw new IllegalArgumentException("compressedSizeBytes cannot be negative: " + compressedSizeBytes);
        }
    }

    /**
     * Calculates the compression ratio as a fraction (compressed / original).
     *
     * @return ratio between 0.0 and 1.0 (or greater if incompressible)
     */
    public double compressionRatio() {
        if (originalSizeBytes == 0) {
            return 1.0;
        }
        return (double) compressedSizeBytes / (double) originalSizeBytes;
    }

    /**
     * Calculates the percentage of storage/bandwidth saved.
     * Example: 40.0 means 40% reduction in size.
     *
     * @return percentage saved
     */
    public double savedPercentage() {
        if (originalSizeBytes == 0) {
            return 0.0;
        }
        return Math.max(0.0, (1.0 - compressionRatio()) * 100.0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CompressedAssetPayload other)) return false;
        return originalSizeBytes == other.originalSizeBytes
                && compressedSizeBytes == other.compressedSizeBytes
                && crc32Checksum == other.crc32Checksum
                && algorithm == other.algorithm
                && Arrays.equals(compressedData, other.compressedData);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(originalSizeBytes, compressedSizeBytes, algorithm, crc32Checksum);
        result = 31 * result + Arrays.hashCode(compressedData);
        return result;
    }
}
