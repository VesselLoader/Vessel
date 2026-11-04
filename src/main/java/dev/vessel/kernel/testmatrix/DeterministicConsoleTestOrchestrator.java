package dev.vessel.kernel.testmatrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic test orchestrator defining the canonical 10-command test suite
 * with explicit expected tokens and validation criteria.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class DeterministicConsoleTestOrchestrator {

    private final ConsoleCommandOutputAssertionEngine assertionEngine =
            new ConsoleCommandOutputAssertionEngine();

    public DeterministicConsoleTestOrchestrator() {
    }

    /**
     * Returns the list of standard test command strings in deterministic order.
     *
     * @return ordered list of commands
     */
    public List<String> getCanonicalCommandSequence() {
        return List.of(
                "vessel status",
                "vessel mods",
                "chunky help",
                "chunky start",
                "mek",
                "mek debug",
                "create",
                "create fixLighting",
                "help",
                "stop"
        );
    }

    /**
     * Returns the expected token corresponding to the given command string.
     *
     * @param command input command string
     * @return expected matching substring
     */
    public String getExpectedTokenForCommand(String command) {
        if (command == null) {
            return "";
        }
        String clean = command.trim();
        if ("vessel status".equals(clean)) {
            return "Engine Status : ONLINE";
        }
        if ("vessel mods".equals(clean)) {
            return "Loaded Mods";
        }
        if ("chunky help".equals(clean)) {
            return "Chunky Commands";
        }
        if ("chunky start".equals(clean)) {
            return "Task started in world";
        }
        if ("mek".equals(clean)) {
            return "[Mekanism]";
        }
        if ("mek debug".equals(clean)) {
            return "command.mekanism.debug";
        }
        if ("create".equals(clean)) {
            return "[Create]";
        }
        if ("create fixLighting".equals(clean)) {
            return "block rendering pipeline is now enabled";
        }
        if ("help".equals(clean)) {
            return "/";
        }
        if ("stop".equals(clean)) {
            return "Stopping server";
        }
        return "";
    }

    /**
     * Validates whether a command's actual output matches expectations.
     *
     * @param command command that was executed
     * @param outputLines lines received in response
     * @return {@code true} if validation passed
     */
    public boolean validateCommandOutput(String command, List<String> outputLines) {
        String expected = getExpectedTokenForCommand(command);
        if (expected.isEmpty()) {
            return true;
        }
        return assertionEngine.assertContains(outputLines, expected);
    }
}
