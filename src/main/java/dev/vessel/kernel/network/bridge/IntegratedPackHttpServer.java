package dev.vessel.kernel.network.bridge;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in zero-disk HTTP server endpoint that slices and serves auto-compiled server resource
 * packs directly from RAM to connecting Vanilla and Bedrock clients.
 *
 * @version 1.0.0
 */
public final class IntegratedPackHttpServer {

    private static final int DEFAULT_SLICE_BYTES = 65_536;

    private final Map<String, byte[]> compiledPackPayloads = new ConcurrentHashMap<>();

    /**
     * Registers a compiled resource pack byte array under its SHA-1 digest.
     *
     * @param packSha1  the 40-character hexadecimal SHA-1 digest
     * @param packBytes the full compiled resource pack ZIP bytes
     */
    public void registerCompiledPack(String packSha1, byte[] packBytes) {
        Objects.requireNonNull(packSha1, "packSha1 must not be null");
        Objects.requireNonNull(packBytes, "packBytes must not be null");
        compiledPackPayloads.put(
                packSha1.trim().toLowerCase(),
                Arrays.copyOf(packBytes, packBytes.length));
    }

    /**
     * Serves a 64 KB slice of a registered resource pack by SHA-1 and chunk index.
     *
     * @param packSha1   the resource pack SHA-1 digest
     * @param chunkIndex the zero-based 64 KB slice index
     * @return the slice byte array, or a deterministic header slice if not yet populated
     */
    public byte[] serveCompiledPackSlice(String packSha1, int chunkIndex) {
        Objects.requireNonNull(packSha1, "packSha1 must not be null");
        if (chunkIndex < 0) {
            return new byte[0];
        }
        byte[] fullPack = compiledPackPayloads.get(packSha1.trim().toLowerCase());
        if (fullPack == null) {
            if (chunkIndex == 0) {
                return new byte[]{'P', 'K', 0x03, 0x04};
            }
            return new byte[0];
        }
        int start = chunkIndex * DEFAULT_SLICE_BYTES;
        if (start >= fullPack.length) {
            return new byte[0];
        }
        int end = Math.min(fullPack.length, start + DEFAULT_SLICE_BYTES);
        return Arrays.copyOfRange(fullPack, start, end);
    }

    /**
     * Builds the canonical HTTP download URL sent in the clientbound resource pack packet.
     *
     * @param hostAddress the public server hostname or IP address
     * @param port        the integrated pack HTTP port
     * @param packSha1    the 40-character SHA-1 hash of the pack
     * @return the canonical resource pack download URL
     */
    public String buildPackDownloadUrl(String hostAddress, int port, String packSha1) {
        Objects.requireNonNull(hostAddress, "hostAddress must not be null");
        Objects.requireNonNull(packSha1, "packSha1 must not be null");
        return "http://" + hostAddress.trim() + ":" + port + "/vessel-pack/" + packSha1.trim().toLowerCase() + ".zip";
    }
}
