package dev.vessel.adapter.bridge;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prevents infinite cyclical P2P tunnel traversal loops in Applied Energistics 2 (AE2)
 * and similar cross-dimensional item/energy routing networks within a single server tick.
 *
 * @version 1.0.0
 */
public final class P2PLoopRecursionGuard {

    private final Set<String> traversedEdgesThisTick = ConcurrentHashMap.newKeySet();
    private final Set<Long> visitedNodesThisTick = ConcurrentHashMap.newKeySet();

    /**
     * Creates a new P2P tunnel loop recursion guard.
     */
    public P2PLoopRecursionGuard() {
    }

    /**
     * Attempts to traverse from {@code sourceTunnelId} to {@code targetTunnelId}, returning
     * {@code false} if this traversal forms a cycle within the current tick.
     *
     * @param sourceTunnelId 64-bit source tunnel identifier
     * @param targetTunnelId 64-bit target tunnel identifier
     * @return {@code true} if the traversal is acyclic and permitted, {@code false} if a loop is detected
     */
    public boolean tryTraverseTunnel(long sourceTunnelId, long targetTunnelId) {
        if (sourceTunnelId == targetTunnelId) {
            return false;
        }
        String edgeKey = sourceTunnelId + "->" + targetTunnelId;
        String reverseEdgeKey = targetTunnelId + "->" + sourceTunnelId;
        if (traversedEdgesThisTick.contains(edgeKey)
                || traversedEdgesThisTick.contains(reverseEdgeKey)
                || visitedNodesThisTick.contains(targetTunnelId)) {
            return false;
        }
        visitedNodesThisTick.add(sourceTunnelId);
        traversedEdgesThisTick.add(edgeKey);
        return true;
    }

    /**
     * Resets the visited tunnel graph at the start of a new server tick.
     */
    public void resetTickTraversalCycle() {
        traversedEdgesThisTick.clear();
        visitedNodesThisTick.clear();
    }
}
