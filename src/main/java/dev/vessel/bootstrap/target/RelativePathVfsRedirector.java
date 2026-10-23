package dev.vessel.bootstrap.target;

import dev.vessel.kernel.identity.SourceLoaderKind;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Locale;
import java.util.Objects;

/**
 * Redirects hardcoded relative filesystem paths ({@code "config/..."}, {@code "mods/..."},
 * {@code "./..."}) used by mods and plugins into their isolated per-loader VFS directory
 * ({@code "data/<loader>/<moduleId>/..."}) under Decision 6.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-29)
 */
public final class RelativePathVfsRedirector {

    /**
     * Redirects a relative filesystem path string into the module's isolated per-loader data scope.
     *
     * @param loader       source loader kind
     * @param moduleId     module identifier
     * @param originalPath original relative path requested by the module
     * @return redirected relative path inside {@code data/<loader>/<moduleId>/}
     */
    public String redirectRelativePath(SourceLoaderKind loader, String moduleId, String originalPath) {
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(moduleId, "moduleId cannot be null");
        Objects.requireNonNull(originalPath, "originalPath cannot be null");

        String loaderPrefix = "data/" + loader.getId().toLowerCase(Locale.ROOT) + "/" + moduleId.trim() + "/";
        String normalized = originalPath.replace('\\', '/').trim();
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("../")) {
            normalized = normalized.substring(3);
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.startsWith(loaderPrefix)) {
            return normalized;
        }
        return loaderPrefix + normalized;
    }

    /**
     * Rewrites hardcoded relative file path string constants inside a module's bytecode so that
     * direct {@code File} / {@code Path} operations target the module's per-loader directory.
     *
     * @param classBytes raw JVM classfile bytes
     * @param loader     source loader kind
     * @param moduleId   module identifier
     * @return transformed JVM classfile bytes
     */
    public byte[] rewriteFileIoBytecodeCalls(byte[] classBytes, SourceLoaderKind loader, String moduleId) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");
        Objects.requireNonNull(loader, "loader cannot be null");
        Objects.requireNonNull(moduleId, "moduleId cannot be null");

        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                if (method.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof LdcInsnNode ldcInsn && ldcInsn.cst instanceof String strVal) {
                        if (isRedirectableRelativePathLiteral(strVal)) {
                            ldcInsn.cst = redirectRelativePath(loader, moduleId, strVal);
                        }
                    }
                }
            }
        }

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static boolean isRedirectableRelativePathLiteral(String literal) {
        return literal.startsWith("config/")
                || literal.startsWith("./config/")
                || literal.startsWith("mods/")
                || literal.startsWith("plugins/")
                || literal.startsWith("saves/");
    }
}
