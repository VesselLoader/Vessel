package dev.vessel.kernel.network.bridge;

import java.util.Objects;

/**
 * Translates Adventure MiniMessage chat components into Bedrock Edition legacy section-sign
 * formatting codes ({@code §}) so Bedrock RakNet clients render server and plugin messages with
 * accurate colors and styles.
 *
 * @version 1.0.0
 */
public final class AdventureToBedrockChatHarmonizer {

    /**
     * Converts Adventure MiniMessage tags (e.g., {@code <green>}, {@code <bold>}, {@code <red>})
     * into Bedrock legacy formatting codes (e.g., {@code §a}, {@code §l}, {@code §c}).
     *
     * @param miniMessageText the MiniMessage formatted string
     * @return the Bedrock-compatible legacy formatted string
     */
    public String convertMiniMessageToBedrockLegacy(String miniMessageText) {
        Objects.requireNonNull(miniMessageText, "miniMessageText must not be null");
        return miniMessageText
                .replace("<black>", "§0")
                .replace("<dark_blue>", "§1")
                .replace("<dark_green>", "§2")
                .replace("<dark_aqua>", "§3")
                .replace("<dark_red>", "§4")
                .replace("<dark_purple>", "§5")
                .replace("<gold>", "§6")
                .replace("<gray>", "§7")
                .replace("<dark_gray>", "§8")
                .replace("<blue>", "§9")
                .replace("<green>", "§a")
                .replace("<aqua>", "§b")
                .replace("<red>", "§c")
                .replace("<light_purple>", "§d")
                .replace("<yellow>", "§e")
                .replace("<white>", "§f")
                .replace("<bold>", "§l")
                .replace("<italic>", "§o")
                .replace("<underlined>", "§n")
                .replace("<strikethrough>", "§m")
                .replace("<obfuscated>", "§k")
                .replace("<reset>", "§r");
    }
}
