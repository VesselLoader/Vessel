package dev.vessel.kernel.resourcepack.bedrock;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

/**
 * Deterministic generator for Bedrock Edition resource pack {@code manifest.json} files.
 * <p>
 * Generates RFC-4122 compliant UUID v5 identifiers derived deterministically from pack/module
 * identifiers, ensuring stable pack identities across server restarts without state storage.
 * Declares {@code format_version: 2} and standard {@code resources} modules.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class BedrockManifestGenerator {

    /** Default namespace UUID for VESSEL Bedrock resource packs (RFC 4122 v5). */
    public static final UUID VESSEL_BEDROCK_NAMESPACE = UUID.fromString("e9b867c4-5412-4f81-a957-6e9a65f4bb2a");

    /** Default minimum engine version [1, 20, 0] for modern Bedrock clients. */
    public static final int[] DEFAULT_MIN_ENGINE_VERSION = new int[]{1, 20, 0};

    /** Default pack version [1, 0, 0]. */
    public static final int[] DEFAULT_PACK_VERSION = new int[]{1, 0, 0};

    /**
     * Generates an RFC-4122 Version 5 (name-based SHA-1) deterministic UUID.
     *
     * @param namespace namespace UUID
     * @param name input identity string
     * @return deterministic UUID v5
     */
    public static UUID generateDeterministicUuid(UUID namespace, String name) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        Objects.requireNonNull(name, "name cannot be null");

        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");

            // Write 16-byte network order representation of namespace UUID
            long msb = namespace.getMostSignificantBits();
            long lsb = namespace.getLeastSignificantBits();
            byte[] nsBytes = new byte[16];
            for (int i = 0; i < 8; i++) {
                nsBytes[i] = (byte) ((msb >>> (56 - i * 8)) & 0xFF);
                nsBytes[8 + i] = (byte) ((lsb >>> (56 - i * 8)) & 0xFF);
            }
            sha1.update(nsBytes);
            sha1.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] digest = sha1.digest();

            // Set Version 5: clear top 4 bits, set to 0101 (0x50)
            digest[6] = (byte) ((digest[6] & 0x0F) | 0x50);
            // Set RFC 4122 Variant: clear top 2 bits, set to 10 (0x80)
            digest[8] = (byte) ((digest[8] & 0x3F) | 0x80);

            long resMsb = 0;
            for (int i = 0; i < 8; i++) {
                resMsb = (resMsb << 8) | (digest[i] & 0xFF);
            }
            long resLsb = 0;
            for (int i = 8; i < 16; i++) {
                resLsb = (resLsb << 8) | (digest[i] & 0xFF);
            }

            return new UUID(resMsb, resLsb);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 digest algorithm unavailable on host JVM", e);
        }
    }

    /**
     * Parses a SemVer string (e.g. "1.2.3" or "2.0") into a 3-element integer array.
     *
     * @param versionStr version string
     * @return int array [major, minor, patch]
     */
    public static int[] parseVersionArray(String versionStr) {
        if (versionStr == null || versionStr.isBlank()) {
            return DEFAULT_PACK_VERSION.clone();
        }
        String cleaned = versionStr.replaceAll("[^0-9.]", "").trim();
        String[] tokens = cleaned.split("\\.");
        int[] result = new int[]{1, 0, 0};
        for (int i = 0; i < Math.min(tokens.length, 3); i++) {
            try {
                result[i] = Math.max(0, Integer.parseInt(tokens[i]));
            } catch (NumberFormatException ignored) {
                // Keep default
            }
        }
        return result;
    }

    /**
     * Generates a complete Bedrock {@code manifest.json} with default versions.
     *
     * @param packId unique pack identifier
     * @param name human-readable pack title
     * @param description pack description
     * @return formatted JSON manifest
     */
    public String generateManifest(String packId, String name, String description) {
        return generateManifest(packId, name, description, DEFAULT_PACK_VERSION, DEFAULT_MIN_ENGINE_VERSION);
    }

    /**
     * Generates a complete Bedrock {@code manifest.json} with specified version arrays.
     *
     * @param packId unique pack identifier
     * @param name human-readable pack title
     * @param description pack description
     * @param packVersion 3-element array [major, minor, patch]
     * @param minEngineVersion 3-element minimum engine version array [major, minor, patch]
     * @return formatted JSON manifest
     */
    public String generateManifest(
            String packId,
            String name,
            String description,
            int[] packVersion,
            int[] minEngineVersion
    ) {
        Objects.requireNonNull(packId, "packId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(packVersion, "packVersion cannot be null");
        Objects.requireNonNull(minEngineVersion, "minEngineVersion cannot be null");

        if (packVersion.length != 3) {
            throw new IllegalArgumentException("packVersion must contain exactly 3 elements [major, minor, patch]");
        }
        if (minEngineVersion.length != 3) {
            throw new IllegalArgumentException("minEngineVersion must contain exactly 3 elements [major, minor, patch]");
        }

        UUID headerUuid = generateDeterministicUuid(VESSEL_BEDROCK_NAMESPACE, packId + ":header");
        UUID moduleUuid = generateDeterministicUuid(VESSEL_BEDROCK_NAMESPACE, packId + ":resources");

        String escapedName = escapeJson(name);
        String escapedDesc = escapeJson(description);

        return "{\n"
                + "  \"format_version\": 2,\n"
                + "  \"header\": {\n"
                + "    \"name\": \"" + escapedName + "\",\n"
                + "    \"description\": \"" + escapedDesc + "\",\n"
                + "    \"uuid\": \"" + headerUuid + "\",\n"
                + "    \"version\": [" + packVersion[0] + ", " + packVersion[1] + ", " + packVersion[2] + "],\n"
                + "    \"min_engine_version\": [" + minEngineVersion[0] + ", " + minEngineVersion[1] + ", " + minEngineVersion[2] + "]\n"
                + "  },\n"
                + "  \"modules\": [\n"
                + "    {\n"
                + "      \"type\": \"resources\",\n"
                + "      \"uuid\": \"" + moduleUuid + "\",\n"
                + "      \"version\": [" + packVersion[0] + ", " + packVersion[1] + ", " + packVersion[2] + "],\n"
                + "      \"description\": \"" + escapedDesc + "\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
    }

    private static String escapeJson(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
