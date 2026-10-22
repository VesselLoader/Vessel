package dev.vessel.kernel.ir;

import dev.vessel.kernel.identity.VesselModuleIdentifier;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;
import java.util.Set;

/**
 * Root container interface for a normalized, architecture-neutral module representation in VESSEL.
 *
 * <p>Encapsulates all compiled components of a module after pre-flight decompilation and
 * metadata normalization (Choice A: Normalized ASM {@link ClassNode} set + Semantic Tables).</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-3)
 */
public interface VesselModuleIR {

    /**
     * Returns the canonical scoped 4-tuple identity of this module.
     *
     * @return the module identifier
     */
    VesselModuleIdentifier identifier();

    /**
     * Returns an unmodifiable map of class internal names to their pre-processed ASM ClassNode representations.
     *
     * @return map of internal name to ClassNode
     */
    Map<String, ClassNode> classes();

    /**
     * Returns the set of capability keys declared or provided by this module.
     *
     * @return set of capability keys
     */
    Set<String> declaredCapabilities();

    /**
     * Returns the set of required dependencies as canonical identity strings.
     *
     * @return set of dependency identity strings
     */
    Set<String> requiredDependencies();

    /**
     * Returns whether this module has completed pre-flight verification and is sealed for execution.
     *
     * @return true if verified and sealed
     */
    boolean isSealed();
}
