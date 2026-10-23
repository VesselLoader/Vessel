package dev.vessel.kernel.remap;

import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Strips client-only annotations ({@code @OnlyIn(Dist.CLIENT)}, {@code @Environment(EnvType.CLIENT)}),
 * fields, methods, associated synthetic lambda methods, and {@code META-INF/services/} providers
 * per Lesson L20.
 *
 * <p>Operates directly on ASM {@link ClassNode} structures with zero inner classes (Rule 17).</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-27)
 */
public final class ClientOnlySideStripper {

    /**
     * Determines whether a scanned mod archive is strictly client-only and should be skipped
     * on the dedicated server.
     *
     * @param symbols             scanned class symbols from the archive entrypoint
     * @param environmentMetadata declared environment string from mod metadata (e.g. {@code "client"})
     * @return {@code true} if the archive is exclusively client-side
     */
    public boolean isClientOnlyModArchive(ScannedClassSymbols symbols, String environmentMetadata) {
        Objects.requireNonNull(symbols, "symbols cannot be null");

        if (environmentMetadata != null) {
            String normalizedEnv = environmentMetadata.trim().toLowerCase(Locale.ROOT);
            if ("client".equals(normalizedEnv) || "client_only".equals(normalizedEnv)) {
                return true;
            }
        }

        if (symbols.hasAnnotationMatching("OnlyIn") || symbols.hasAnnotationMatching("Environment")) {
            if (symbols.className().toLowerCase(Locale.ROOT).contains("client")) {
                return true;
            }
        }

        for (String iface : symbols.interfaces()) {
            if (iface.endsWith("ClientModInitializer")
                    || iface.contains("/client/")
                    || iface.endsWith("ClientBundle")) {
                return true;
            }
        }

        String superName = symbols.superName();
        return superName != null && (superName.contains("net/minecraft/client/")
                || superName.endsWith("Screen")
                || superName.endsWith("EntityRenderer"));
    }

    /**
     * Strips fields, methods, client interfaces, and synthetic lambda methods referenced by
     * client-annotated methods from the given class bytecode (Lesson L20).
     *
     * @param classBytes input JVM classfile bytes
     * @return sanitized JVM classfile bytes safe for dedicated server loading
     */
    public byte[] stripAnnotatedClientMembers(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");

        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        if (classNode.interfaces != null && !classNode.interfaces.isEmpty()) {
            List<String> retainedInterfaces = new ArrayList<>(classNode.interfaces.size());
            for (String iface : classNode.interfaces) {
                if (!isClientOnlyInterface(iface)) {
                    retainedInterfaces.add(iface);
                }
            }
            classNode.interfaces = retainedInterfaces;
        }

        if (classNode.fields != null && !classNode.fields.isEmpty()) {
            List<FieldNode> retainedFields = new ArrayList<>(classNode.fields.size());
            for (FieldNode field : classNode.fields) {
                if (!hasClientOnlyAnnotation(field.visibleAnnotations, field.invisibleAnnotations)) {
                    retainedFields.add(field);
                }
            }
            classNode.fields = retainedFields;
        }

        if (classNode.methods != null && !classNode.methods.isEmpty()) {
            Set<String> strippedLambdaTargets = new HashSet<>();
            List<MethodNode> firstPassRetained = new ArrayList<>(classNode.methods.size());

            for (MethodNode method : classNode.methods) {
                if (hasClientOnlyAnnotation(method.visibleAnnotations, method.invisibleAnnotations)) {
                    collectSyntheticLambdaTargets(classNode.name, method, strippedLambdaTargets);
                } else {
                    firstPassRetained.add(method);
                }
            }

            List<MethodNode> finalRetained = new ArrayList<>(firstPassRetained.size());
            for (MethodNode method : firstPassRetained) {
                String key = method.name + method.desc;
                if (method.name.startsWith("lambda$") && strippedLambdaTargets.contains(key)) {
                    continue;
                }
                finalRetained.add(method);
            }
            classNode.methods = finalRetained;
        }

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Filters a {@code META-INF/services/} provider configuration file, removing any lines referencing
     * stripped client-only classes (Lesson L20).
     *
     * @param serviceFileContent raw text content of the service file
     * @param strippedClassNames set of stripped internal or binary class names
     * @return filtered service file content
     */
    public String filterMetaInfServices(String serviceFileContent, Set<String> strippedClassNames) {
        Objects.requireNonNull(serviceFileContent, "serviceFileContent cannot be null");
        Objects.requireNonNull(strippedClassNames, "strippedClassNames cannot be null");

        Set<String> normalizedStripped = new HashSet<>(strippedClassNames.size() * 2);
        for (String name : strippedClassNames) {
            normalizedStripped.add(name.replace('/', '.'));
            normalizedStripped.add(name.replace('.', '/'));
        }

        StringBuilder result = new StringBuilder();
        String[] lines = serviceFileContent.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int commentIdx = trimmed.indexOf('#');
            String providerClass = (commentIdx >= 0 ? trimmed.substring(0, commentIdx) : trimmed).trim();
            if (!normalizedStripped.contains(providerClass)) {
                if (!result.isEmpty()) {
                    result.append('\n');
                }
                result.append(providerClass);
            }
        }
        return result.toString();
    }

    private static boolean isClientOnlyInterface(String internalName) {
        return internalName.startsWith("net/minecraft/client/")
                || internalName.endsWith("ClientModInitializer")
                || internalName.contains("/client/renderer/");
    }

    private static boolean hasClientOnlyAnnotation(
            List<AnnotationNode> visible,
            List<AnnotationNode> invisible
    ) {
        return containsClientAnnotationInList(visible) || containsClientAnnotationInList(invisible);
    }

    private static boolean containsClientAnnotationInList(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode ann : annotations) {
            if (ann.desc == null) {
                continue;
            }
            if (ann.desc.contains("OnlyIn") || ann.desc.contains("Environment")) {
                if (ann.values == null || ann.values.isEmpty()) {
                    return true;
                }
                for (Object val : ann.values) {
                    if (val instanceof String strVal && strVal.toUpperCase(Locale.ROOT).contains("CLIENT")) {
                        return true;
                    } else if (val instanceof String[] enumArr && enumArr.length >= 2
                            && enumArr[1].toUpperCase(Locale.ROOT).contains("CLIENT")) {
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static void collectSyntheticLambdaTargets(
            String ownerInternalName,
            MethodNode strippedMethod,
            Set<String> strippedLambdaTargets
    ) {
        if (strippedMethod.instructions == null) {
            return;
        }
        for (AbstractInsnNode insn = strippedMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof InvokeDynamicInsnNode indy && indy.bsmArgs != null) {
                for (Object arg : indy.bsmArgs) {
                    if (arg instanceof Handle handle
                            && ownerInternalName.equals(handle.getOwner())
                            && handle.getName().startsWith("lambda$")) {
                        strippedLambdaTargets.add(handle.getName() + handle.getDesc());
                    }
                }
            } else if (insn instanceof MethodInsnNode methodInsn
                    && ownerInternalName.equals(methodInsn.owner)
                    && methodInsn.name.startsWith("lambda$")) {
                strippedLambdaTargets.add(methodInsn.name + methodInsn.desc);
            }
        }
    }
}
