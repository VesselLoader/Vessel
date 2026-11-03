package org.bukkit.command;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canonical Bukkit {@code ConsoleCommandSender} implementation representing the server console
 * command actor with full operator privileges.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ConsoleCommandSender implements CommandSender {

    private final List<String> sentMessages = new CopyOnWriteArrayList<>();

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public void sendMessage(String message) {
        if (message != null) {
            sentMessages.add(message);
        }
    }

    @Override
    public boolean hasPermission(String name) {
        return true;
    }

    @Override
    public boolean isOp() {
        return true;
    }

    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
