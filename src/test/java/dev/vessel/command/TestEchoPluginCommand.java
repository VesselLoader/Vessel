package dev.vessel.command;

import java.util.Arrays;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Test fixture command implementing {@link Command} to verify command map dispatching and logging.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TestEchoPluginCommand extends Command {

    private String lastExecutionLog = "";

    public TestEchoPluginCommand(String name) {
        super(name, "Echo command for tests", "/" + name, List.of("ec"));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        this.lastExecutionLog = "Executed " + commandLabel + " with " + Arrays.toString(args);
        sender.sendMessage(this.lastExecutionLog);
        return true;
    }

    public String getLastExecutionLog() {
        return lastExecutionLog;
    }
}
