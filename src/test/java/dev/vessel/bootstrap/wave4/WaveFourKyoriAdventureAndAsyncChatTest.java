package dev.vessel.bootstrap.wave4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.VesselWorld;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 4 verifying Kyori Adventure API surface
 * ({@link Key}, {@link TextColor}, {@link NamedTextColor}, {@link Component},
 * {@link TextComponent}), {@link Player#sendMessage(Component)}, {@link Player#displayName()},
 * and Paper {@link AsyncChatEvent}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFourKyoriAdventureAndAsyncChatTest {

    @Test
    void testKyoriAdventureKeyComponentPlayerAndAsyncChatEvent() {
        Key customKey = Key.key("vessel:super_engine");
        assertEquals("vessel", customKey.namespace());
        assertEquals("super_engine", customKey.value());
        assertEquals("vessel:super_engine", customKey.asString());

        Key defaultNsKey = Key.key("diamond_sword");
        assertEquals("minecraft", defaultNsKey.namespace());
        assertEquals("diamond_sword", defaultNsKey.value());
        assertEquals("minecraft:diamond_sword", defaultNsKey.asString());

        TextColor aquaHex = TextColor.fromHexString("#55FFFF");
        assertNotNull(aquaHex);
        assertEquals(0x55, aquaHex.red());
        assertEquals(0xFF, aquaHex.green());
        assertEquals(0xFF, aquaHex.blue());
        assertEquals("#55ffff", aquaHex.asHexString());

        Component greeting = Component.text("Hello ", NamedTextColor.GOLD)
                .append(Component.text("VESSEL", aquaHex));
        assertEquals("Hello VESSEL", greeting.toPlainText());
        assertEquals(NamedTextColor.GOLD, greeting.color());
        assertEquals(1, greeting.children().size());

        World world = new VesselWorld("world");
        Player alex = new Player("Alex", new Location(world, 0.0, 64.0, 0.0));
        alex.setDisplayName("[Admin] Alex");
        assertEquals("[Admin] Alex", alex.displayName().toPlainText());

        alex.sendMessage(greeting);
        assertEquals(List.of("Hello VESSEL"), alex.getReceivedMessages());

        AsyncChatEvent chatEvent = new AsyncChatEvent(alex, greeting);
        assertTrue(chatEvent.isAsynchronous());
        assertEquals(alex, chatEvent.getPlayer());
        assertEquals("Hello VESSEL", chatEvent.message().toPlainText());
        assertFalse(chatEvent.isCancelled());

        Component moderatedMessage = Component.text("[Verified] Hello VESSEL", NamedTextColor.GREEN);
        chatEvent.message(moderatedMessage);
        assertEquals("[Verified] Hello VESSEL", chatEvent.message().toPlainText());

        chatEvent.setCancelled(true);
        assertTrue(chatEvent.isCancelled());
    }
}
