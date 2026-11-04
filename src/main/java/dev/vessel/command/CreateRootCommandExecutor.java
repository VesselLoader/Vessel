package dev.vessel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

/**
 * Fallback Brigadier command executor for bare {@code /create} and {@code /c} command invocations,
 * displaying version info and available subcommands rather than syntax errors.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CreateRootCommandExecutor implements Command<Object> {

    private final BrigadierFeedbackSender feedbackSender = new BrigadierFeedbackSender();

    public CreateRootCommandExecutor() {
    }

    @Override
    public int run(CommandContext<Object> context) {
        feedbackSender.sendFeedback(context, "[Create] v6.0.8 - Usage: /create <debuginfo|fixLighting|trains|highlight|passenger|coupling|clone|train|glue|util>");
        feedbackSender.sendFeedback(context, "  -> /create fixLighting : Toggle experimental rendering pipeline");
        feedbackSender.sendFeedback(context, "  -> /create debuginfo : Send client/server debug state");
        feedbackSender.sendFeedback(context, "  -> /create trains : Inspect railways, trains and schedules");
        feedbackSender.sendFeedback(context, "  -> /create util : Camera distance, buffer clear and utility actions");
        return 1;
    }
}
