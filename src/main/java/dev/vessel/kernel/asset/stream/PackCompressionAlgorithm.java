package dev.vessel.kernel.asset.stream;

/**
 * Enumeration of compression algorithms available for Server Resource Pack distribution.
 * Extinguishes AST-OPT.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum PackCompressionAlgorithm {

    /**
     * Maximum efficiency Deflate compression (Level 9 with optimized Huffman/Filtered strategy).
     */
    DEFLATE_OPTIMAL,

    /**
     * Ultra-high ratio Zstandard compression (Compression Level 19).
     */
    ZSTD_LEVEL_19,

    /**
     * Auto-detect algorithm based on client protocol and environment capability.
     */
    AUTO_OPTIMAL
}
