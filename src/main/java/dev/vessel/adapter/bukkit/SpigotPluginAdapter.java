package dev.vessel.adapter.bukkit;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges Spigot-specific APIs including BungeeCord ChatComponent legacy formatting codes
 * ({@code §} / {@code &}) to Adventure MiniMessage tags and dispatches ActionBar messages.
 *
 * @version 1.0.0
 */
public final class SpigotPluginAdapter {

    private final Map<UUID, String> lastActionBarMessages = new ConcurrentHashMap<>();

    /**
     * Converts legacy BungeeCord / Spigot color codes (e.g., {@code §a}, {@code &c}, {@code §l})
     * into Adventure MiniMessage syntax (e.g., {@code <green>}, {@code <red>}, {@code <bold>}).
     *
     * @param legacyChatText the legacy formatted chat string
     * @return the MiniMessage formatted string
     */
    public String convertBungeeComponentToMiniMessage(String legacyChatText) {
        Objects.requireNonNull(legacyChatText, "legacyChatText must not be null");
        StringBuilder out = new StringBuilder(legacyChatText.length() + 16);
        int len = legacyChatText.length();
        for (int i = 0; i < len; i++) {
            char c = legacyChatText.charAt(i);
            if ((c == '§' || c == '&') && (i + 1) < len) {
                char code = Character.toLowerCase(legacyChatText.charAt(i + 1));
                String tag = mapLegacyCodeToMiniMessageTag(code);
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * Sends an ActionBar message to an online player via the Spigot compatibility shim.
     *
     * @param playerUuid the target player's UUID
     * @param message    the ActionBar text (automatically converted to MiniMessage)
     */
    public void sendActionBarViaSpigotShim(UUID playerUuid, String message) {
        Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        Objects.requireNonNull(message, "message must not be null");
        lastActionBarMessages.put(playerUuid, convertBungeeComponentToMiniMessage(message));
    }

    /**
     * Returns the last MiniMessage-converted ActionBar payload sent to a player.
     *
     * @param playerUuid the player's UUID
     * @return the last ActionBar message, or {@code null} if none
     */
    public String getLastActionBarMessage(UUID playerUuid) {
        return lastActionBarMessages.get(playerUuid);
    }

    private static String mapLegacyCodeToMiniMessageTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underlined>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }
}
