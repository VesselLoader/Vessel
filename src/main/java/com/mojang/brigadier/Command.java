package com.mojang.brigadier;

import com.mojang.brigadier.context.CommandContext;

/**
 * Functional Brigadier command action ({@code com.mojang.brigadier.Command}) invoked when a
 * matched command node is executed.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <S> command source type
 */
@FunctionalInterface
public interface Command<S> {

    /**
     * Standard return code indicating a single successful command execution.
     */
    int SINGLE_SUCCESS = 1;

    /**
     * Executes this command for the given {@link CommandContext}.
     *
     * @param context execution context holding the command source and raw input
     * @return integer result code
     */
    int run(CommandContext<S> context);
}
