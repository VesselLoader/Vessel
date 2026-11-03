package com.mojang.brigadier.tree;

import com.mojang.brigadier.Command;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Immutable Brigadier command tree node representing a fixed literal token
 * ({@code com.mojang.brigadier.tree.LiteralCommandNode}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @param <S> command source type
 */
public final class LiteralCommandNode<S> {

    private final String literal;
    private final Command<S> command;
    private final Predicate<S> requirement;
    private final Map<String, LiteralCommandNode<S>> children = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code LiteralCommandNode}.
     *
     * @param literal     command literal token
     * @param command     optional command execution callback (may be {@code null})
     * @param requirement optional source permission predicate (may be {@code null})
     * @param children    child literal command nodes
     */
    public LiteralCommandNode(
            String literal,
            Command<S> command,
            Predicate<S> requirement,
            Map<String, LiteralCommandNode<S>> children) {
        this.literal = Objects.requireNonNull(literal, "literal must not be null");
        this.command = command;
        this.requirement = requirement;
        if (children != null) {
            this.children.putAll(children);
        }
    }

    /**
     * Returns the literal token matched by this command node.
     *
     * @return literal token
     */
    public String getLiteral() {
        return literal;
    }

    /**
     * Returns the {@link Command} callback bound to this node, or {@code null} if this node is an
     * intermediate branch.
     *
     * @return bound command or {@code null}
     */
    public Command<S> getCommand() {
        return command;
    }

    /**
     * Returns the requirement predicate for this node, or {@code null} if unrestricted.
     *
     * @return requirement predicate or {@code null}
     */
    public Predicate<S> getRequirement() {
        return requirement;
    }

    /**
     * Checks whether the given command source satisfies this node's requirement predicate.
     *
     * @param source command source to test
     * @return {@code true} if allowed to execute or traverse this node
     */
    public boolean canUse(S source) {
        return requirement == null || requirement.test(source);
    }

    /**
     * Looks up a direct child literal node by its literal token.
     *
     * @param name child literal name
     * @return matching child node, or {@code null} if none exists
     */
    public LiteralCommandNode<S> getChild(String name) {
        if (name == null) {
            return null;
        }
        return children.get(name);
    }

    /**
     * Returns an unmodifiable view of all child nodes attached to this literal node.
     *
     * @return unmodifiable child map
     */
    public Map<String, LiteralCommandNode<S>> getChildren() {
        return Collections.unmodifiableMap(children);
    }
}
