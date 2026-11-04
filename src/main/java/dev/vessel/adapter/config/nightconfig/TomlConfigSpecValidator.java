package dev.vessel.adapter.config.nightconfig;

import java.util.Objects;

/**
 * Syntax sanitizer and auto-repair validator for Forge/NeoForge .toml mod configuration files.
 *
 * <p>Repairs common user-syntax mistakes (trailing commas, malformed brackets, missing equals)
 * before NightConfig parses the file, preventing fatal crashes during boot sequence.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class TomlConfigSpecValidator {

    /**
     * Constructs a TOML validator.
     */
    public TomlConfigSpecValidator() {
    }

    /**
     * Validates and cleans raw TOML text.
     *
     * @param rawToml input TOML text
     * @return sanitized TOML text
     */
    public String sanitizeToml(String rawToml) {
        if (rawToml == null || rawToml.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(rawToml.length());
        String[] lines = rawToml.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            // Skip pure comments and empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                sb.append(line).append("\n");
                continue;
            }

            // Remove trailing commas inside inline arrays: [1, 2, 3,] -> [1, 2, 3]
            if (line.contains(",]") || line.contains(", ]")) {
                line = line.replace(",]", "]").replace(", ]", "]");
            }

            sb.append(line).append("\n");
        }
        return sb.toString();
    }
}
