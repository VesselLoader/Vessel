package com.mojang.brigadier.builder;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Fluent builder for constructing Brigadier {@link LiteralCommandNode} command trees
 * ({@code com.mojang.brigadier.builder.LiteralArgumentBuilder}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <S> command source type
 */
public final class LiteralArgumentBuilder<S> {

    private final String literal;
    private Command<S> command;
    private Predicate<S> requirement;
    private final Map<String, LiteralCommandNode<S>> children = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code LiteralArgumentBuilder} for the specified literal token.
     *
     * @param literal non-null literal token
     */
    public LiteralArgumentBuilder(String literal) {
        this.literal = Objects.requireNonNull(literal, "literal must not be null");
    }

    /**
     * Creates a new {@code LiteralArgumentBuilder} for the given literal name.
     *
     * @param name literal token name
     * @param <S>  command source type
     * @return new builder instance
     */
    public static <S> LiteralArgumentBuilder<S> literal(String name) {
        return new LiteralArgumentBuilder<>(name);
    }

    /**
     * Returns the literal token configured on this builder.
     *
     * @return literal token
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * Assigns a requirement predicate that must pass for a command source to use this node.
     *
     * @param requirement permission predicate
     * @return {@code this} builder
     */
    public LiteralArgumentBuilder<S> requires(Predicate<S> requirement) {
        this.requirement = requirement;
        return this;
    }

    /**
     * Binds the terminal {@link Command} action executed when this node is matched.
     *
     * @param command command action callback
     * @return {@code this} builder
     */
    public LiteralArgumentBuilder<S> executes(Command<S> command) {
        this.command = command;
        return this;
    }

    /**
     * Builds and attaches a child literal branch to this builder.
     *
     * @param argument child builder
     * @return {@code this} builder
     */
    public LiteralArgumentBuilder<S> then(LiteralArgumentBuilder<S> argument) {
        Objects.requireNonNull(argument, "argument must not be null");
        LiteralCommandNode<S> childNode = argument.build();
        children.put(childNode.getLiteral(), childNode);
        return this;
    }

    /**
     * Builds an immutable {@link LiteralCommandNode} from the current builder state.
     *
     * @return built {@link LiteralCommandNode}
     */
    public LiteralCommandNode<S> build() {
        return new LiteralCommandNode<>(literal, command, requirement, children);
    }
}
