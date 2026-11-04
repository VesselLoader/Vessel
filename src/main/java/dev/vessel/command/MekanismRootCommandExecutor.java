package dev.vessel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

/**
 * Fallback Brigadier command executor for bare {@code /mek} command invocations,
 * displaying version info and available subcommands rather than syntax errors.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MekanismRootCommandExecutor implements Command<Object> {

    private final BrigadierFeedbackSender feedbackSender = new BrigadierFeedbackSender();

    public MekanismRootCommandExecutor() {
    }

    @Override
    public int run(CommandContext<Object> context) {
        feedbackSender.sendFeedback(context, "[Mekanism] v10.4.16 - Usage: /mek <chunk|debug|retrogen|radiation|testrules>");
        feedbackSender.sendFeedback(context, "  -> /mek chunk : Manage chunk watcher positions");
        feedbackSender.sendFeedback(context, "  -> /mek debug : Toggle developer debug diagnostics mode");
        feedbackSender.sendFeedback(context, "  -> /mek radiation : Radiation management and queries");
        feedbackSender.sendFeedback(context, "  -> /mek retrogen : Trigger world retrogen processing");
        return 1;
    }
}
