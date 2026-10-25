package dev.vessel.kernel.bootstrap;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of symbols, annotations, and metadata extracted from class bytecode
 * without loading the class into the JVM.
 *
 * @param className internal class name (e.g., "net/example/MyMod")
 * @param superName internal name of the superclass
 * @param interfaces unmodifiable list of implemented interface internal names
 * @param annotations unmodifiable set of annotation descriptors (e.g., "Lnet/neoforged/fml/common/Mod;")
 * @param entrypoints list of discovered entrypoint candidate names
 * @param mixinTargets list of target class names if this class is annotated with Mixin
 * @param isAbstract whether the class is abstract
 * @param isInterface whether the class is an interface
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ScannedClassSymbols(
        String className,
        String superName,
        List<String> interfaces,
        Set<String> annotations,
        List<String> entrypoints,
        List<String> mixinTargets,
        boolean isAbstract,
        boolean isInterface
) {
    public ScannedClassSymbols {
        Objects.requireNonNull(className, "className cannot be null");
        interfaces = List.copyOf(interfaces != null ? interfaces : List.of());
        annotations = Set.copyOf(annotations != null ? annotations : Set.of());
        entrypoints = List.copyOf(entrypoints != null ? entrypoints : List.of());
        mixinTargets = List.copyOf(mixinTargets != null ? mixinTargets : List.of());
    }

    /**
     * Checks if this class has the specified annotation descriptor.
     *
     * @param annotationDescriptor the descriptor to search (e.g., "Ldev/vessel/annotation/Module;")
     * @return true if present
     */
    public boolean hasAnnotation(String annotationDescriptor) {
        if (annotationDescriptor == null) {
            return false;
        }
        return annotations.contains(annotationDescriptor);
    }

    /**
     * Checks if this class has any annotation whose descriptor contains the given substring.
     *
     * @param partialName simple or partial name (e.g. "Mod", "Mixin")
     * @return true if matching annotation is found
     */
    public boolean hasAnnotationMatching(String partialName) {
        if (partialName == null || partialName.isEmpty()) {
            return false;
        }
        for (String ann : annotations) {
            if (ann.contains(partialName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this class targets the specified target class via Mixin.
     *
     * @param targetClassName class name to match
     * @return true if this class targets it
     */
    public boolean targetsClass(String targetClassName) {
        if (targetClassName == null) {
            return false;
        }
        return mixinTargets.contains(targetClassName);
    }
}
