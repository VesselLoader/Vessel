package dev.vessel.kernel.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Computes and validates SHA-1 checksums of client presentation packs and individual asset assets.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class PresentationResourceChecksumVerifier {

    public String computeFileSha1Hex(Path filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        if (!Files.isRegularFile(filePath)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest unavailable in JVM", e);
        }
    }

    public boolean verifyChecksum(Path filePath, String expectedSha1) throws IOException {
        if (filePath == null || expectedSha1 == null || expectedSha1.isBlank()) {
            return false;
        }
        String actual = computeFileSha1Hex(filePath);
        return actual.equalsIgnoreCase(expectedSha1.trim());
    }
}
