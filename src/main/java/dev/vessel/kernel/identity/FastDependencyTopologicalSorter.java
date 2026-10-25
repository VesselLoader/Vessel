package dev.vessel.kernel.identity;

import dev.vessel.kernel.identity.dependency.DependencyConstraintKind;
import dev.vessel.kernel.identity.dependency.ModuleDependencyRule;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Non-recursive Tarjan-based topological dependency sorting engine.
 * Eliminates Metaspace leaks, stack overflows on deep dependency graphs,
 * and deterministically resolves soft/ordering cycles between ecosystem modules (ERR-19, ERR-20).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastDependencyTopologicalSorter {

    /**
     * Constructs a FastDependencyTopologicalSorter.
     */
    public FastDependencyTopologicalSorter() {
    }

    /**
     * Computes a linear startup order for modules satisfying all declared dependency constraints.
     * Automatically resolves soft/hint dependency cycles and throws {@link CircularDependencyException}
     * if an unresolvable hard cycle is detected.
     *
     * @param modules the set of all active modules
     * @param rules the set of declared dependency rules
     * @return sequentially ordered list of modules ready for bootstrap
     * @throws CircularDependencyException if an unresolvable circular dependency cycle is detected
     */
    public List<VesselModuleIdentifier> sort(
            Collection<VesselModuleIdentifier> modules,
            Collection<ModuleDependencyRule> rules
    ) {
        Objects.requireNonNull(modules, "modules cannot be null");
        Objects.requireNonNull(rules, "rules cannot be null");

        int n = modules.size();
        if (n == 0) {
            return List.of();
        }

        // Map namespace to module identifier and integer index
        List<VesselModuleIdentifier> moduleList = new ArrayList<>(modules);
        moduleList.sort(Comparator.comparing(VesselModuleIdentifier::namespace));

        Map<String, Integer> namespaceToIndex = new HashMap<>(n);
        for (int i = 0; i < n; i++) {
            namespaceToIndex.put(moduleList.get(i).namespace(), i);
        }

        // Build directed edges: from -> to
        // If A requires B (MANDATORY or LOAD_AFTER), B must come before A: Edge B -> A
        // If A LOAD_BEFORE B, A must come before B: Edge A -> B
        List<ModuleDependencyRule> activeRules = new ArrayList<>(rules);

        // Iteratively resolve cycles by eliminating soft constraints if necessary
        boolean graphChanged = true;
        while (graphChanged) {
            graphChanged = false;

            // Construct adjacency matrix / list
            List<List<Integer>> adj = new ArrayList<>(n);
            List<List<ModuleDependencyRule>> edgeRules = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                adj.add(new ArrayList<>());
                edgeRules.add(new ArrayList<>());
            }

            for (ModuleDependencyRule rule : activeRules) {
                Integer sourceIdx = namespaceToIndex.get(rule.sourceModule().namespace());
                Integer targetIdx = namespaceToIndex.get(rule.targetNamespace());

                if (sourceIdx == null || targetIdx == null) {
                    if (rule.kind() == DependencyConstraintKind.MANDATORY && targetIdx == null) {
                        throw new IllegalStateException("Missing mandatory dependency: " + rule.targetNamespace()
                                + " required by " + rule.sourceModule().namespace());
                    }
                    continue;
                }

                if (sourceIdx.equals(targetIdx)) {
                    continue; // Ignore self-dependency
                }

                if (rule.kind() == DependencyConstraintKind.MANDATORY || rule.kind() == DependencyConstraintKind.LOAD_AFTER) {
                    // target -> source
                    adj.get(targetIdx).add(sourceIdx);
                    edgeRules.get(targetIdx).add(rule);
                } else if (rule.kind() == DependencyConstraintKind.LOAD_BEFORE) {
                    // source -> target
                    adj.get(sourceIdx).add(targetIdx);
                    edgeRules.get(sourceIdx).add(rule);
                }
            }

            // Run non-recursive Tarjan SCC algorithm
            List<List<Integer>> sccs = findStronglyConnectedComponentsNonRecursive(n, adj);

            for (List<Integer> scc : sccs) {
                if (scc.size() > 1) {
                    // Cycle detected! Check if we can break a soft/hint edge
                    Set<Integer> sccNodes = new HashSet<>(scc);
                    ModuleDependencyRule softRuleToBreak = null;

                    for (Integer u : scc) {
                        List<ModuleDependencyRule> rulesFromU = edgeRules.get(u);
                        List<Integer> neighbors = adj.get(u);
                        for (int i = 0; i < neighbors.size(); i++) {
                            int v = neighbors.get(i);
                            if (sccNodes.contains(v)) {
                                ModuleDependencyRule candidate = rulesFromU.get(i);
                                if (candidate.kind() == DependencyConstraintKind.LOAD_BEFORE
                                        || candidate.kind() == DependencyConstraintKind.LOAD_AFTER
                                        || candidate.kind() == DependencyConstraintKind.OPTIONAL) {
                                    softRuleToBreak = candidate;
                                    break;
                                }
                            }
                        }
                        if (softRuleToBreak != null) {
                            break;
                        }
                    }

                    if (softRuleToBreak != null) {
                        activeRules.remove(softRuleToBreak);
                        graphChanged = true;
                        break; // re-run graph construction
                    } else {
                        // All edges in this cycle are MANDATORY. Unresolvable cycle!
                        List<String> cycleNames = new ArrayList<>(scc.size());
                        for (Integer idx : scc) {
                            cycleNames.add(moduleList.get(idx).namespace());
                        }
                        throw new CircularDependencyException(
                                "Circular dependency detected among mandatory modules: " + cycleNames,
                                cycleNames
                        );
                    }
                }
            }
        }

        // At this point, the graph is a verified DAG. Compute deterministic topological sort.
        return computeTopologicalOrder(n, moduleList, namespaceToIndex, activeRules);
    }

    private List<List<Integer>> findStronglyConnectedComponentsNonRecursive(int n, List<List<Integer>> adj) {
        int[] dfn = new int[n];
        int[] low = new int[n];
        boolean[] inStack = new boolean[n];
        int[] sccStack = new int[n];
        int sccStackPtr = 0;
        int time = 0;

        List<List<Integer>> sccs = new ArrayList<>();

        // Execution stack for non-recursive traversal: [node, edgeIndex]
        int[] callStackNode = new int[n];
        int[] callStackEdge = new int[n];

        for (int i = 0; i < n; i++) {
            if (dfn[i] != 0) {
                continue;
            }

            int callStackPtr = 0;
            callStackNode[0] = i;
            callStackEdge[0] = 0;
            callStackPtr++;

            time++;
            dfn[i] = time;
            low[i] = time;
            sccStack[sccStackPtr++] = i;
            inStack[i] = true;

            while (callStackPtr > 0) {
                int topIdx = callStackPtr - 1;
                int u = callStackNode[topIdx];
                int edgeIdx = callStackEdge[topIdx];

                List<Integer> neighbors = adj.get(u);
                if (edgeIdx < neighbors.size()) {
                    int v = neighbors.get(edgeIdx);
                    callStackEdge[topIdx] = edgeIdx + 1;

                    if (dfn[v] == 0) {
                        time++;
                        dfn[v] = time;
                        low[v] = time;
                        sccStack[sccStackPtr++] = v;
                        inStack[v] = true;

                        callStackNode[callStackPtr] = v;
                        callStackEdge[callStackPtr] = 0;
                        callStackPtr++;
                    } else if (inStack[v]) {
                        low[u] = Math.min(low[u], dfn[v]);
                    }
                } else {
                    // Finished exploring all outgoing edges from u
                    callStackPtr--;

                    if (callStackPtr > 0) {
                        int parent = callStackNode[callStackPtr - 1];
                        low[parent] = Math.min(low[parent], low[u]);
                    }

                    if (low[u] == dfn[u]) {
                        List<Integer> scc = new ArrayList<>();
                        while (sccStackPtr > 0) {
                            int popped = sccStack[--sccStackPtr];
                            inStack[popped] = false;
                            scc.add(popped);
                            if (popped == u) {
                                break;
                            }
                        }
                        sccs.add(scc);
                    }
                }
            }
        }

        return sccs;
    }

    private List<VesselModuleIdentifier> computeTopologicalOrder(
            int n,
            List<VesselModuleIdentifier> moduleList,
            Map<String, Integer> namespaceToIndex,
            List<ModuleDependencyRule> activeRules
    ) {
        int[] inDegree = new int[n];
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }

        for (ModuleDependencyRule rule : activeRules) {
            Integer sourceIdx = namespaceToIndex.get(rule.sourceModule().namespace());
            Integer targetIdx = namespaceToIndex.get(rule.targetNamespace());

            if (sourceIdx == null || targetIdx == null || sourceIdx.equals(targetIdx)) {
                continue;
            }

            if (rule.kind() == DependencyConstraintKind.MANDATORY || rule.kind() == DependencyConstraintKind.LOAD_AFTER) {
                adj.get(targetIdx).add(sourceIdx);
                inDegree[sourceIdx]++;
            } else if (rule.kind() == DependencyConstraintKind.LOAD_BEFORE) {
                adj.get(sourceIdx).add(targetIdx);
                inDegree[targetIdx]++;
            }
        }

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.add(i);
            }
        }

        List<VesselModuleIdentifier> sorted = new ArrayList<>(n);
        while (!queue.isEmpty()) {
            int current = queue.poll();
            sorted.add(moduleList.get(current));

            for (int neighbor : adj.get(current)) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != n) {
            throw new CircularDependencyException(
                    "Circular dependency remained after reduction! Processed " + sorted.size() + " of " + n,
                    List.of()
            );
        }

        return Collections.unmodifiableList(sorted);
    }
}
