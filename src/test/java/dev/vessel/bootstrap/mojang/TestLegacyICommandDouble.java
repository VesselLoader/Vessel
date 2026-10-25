package dev.vessel.bootstrap.mojang;

/**
 * Top-level test double representing a legacy {@code ICommand} instance with a name for verifying
 * {@code compareTo} sorting without inner classes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class TestLegacyICommandDouble {

    private final String commandName;

    public TestLegacyICommandDouble(String commandName) {
        this.commandName = commandName;
    }

    public String getName() {
        return commandName;
    }

    public String getCommandName() {
        return commandName;
    }
}
