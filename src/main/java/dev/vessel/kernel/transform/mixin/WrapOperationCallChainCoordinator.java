package dev.vessel.kernel.transform.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Coordinates and sequences nested @WrapOperation (MixinExtras) call chains across conflicting mods.
 * <p>
 * Prevents destructive sponge @Redirect clashes by arbitrating multiple interceptors into a structured,
 * chained pipeline where each mod delegates safely to the next via Operation call semantics.
 * </p>
 * <p>
 * Directly resolves ERR-58, ERR-59, and MAP-MIX-0201..0400.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class WrapOperationCallChainCoordinator {

    private final Map<WrapOperationTarget, List<WrapOperationHandler>> registeredTargets = new HashMap<>();

    /**
     * Registers a new wrap handler for a target call site.
     *
     * @param target the target instruction call site
     * @param handler the handler method to include in the call chain
     */
    public synchronized void registerHandler(WrapOperationTarget target, WrapOperationHandler handler) {
        Objects.requireNonNull(target, "target cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        registeredTargets
                .computeIfAbsent(target, k -> new ArrayList<>())
                .add(handler);
    }

    /**
     * Constructs an ordered execution chain for the given call site.
     * Handlers are sorted by priority in ascending order (lower numerical value executes earlier).
     *
     * @param target the target instruction call site
     * @return populated WrapOperationChain
     */
    public synchronized WrapOperationChain buildChain(WrapOperationTarget target) {
        Objects.requireNonNull(target, "target cannot be null");

        List<WrapOperationHandler> list = registeredTargets.get(target);
        if (list == null || list.isEmpty()) {
            return new WrapOperationChain(target, List.of());
        }

        List<WrapOperationHandler> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt(WrapOperationHandler::priority));

        return new WrapOperationChain(target, sorted);
    }

    /**
     * Returns all registered targets that have more than one wrap handler attached.
     *
     * @return map of contested targets to their handler counts
     */
    public synchronized Map<WrapOperationTarget, Integer> getContestedTargets() {
        Map<WrapOperationTarget, Integer> contested = new HashMap<>();
        for (Map.Entry<WrapOperationTarget, List<WrapOperationHandler>> entry : registeredTargets.entrySet()) {
            if (entry.getValue().size() > 1) {
                contested.put(entry.getKey(), entry.getValue().size());
            }
        }
        return Collections.unmodifiableMap(contested);
    }

    /**
     * Returns the total number of registered wrap targets.
     *
     * @return count of target call sites
     */
    public synchronized int totalTargetCount() {
        return registeredTargets.size();
    }
}
