package dev.vessel.kernel.adapter.fabric;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Immutable node definition within a Directed Acyclic Graph (DAG) event execution pipeline.
 *
 * @param <T> event payload type
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DagEventNode<T> {

    private final String id;
    private final Consumer<T> listener;
    private final Set<String> before;
    private final Set<String> after;

    /**
     * Constructs a new DagEventNode.
     *
     * @param id unique identifier of the node
     * @param listener listener callback
     * @param before identifiers that this node must execute before
     * @param after identifiers that this node must execute after
     */
    public DagEventNode(String id, Consumer<T> listener, Set<String> before, Set<String> after) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.listener = Objects.requireNonNull(listener, "listener cannot be null");
        this.before = before != null ? Collections.unmodifiableSet(before) : Collections.emptySet();
        this.after = after != null ? Collections.unmodifiableSet(after) : Collections.emptySet();
    }

    /**
     * Retrieves node identifier.
     *
     * @return node ID
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves event listener callback.
     *
     * @return listener consumer
     */
    public Consumer<T> getListener() {
        return listener;
    }

    /**
     * Retrieves the set of node identifiers that must run after this node.
     *
     * @return set of successor IDs
     */
    public Set<String> getBefore() {
        return before;
    }

    /**
     * Retrieves the set of node identifiers that must run prior to this node.
     *
     * @return set of predecessor IDs
     */
    public Set<String> getAfter() {
        return after;
    }
}
