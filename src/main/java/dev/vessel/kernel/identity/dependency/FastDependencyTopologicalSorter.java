package dev.vessel.kernel.identity.dependency;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.Collection;
import java.util.List;

/**
 * Backward compatibility delegation facade for {@link dev.vessel.kernel.identity.FastDependencyTopologicalSorter}.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class FastDependencyTopologicalSorter {

    private final dev.vessel.kernel.identity.FastDependencyTopologicalSorter delegate =
            new dev.vessel.kernel.identity.FastDependencyTopologicalSorter();

    /**
     * Constructs the topological sorter.
     */
    public FastDependencyTopologicalSorter() {
    }

    /**
     * Computes linear startup order delegating to the Tarjan-based solver.
     *
     * @param modules the set of all active modules
     * @param rules the set of declared dependency rules
     * @return sequentially ordered list of modules ready for bootstrap
     */
    public List<VesselModuleIdentifier> sort(
            Collection<VesselModuleIdentifier> modules,
            Collection<ModuleDependencyRule> rules
    ) {
        return delegate.sort(modules, rules);
    }
}
