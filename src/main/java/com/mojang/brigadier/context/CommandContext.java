package com.mojang.brigadier.context;

import java.util.Objects;

/**
 * Brigadier command execution context ({@code com.mojang.brigadier.context.CommandContext})
 * holding the command source and raw command input string.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <S> command source type
 */
public final class CommandContext<S> {

    private final S source;
    private final String input;

    /**
     * Constructs a new {@code CommandContext} for the given command source and input string.
     *
     * @param source command sender/source instance
     * @param input  raw command input string
     */
    public CommandContext(S source, String input) {
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.input = Objects.requireNonNull(input, "input must not be null");
    }

    /**
     * Returns the command source that initiated execution.
     *
     * @return command source
     */
    public S getSource() {
        return source;
    }

    /**
     * Returns the raw command string that was dispatched.
     *
     * @return raw command input
     */
    public String getInput() {
        return input;
    }
}
