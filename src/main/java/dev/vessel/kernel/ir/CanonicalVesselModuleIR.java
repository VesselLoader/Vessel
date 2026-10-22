package dev.vessel.kernel.ir;

import dev.vessel.kernel.identity.VesselModuleIdentifier;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Canonical in-memory implementation of {@link VesselModuleIR}.
 * <p>
 * Aggregates pre-processed ASM {@link ClassNode} sets, semantic interception hooks,
 * access widening tables, and capability declarations.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-7)
 */
public class CanonicalVesselModuleIR implements VesselModuleIR {

    private final VesselModuleIdentifier identifier;
    private final Map<String, ClassNode> classMap = new HashMap<>();
    private final VesselAccessWideningTable accessWideningTable = new VesselAccessWideningTable();
    private final List<VesselSemanticHookRecord> hooks = new ArrayList<>();
    private final Set<String> capabilities = new HashSet<>();
    private final Set<String> dependencies = new HashSet<>();
    private final AtomicBoolean sealed = new AtomicBoolean(false);

    public CanonicalVesselModuleIR(VesselModuleIdentifier identifier) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
    }

    /**
     * Adds an ASM ClassNode representation to this module IR.
     *
     * @param node ClassNode to add
     * @throws IllegalStateException if module is already sealed
     */
    public synchronized void addClass(ClassNode node) {
        ensureUnsealed();
        Objects.requireNonNull(node, "node cannot be null");
        classMap.put(node.name, node);
    }

    /**
     * Adds a semantic bytecode hook record.
     *
     * @param hook hook intent record
     */
    public synchronized void addHook(VesselSemanticHookRecord hook) {
        ensureUnsealed();
        Objects.requireNonNull(hook, "hook cannot be null");
        hooks.add(hook);
    }

    public synchronized void addCapability(String capability) {
        ensureUnsealed();
        Objects.requireNonNull(capability, "capability cannot be null");
        capabilities.add(capability);
    }

    public synchronized void addDependency(String dependencyId) {
        ensureUnsealed();
        Objects.requireNonNull(dependencyId, "dependencyId cannot be null");
        dependencies.add(dependencyId);
    }

    public VesselAccessWideningTable getAccessWideningTable() {
        return accessWideningTable;
    }

    public synchronized List<VesselSemanticHookRecord> getHooks() {
        return Collections.unmodifiableList(new ArrayList<>(hooks));
    }

    /**
     * Seals this module IR container, making it completely immutable for runtime execution.
     */
    public void seal() {
        sealed.set(true);
    }

    @Override
    public VesselModuleIdentifier identifier() {
        return identifier;
    }

    @Override
    public synchronized Map<String, ClassNode> classes() {
        return Collections.unmodifiableMap(new HashMap<>(classMap));
    }

    @Override
    public synchronized Set<String> declaredCapabilities() {
        return Collections.unmodifiableSet(new HashSet<>(capabilities));
    }

    @Override
    public synchronized Set<String> requiredDependencies() {
        return Collections.unmodifiableSet(new HashSet<>(dependencies));
    }

    @Override
    public boolean isSealed() {
        return sealed.get();
    }

    private void ensureUnsealed() {
        if (sealed.get()) {
            throw new IllegalStateException("CanonicalVesselModuleIR for " + identifier + " is sealed");
        }
    }
}
