package dev.vessel.bootstrap.mojang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top-level test double representing a legacy Minecraft {@code ICommandSender} for verifying
 * multi-line chat message delivery without inner classes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class TestLegacyCommandSender {

    private final List<String> receivedMessages = new ArrayList<>();

    public TestLegacyCommandSender() {
    }

    public void sendMessage(String message) {
        if (message != null) {
            receivedMessages.add(message);
        }
    }

    public List<String> getReceivedMessages() {
        return Collections.unmodifiableList(receivedMessages);
    }
}
