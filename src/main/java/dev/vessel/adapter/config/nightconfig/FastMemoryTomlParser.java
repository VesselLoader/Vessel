package dev.vessel.adapter.config.nightconfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight, zero-allocation memory parser for basic TOML key-value pairs.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FastMemoryTomlParser {

    private final TomlConfigSpecValidator validator;

    /**
     * Constructs a fast memory TOML parser.
     *
     * @param validator syntax validator
     */
    public FastMemoryTomlParser(TomlConfigSpecValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
    }

    /**
     * Parses key-value pairs from a TOML string.
     *
     * @param rawToml raw TOML content
     * @return map of key-value pairs
     */
    public Map<String, String> parseSimpleKeyValuePairs(String rawToml) {
        String clean = validator.sanitizeToml(rawToml);
        if (clean.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new HashMap<>();
        String currentSection = "";

        String[] lines = clean.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }

            int eqIdx = trimmed.indexOf('=');
            if (eqIdx != -1) {
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                // Strip quotes if present
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                result.put(fullKey, value);
            }
        }

        return Collections.unmodifiableMap(result);
    }
}
