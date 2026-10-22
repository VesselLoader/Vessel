package dev.vessel.kernel.identity.dependency;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.*;

/**
 * Ultra-fast topological dependency resolution engine based on Kahn's algorithm.
 * Guarantees deterministic, circular-dependency-free module bootstrap ordering
 * across multi-loader ecosystems.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastDependencyTopologicalSorter {

    /**
     * Computes a linear startup order for modules satisfying all declared dependency constraints.
     *
     * @param modules the set of all active modules
     * @param rules the set of declared dependency rules
     * @return sequentially ordered list of modules ready for bootstrap
     * @throws IllegalStateException if a circular dependency cycle is detected
     */
    public List<VesselModuleIdentifier> sort(
            Collection<VesselModuleIdentifier> modules,
            Collection<ModuleDependencyRule> rules) {
        Objects.requireNonNull(modules, "modules cannot be null");
        Objects.requireNonNull(rules, "rules cannot be null");

        // Map namespace to module identifier
        Map<String, VesselModuleIdentifier> moduleByNamespace = new HashMap<>();
        for (VesselModuleIdentifier mod : modules) {
            moduleByNamespace.put(mod.namespace(), mod);
        }

        // Build adjacency graph: prerequisite -> dependent
        Map<VesselModuleIdentifier, List<VesselModuleIdentifier>> adjacency = new HashMap<>();
        Map<VesselModuleIdentifier, Integer> inDegree = new HashMap<>();

        for (VesselModuleIdentifier mod : modules) {
            adjacency.put(mod, new ArrayList<>());
            inDegree.put(mod, 0);
        }

        for (ModuleDependencyRule rule : rules) {
            VesselModuleIdentifier source = rule.sourceModule();
            VesselModuleIdentifier target = moduleByNamespace.get(rule.targetNamespace());

            if (target == null) {
                // If dependency is mandatory and missing, handle appropriately
                if (rule.kind() == DependencyConstraintKind.MANDATORY) {
                    throw new IllegalStateException("Missing mandatory dependency: " + rule.targetNamespace()
                            + " required by " + source);
                }
                continue;
            }

            // Determine edge direction
            if (rule.kind() == DependencyConstraintKind.MANDATORY || rule.kind() == DependencyConstraintKind.LOAD_AFTER) {
                // Target must load BEFORE source => Edge: target -> source
                adjacency.get(target).add(source);
                inDegree.put(source, inDegree.get(source) + 1);
            } else if (rule.kind() == DependencyConstraintKind.LOAD_BEFORE) {
                // Source must load BEFORE target => Edge: source -> target
                adjacency.get(source).add(target);
                inDegree.put(target, inDegree.get(target) + 1);
            }
        }

        // Initialize queue with zero in-degree modules
        ArrayDeque<VesselModuleIdentifier> queue = new ArrayDeque<>();
        for (Map.Entry<VesselModuleIdentifier, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<VesselModuleIdentifier> sorted = new ArrayList<>(modules.size());
        while (!queue.isEmpty()) {
            VesselModuleIdentifier current = queue.poll();
            sorted.add(current);

            for (VesselModuleIdentifier neighbor : adjacency.get(current)) {
                int updatedInDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, updatedInDegree);
                if (updatedInDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != modules.size()) {
            throw new IllegalStateException("Circular dependency detected among modules! Processed "
                    + sorted.size() + " of " + modules.size() + " total modules.");
        }

        return Collections.unmodifiableList(sorted);
    }
}
