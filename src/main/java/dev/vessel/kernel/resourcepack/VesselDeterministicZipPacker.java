package dev.vessel.kernel.resourcepack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Deterministic ZIP packaging engine for Minecraft client resource packs and Bedrock addons.
 * <p>
 * Ensures 100% reproducible byte-for-byte archive output by enforcing:
 * <ul>
 *   <li>Normalized, strictly alphabetical entry ordering (lexicographical sort)</li>
 *   <li>Fixed epoch timestamp (1980-01-01 00:00:00 UTC) on all ZIP header records</li>
 *   <li>Sanitized ZIP header extra fields to eliminate platform/OS-specific metadata drift</li>
 *   <li>Deterministic DEFLATE compression levels</li>
 * </ul>
 * Computes identical SHA-1 hashes across server restarts for client handshake verification.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 * <!-- Changelog:
 *   1.0.0: Initial implementation of deterministic zip packaging.
 *   1.0.1: Fixed JVM TimeZone dependency via setTimeLocal and deduplicated colliding paths via TreeMap.
 * -->
 */
public final class VesselDeterministicZipPacker {

    /** DOS Epoch timestamp (1980-01-01 00:00:00 UTC) in milliseconds. */
    public static final long DETERMINISTIC_TIMESTAMP_MILLIS = 315532800000L;

    /** Fixed local date-time representing the DOS epoch (1980-01-01 00:00:00). */
    public static final java.time.LocalDateTime DETERMINISTIC_LOCAL_DATETIME = java.time.LocalDateTime.of(1980, 1, 1, 0, 0, 0);

    /** Default compression level (Deflater.BEST_COMPRESSION for maximal pack payload density). */
    public static final int DEFAULT_COMPRESSION_LEVEL = Deflater.BEST_COMPRESSION;

    private final int compressionLevel;

    /**
     * Constructs a deterministic zip packer with the specified compression level.
     *
     * @param compressionLevel compression level between {@link Deflater#DEFAULT_COMPRESSION} and {@link Deflater#BEST_COMPRESSION}
     */
    public VesselDeterministicZipPacker(int compressionLevel) {
        if (compressionLevel < -1 || compressionLevel > 9) {
            throw new IllegalArgumentException("Invalid compression level: " + compressionLevel);
        }
        this.compressionLevel = compressionLevel;
    }

    /**
     * Constructs a deterministic zip packer with default {@link Deflater#BEST_COMPRESSION}.
     */
    public VesselDeterministicZipPacker() {
        this(DEFAULT_COMPRESSION_LEVEL);
    }

    /**
     * Packs entry map into a deterministic ZIP archive byte array.
     *
     * @param entries map of relative entry paths to byte contents
     * @return reproducible ZIP archive byte array
     */
    public byte[] pack(Map<String, byte[]> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        List<VesselDeterministicZipEntry> entryList = new ArrayList<>(entries.size());
        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            entryList.add(new VesselDeterministicZipEntry(entry.getKey(), entry.getValue()));
        }
        return pack(entryList);
    }

    /**
     * Packs entry collection into a deterministic ZIP archive byte array.
     *
     * @param entries collection of archive entries
     * @return reproducible ZIP archive byte array
     */
    public byte[] pack(Collection<VesselDeterministicZipEntry> entries) {
        Objects.requireNonNull(entries, "entries cannot be null");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            packToStream(entries, baos);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to pack deterministic zip in-memory", e);
        }
        return baos.toByteArray();
    }

    /**
     * Packs entry collection directly into an output stream.
     * Deduplicates colliding normalized paths and enforces strict alphabetical ordering.
     *
     * @param entries collection of archive entries
     * @param outputStream target destination stream
     * @throws IOException if an I/O error occurs
     */
    public void packToStream(Collection<VesselDeterministicZipEntry> entries, OutputStream outputStream) throws IOException {
        Objects.requireNonNull(entries, "entries cannot be null");
        Objects.requireNonNull(outputStream, "outputStream cannot be null");

        // Sort entries in strict lexicographical order and deduplicate colliding normalized paths
        java.util.Map<String, VesselDeterministicZipEntry> sortedMap = new java.util.TreeMap<>();
        for (VesselDeterministicZipEntry entry : entries) {
            if (entry != null) {
                sortedMap.put(entry.path(), entry);
            }
        }

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            zos.setLevel(compressionLevel);
            zos.setMethod(ZipOutputStream.DEFLATED);

            for (VesselDeterministicZipEntry item : sortedMap.values()) {
                byte[] payload = item.data();
                ZipEntry zipEntry = new ZipEntry(item.path());

                // Enforce strictly timezone-independent DOS epoch timestamp (1980-01-01 00:00:00)
                zipEntry.setTimeLocal(DETERMINISTIC_LOCAL_DATETIME);
                zipEntry.setExtra(null); // Clear extended OS-specific fields
                zipEntry.setSize(payload.length);

                CRC32 crc = new CRC32();
                crc.update(payload);
                zipEntry.setCrc(crc.getValue());

                zos.putNextEntry(zipEntry);
                zos.write(payload);
                zos.closeEntry();
            }
            zos.finish();
        }
    }

    /**
     * Packs entries into a {@link VirtualResourcePack} ready for client distribution.
     *
     * @param packId unique pack ID
     * @param name human-readable pack title
     * @param platform target client platform (Java or Bedrock)
     * @param entries archive entries map
     * @return virtual resource pack container with pre-computed SHA-1
     */
    public VirtualResourcePack packVirtual(String packId, String name, PackTargetPlatform platform, Map<String, byte[]> entries) {
        byte[] zipBytes = pack(entries);
        return VirtualResourcePack.of(packId, name, platform, zipBytes);
    }

    /**
     * Packs entries into a {@link VirtualResourcePack} ready for client distribution.
     *
     * @param packId unique pack ID
     * @param name human-readable pack title
     * @param platform target client platform (Java or Bedrock)
     * @param entries archive entries collection
     * @return virtual resource pack container with pre-computed SHA-1
     */
    public VirtualResourcePack packVirtual(String packId, String name, PackTargetPlatform platform, Collection<VesselDeterministicZipEntry> entries) {
        byte[] zipBytes = pack(entries);
        return VirtualResourcePack.of(packId, name, platform, zipBytes);
    }

    /**
     * Computes raw SHA-1 digest from archive bytes.
     *
     * @param data binary payload
     * @return 20-byte SHA-1 digest
     */
    public static byte[] computeSha1(byte[] data) {
        Objects.requireNonNull(data, "data cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest unavailable on JVM", e);
        }
    }

    /**
     * Computes 40-character lowercase SHA-1 hex string from archive bytes.
     *
     * @param data binary payload
     * @return 40-character lowercase hexadecimal hash string
     */
    public static String computeSha1Hex(byte[] data) {
        return HexFormat.of().formatHex(computeSha1(data));
    }
}
