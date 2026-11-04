package dev.vessel.kernel.logging;

import java.util.regex.Pattern;

/**
 * Sanitizes raw console log text by stripping legacy Minecraft formatting codes
 * ({@code §[0-9a-fk-orA-FK-OR]}) and residual terminal ANSI escape sequences.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselConsoleLogColorSanitizer {

    private static final Pattern MINECRAFT_SECTION_PATTERN =
            Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    private static final Pattern ANSI_ESCAPE_PATTERN =
            Pattern.compile("\u001B\\[[;?0-9]*[a-zA-Z]");

    public VesselConsoleLogColorSanitizer() {
    }

    /**
     * Sanitizes the given input text by stripping Minecraft and ANSI formatting.
     *
     * @param input raw input string
     * @return clean plain-text string, or empty string if input is null
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String strippedMinecraft = MINECRAFT_SECTION_PATTERN.matcher(input).replaceAll("");
        return ANSI_ESCAPE_PATTERN.matcher(strippedMinecraft).replaceAll("");
    }
}
