package dev.vessel.kernel.adapter.fabric;

import dev.vessel.kernel.event.VesselCancellable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 * High-performance, array-backed Directed Acyclic Graph (DAG) event dispatcher for Fabric
 * lifecycle events.
 *
 * <p>Resolves inter-listener dependencies using deterministic topological sorting and bakes
 * callbacks into a flat immutable array, guaranteeing zero memory allocation on critical dispatch
 * hot-paths.</p>
 *
 * <p>Quells: ERR-57, ECO-071..080.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @param <T> event payload type
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DagEventArrayBackedDispatcher<T> {

    @SuppressWarnings("unchecked")
    private static final Consumer<?>[] EMPTY_ARRAY = new Consumer<?>[0];

    private final Map<String, DagEventNode<T>> registeredNodes = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    private volatile Consumer<T>[] bakedListeners = (Consumer<T>[]) EMPTY_ARRAY;

    /**
     * Constructs a new empty DAG event dispatcher.
     */
    public DagEventArrayBackedDispatcher() {
    }

    /**
     * Registers a listener node with explicit ordering constraints.
     *
     * @param id unique identifier of the listener
     * @param listener the event listener callback
     * @param before identifiers of listeners that must execute AFTER this listener
     * @param after identifiers of listeners that must execute BEFORE this listener
     */
    public synchronized void register(String id, Consumer<T> listener, Set<String> before, Set<String> after) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        DagEventNode<T> node = new DagEventNode<>(id, listener, before, after);
        registeredNodes.put(id, node);
        bake();
    }

    /**
     * Registers a listener node with no dependencies.
     *
     * @param id unique identifier of the listener
     * @param listener the event listener callback
     */
    public void register(String id, Consumer<T> listener) {
        register(id, listener, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Unregisters a listener node by ID and rebuilds the baked execution array.
     *
     * @param id the listener identifier to remove
     * @return true if removed, false otherwise
     */
    public synchronized boolean unregister(String id) {
        Objects.requireNonNull(id, "id cannot be null");
        DagEventNode<T> removed = registeredNodes.remove(id);
        if (removed != null) {
            bake();
            return true;
        }
        return false;
    }

    /**
     * Dispatches an event across all baked listeners in topologically sorted order.
     * Zero-allocation hot path.
     *
     * @param event the event payload
     */
    public void dispatch(T event) {
        dispatch(event, true);
    }

    /**
     * Dispatches an event across all baked listeners, optionally honoring cancellation.
     *
     * @param event the event payload
     * @param checkCancellation whether to halt dispatch when event is cancelled
     */
    public void dispatch(T event, boolean checkCancellation) {
        Consumer<T>[] listeners = this.bakedListeners;
        boolean cancellable = event instanceof VesselCancellable;

        for (int i = 0; i < listeners.length; i++) {
            if (checkCancellation && cancellable && ((VesselCancellable) event).isCancelled()) {
                break;
            }
            listeners[i].accept(event);
        }
    }

    /**
     * Topologically sorts the registered DAG nodes and bakes them into a contiguous array.
     *
     * @throws DagCycleDetectedException if a cyclic dependency is detected in the DAG
     */
    @SuppressWarnings("unchecked")
    public synchronized void bake() {
        if (registeredNodes.isEmpty()) {
            this.bakedListeners = (Consumer<T>[]) EMPTY_ARRAY;
            return;
        }

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (String id : registeredNodes.keySet()) {
            inDegree.put(id, 0);
            adjacency.put(id, new ArrayList<>());
        }

        for (DagEventNode<T> node : registeredNodes.values()) {
            String u = node.getId();

            // Edge u -> v for each v in node.getBefore()
            for (String v : node.getBefore()) {
                if (registeredNodes.containsKey(v)) {
                    adjacency.get(u).add(v);
                    inDegree.put(v, inDegree.get(v) + 1);
                }
            }

            // Edge w -> u for each w in node.getAfter()
            for (String w : node.getAfter()) {
                if (registeredNodes.containsKey(w)) {
                    adjacency.get(w).add(u);
                    inDegree.put(u, inDegree.get(u) + 1);
                }
            }
        }

        Queue<String> queue = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<Consumer<T>> sortedListeners = new ArrayList<>(registeredNodes.size());
        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            String curr = queue.poll();
            visited.add(curr);
            DagEventNode<T> node = registeredNodes.get(curr);
            if (node != null) {
                sortedListeners.add(node.getListener());
            }

            List<String> neighbors = adjacency.get(curr);
            if (neighbors != null) {
                for (int i = 0; i < neighbors.size(); i++) {
                    String neighbor = neighbors.get(i);
                    int deg = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, deg);
                    if (deg == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
        }

        if (visited.size() < registeredNodes.size()) {
            throw new DagCycleDetectedException(
                    "Cyclic dependency detected in DAG event listeners. Processed "
                            + visited.size() + " of " + registeredNodes.size() + " nodes.");
        }

        Consumer<T>[] array = (Consumer<T>[]) new Consumer<?>[sortedListeners.size()];
        for (int i = 0; i < sortedListeners.size(); i++) {
            array[i] = sortedListeners.get(i);
        }
        this.bakedListeners = array;
    }

    /**
     * Returns the count of active baked listeners.
     *
     * @return listener count
     */
    public int getListenerCount() {
        return bakedListeners.length;
    }
}
