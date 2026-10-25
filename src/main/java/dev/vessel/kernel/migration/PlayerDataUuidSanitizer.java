package dev.vessel.kernel.migration;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Deterministic UUID sanitizer converting disparate or malformed player UUIDs into
 * canonical lowercase dash-separated strings to prevent case-sensitivity file collisions.
 * <p>
 * Mitigates: <b>ERR-95</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class PlayerDataUuidSanitizer {

    /** Standard regex pattern matching canonical 36-character hyphenated UUID. */
    public static final String CANONICAL_UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    /** Undashed 32-character hexadecimal UUID pattern. */
    public static final String UNDASHED_UUID_REGEX = "^[0-9a-fA-F]{32}$";

    private static final Pattern CANONICAL_PATTERN = Pattern.compile(CANONICAL_UUID_REGEX);
    private static final Pattern UNDASHED_PATTERN = Pattern.compile(UNDASHED_UUID_REGEX);

    /**
     * Sanitizes an arbitrary UUID string representation into canonical lowercase format.
     *
     * @param rawUuid raw input string
     * @return canonical lowercase 36-char UUID string
     * @throws PlayerDataPreservationException if the string cannot be parsed as a valid UUID
     */
    public String sanitizeUuid(String rawUuid) {
        if (rawUuid == null) {
            throw new PlayerDataPreservationException("Input UUID cannot be null");
        }
        String trimmed = rawUuid.trim();
        if (trimmed.isEmpty()) {
            throw new PlayerDataPreservationException("Input UUID cannot be empty");
        }

        if (CANONICAL_PATTERN.matcher(trimmed).matches()) {
            return trimmed.toLowerCase(Locale.ROOT);
        }

        if (UNDASHED_PATTERN.matcher(trimmed).matches()) {
            String lower = trimmed.toLowerCase(Locale.ROOT);
            return lower.substring(0, 8) + "-"
                    + lower.substring(8, 12) + "-"
                    + lower.substring(12, 16) + "-"
                    + lower.substring(16, 20) + "-"
                    + lower.substring(20, 32);
        }

        try {
            UUID parsed = UUID.fromString(trimmed);
            return parsed.toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            throw new PlayerDataPreservationException("Invalid UUID format: " + trimmed, ex);
        }
    }

    /**
     * Sanitizes a standard Java {@link UUID} instance.
     *
     * @param uuid uuid instance
     * @return canonical lowercase string
     */
    public String sanitizeUuid(UUID uuid) {
        Objects.requireNonNull(uuid, "UUID object cannot be null");
        return uuid.toString().toLowerCase(Locale.ROOT);
    }
}
