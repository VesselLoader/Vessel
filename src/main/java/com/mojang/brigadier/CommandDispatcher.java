package com.mojang.brigadier;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Root Brigadier command dispatcher ({@code com.mojang.brigadier.CommandDispatcher}) that stores
 * registered {@link LiteralCommandNode} trees and dispatches command strings against a command
 * source.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <S> command source type
 */
public final class CommandDispatcher<S> {

    private final Map<String, LiteralCommandNode<S>> rootNodes = new ConcurrentHashMap<>();

    /**
     * Constructs an empty {@code CommandDispatcher}.
     */
    public CommandDispatcher() {
    }

    /**
     * Builds and registers a root command literal on this dispatcher.
     *
     * @param command root literal builder
     * @return registered {@link LiteralCommandNode}
     */
    public LiteralCommandNode<S> register(LiteralArgumentBuilder<S> command) {
        Objects.requireNonNull(command, "command must not be null");
        LiteralCommandNode<S> node = command.build();
        rootNodes.put(node.getLiteral(), node);
        return node;
    }

    /**
     * Parses and executes the given command string for {@code source}.
     *
     * @param input  raw command input (with or without a leading {@code '/'})
     * @param source command sender/source instance
     * @return integer execution result, or {@code 0} if no command matched or permission was denied
     */
    public int execute(String input, S source) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(source, "source must not be null");
        String normalized = input.trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isEmpty()) {
            return 0;
        }
        String[] tokens = normalized.split("\\s+");
        LiteralCommandNode<S> node = rootNodes.get(tokens[0]);
        if (node == null || !node.canUse(source)) {
            return 0;
        }
        for (int i = 1; i < tokens.length; i++) {
            LiteralCommandNode<S> child = node.getChild(tokens[i]);
            if (child == null) {
                break;
            }
            if (!child.canUse(source)) {
                return 0;
            }
            node = child;
        }
        if (node.getCommand() != null) {
            return node.getCommand().run(new CommandContext<>(source, input));
        }
        return 0;
    }

    /**
     * Returns an unmodifiable view of all root command nodes registered in this dispatcher.
     *
     * @return unmodifiable map of root literals to command nodes
     */
    public Map<String, LiteralCommandNode<S>> getRootNodes() {
        return Collections.unmodifiableMap(rootNodes);
    }
}
