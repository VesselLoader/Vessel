package dev.vessel.kernel.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * SHA-256 content verification utility for verifying input JAR sidecars and cached artifacts.
 * <p>
 * Ensures reproducible builds and prevents corrupted or tampered module payloads from executing.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-4)
 */
public class JarContentHashVerifier {

    private static final String SHA_256 = "SHA-256";

    /**
     * Computes the SHA-256 hex digest of a byte array.
     *
     * @param data the payload bytes
     * @return lowercase hex string representation
     */
    public String computeSha256(byte[] data) {
        Objects.requireNonNull(data, "data cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available on current JVM", e);
        }
    }

    /**
     * Computes the SHA-256 hex digest of an input stream.
     *
     * @param stream the stream to read and hash
     * @return lowercase hex string representation
     * @throws IOException if an error occurs reading the stream
     */
    public String computeSha256(InputStream stream) throws IOException {
        Objects.requireNonNull(stream, "stream cannot be null");
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available on current JVM", e);
        }
    }

    /**
     * Verifies that the computed SHA-256 matches the expected checksum string.
     *
     * @param data payload bytes
     * @param expectedHash expected hex digest (case-insensitive)
     * @return true if checksums match
     */
    public boolean verify(byte[] data, String expectedHash) {
        Objects.requireNonNull(expectedHash, "expectedHash cannot be null");
        String actual = computeSha256(data);
        return actual.equalsIgnoreCase(expectedHash.trim());
    }

    /**
     * Verifies that the input stream's computed SHA-256 matches the expected checksum string.
     *
     * @param stream the input stream
     * @param expectedHash expected hex digest (case-insensitive)
     * @return true if checksums match
     * @throws IOException if stream reading fails
     */
    public boolean verify(InputStream stream, String expectedHash) throws IOException {
        Objects.requireNonNull(expectedHash, "expectedHash cannot be null");
        String actual = computeSha256(stream);
        return actual.equalsIgnoreCase(expectedHash.trim());
    }
}
