package dev.vessel.kernel.optimization.chunk;

/**
 * Standard and high-performance compression codecs for Anvil and Linear region storage.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum RegionCompressionFormat {
    GZIP((byte) 1),
    DEFLATE((byte) 2),
    UNCOMPRESSED((byte) 3),
    LZ4((byte) 4),
    ZSTD((byte) 5);

    private final byte typeId;

    RegionCompressionFormat(byte typeId) {
        this.typeId = typeId;
    }

    /**
     * Binary codec type identifier recorded in chunk stream headers.
     *
     * @return codec byte identifier
     */
    public byte typeId() {
        return typeId;
    }

    /**
     * Resolves the compression format from a header type byte.
     *
     * @param id byte identifier from chunk header
     * @return corresponding format, defaults to DEFLATE if unrecognized
     */
    public static RegionCompressionFormat fromId(byte id) {
        return switch (id) {
            case 1 -> GZIP;
            case 2 -> DEFLATE;
            case 3 -> UNCOMPRESSED;
            case 4 -> LZ4;
            case 5 -> ZSTD;
            default -> DEFLATE;
        };
    }
}
