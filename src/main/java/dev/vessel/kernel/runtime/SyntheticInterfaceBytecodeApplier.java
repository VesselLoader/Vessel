package dev.vessel.kernel.runtime;

import org.objectweb.asm.tree.ClassNode;

import java.util.Objects;
import java.util.Set;

/**
 * High-performance bytecode transformation worker that attaches synthetic
 * interfaces directly to an ASM ClassNode before class definition.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SyntheticInterfaceBytecodeApplier {

    /**
     * Injects synthetic interfaces into the specified ClassNode in-place.
     * Guarantees no duplicate interface entries are added.
     *
     * @param classNode the ASM ClassNode to modify
     * @param interfaceInternalNames the set of internal interface names to inject
     * @return the number of newly added interfaces
     */
    public int apply(ClassNode classNode, Set<String> interfaceInternalNames) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        if (interfaceInternalNames == null || interfaceInternalNames.isEmpty()) {
            return 0;
        }

        int addedCount = 0;
        for (String iface : interfaceInternalNames) {
            String normalized = iface.replace('.', '/');
            if (!classNode.interfaces.contains(normalized)) {
                classNode.interfaces.add(normalized);
                addedCount++;
            }
        }

        return addedCount;
    }
}
