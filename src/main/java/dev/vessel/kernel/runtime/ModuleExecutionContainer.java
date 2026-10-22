package dev.vessel.kernel.runtime;

import dev.vessel.kernel.identity.VesselModuleIdentifier;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Execution container wrapping the runtime environment of a single VESSEL module.
 * Manages modular lifecycle transitions, ClassLoader isolation, and state metrics.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModuleExecutionContainer {

    private final VesselModuleIdentifier identifier;
    private final VesselModuleClassLoader classLoader;
    private final AtomicReference<ModuleExecutionState> state =
            new AtomicReference<>(ModuleExecutionState.INITIALIZED);

    /**
     * Constructs a new ModuleExecutionContainer.
     *
     * @param identifier the module identifier
     * @param classLoader the isolated classloader for this module
     */
    public ModuleExecutionContainer(
            VesselModuleIdentifier identifier,
            VesselModuleClassLoader classLoader) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader cannot be null");
    }

    /**
     * Transitions the module to the ACTIVE state.
     *
     * @throws IllegalStateException if transition is invalid from current state
     */
    public void start() {
        if (!state.compareAndSet(ModuleExecutionState.INITIALIZED, ModuleExecutionState.ACTIVE)
                && !state.compareAndSet(ModuleExecutionState.LOADING, ModuleExecutionState.ACTIVE)) {
            throw new IllegalStateException("Cannot start module " + identifier + " from state " + state.get());
        }
    }

    /**
     * Transitions the module to the DISABLED state.
     */
    public void stop() {
        state.set(ModuleExecutionState.DISABLED);
    }

    /**
     * Marks the module as FAILED with an error flag.
     */
    public void markFailed() {
        state.set(ModuleExecutionState.FAILED);
    }

    /**
     * Returns the module identifier.
     *
     * @return module identifier
     */
    public VesselModuleIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns the classloader associated with this module container.
     *
     * @return module class loader
     */
    public VesselModuleClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Returns the current lifecycle state of the container.
     *
     * @return current state
     */
    public ModuleExecutionState getState() {
        return state.get();
    }

    /**
     * Returns the total count of classes loaded into this container's classloader.
     *
     * @return count of loaded classes
     */
    public int getLoadedClassCount() {
        return classLoader.getLoadedClassCount();
    }
}
