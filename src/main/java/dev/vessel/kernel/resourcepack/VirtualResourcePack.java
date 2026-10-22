package dev.vessel.kernel.resourcepack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * In-memory virtual resource pack or Bedrock addon payload.
 * <p>
 * Aggregates mod assets, block textures, and model definitions dynamically in RAM
 * without disk extraction, computing SHA-1 client handshake verification digests automatically.
 * </p>
 *
 * @param packId unique pack identifier
 * @param name human-readable pack title
 * @param platform target edition platform (Java or Bedrock)
 * @param sha1Hex lowercase SHA-1 checksum required by client download handshake
 * @param archiveData raw zip or mcpack binary data
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public record VirtualResourcePack(
        String packId,
        String name,
        PackTargetPlatform platform,
        String sha1Hex,
        byte[] archiveData
) {

    public VirtualResourcePack {
        Objects.requireNonNull(packId, "packId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(platform, "platform cannot be null");
        Objects.requireNonNull(sha1Hex, "sha1Hex cannot be null");
        Objects.requireNonNull(archiveData, "archiveData cannot be null");
    }

    /**
     * Factory constructor computing SHA-1 checksum automatically from raw archive data.
     *
     * @param packId unique pack identifier
     * @param name human-readable name
     * @param platform target edition platform
     * @param archiveData zip or mcpack bytes
     * @return new VirtualResourcePack instance
     */
    public static VirtualResourcePack of(String packId, String name, PackTargetPlatform platform, byte[] archiveData) {
        Objects.requireNonNull(archiveData, "archiveData cannot be null");
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(archiveData);
            String hex = HexFormat.of().formatHex(digest);
            return new VirtualResourcePack(packId, name, platform, hex, archiveData);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm unavailable on JVM", e);
        }
    }

    /**
     * Returns total size in bytes of the resource pack archive.
     *
     * @return byte size
     */
    public int sizeBytes() {
        return archiveData.length;
    }
}
