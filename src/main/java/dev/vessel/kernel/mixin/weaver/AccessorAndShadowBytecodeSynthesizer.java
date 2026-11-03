package dev.vessel.kernel.mixin.weaver;

import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Synthesizes runtime access-widened target class bytecode so {@code @Accessor}, {@code @Invoker},
 * and {@code @Shadow} Mixin members can read, write, and invoke target fields and methods directly
 * without triggering {@link IllegalAccessError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AccessorAndShadowBytecodeSynthesizer {

    /**
     * Widens matching fields and methods in {@code targetClassBytes} from {@code private} or
     * {@code protected} to {@code public}. If {@code fieldNamesToWiden} is empty, widens all
     * non-public fields and methods in the target class.
     *
     * @param targetClassBytes  raw JVM classfile bytes of the target class (must not be null)
     * @param fieldNamesToWiden set of specific field/method names to widen, or empty to widen all
     * @return access-widened JVM classfile bytes
     */
    public byte[] widenTargetFieldsAndMethodsToPublic(
            byte[] targetClassBytes,
            Set<String> fieldNamesToWiden) {
        Objects.requireNonNull(targetClassBytes, "targetClassBytes must not be null");
        if (targetClassBytes.length < 8) {
            return targetClassBytes;
        }
        if ((targetClassBytes[0] & 0xFF) != 0xCA
                || (targetClassBytes[1] & 0xFF) != 0xFE
                || (targetClassBytes[2] & 0xFF) != 0xBA
                || (targetClassBytes[3] & 0xFF) != 0xBE) {
            return targetClassBytes;
        }
        try {
            ClassReader cr = new ClassReader(targetClassBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            boolean widenAll = fieldNamesToWiden == null || fieldNamesToWiden.isEmpty();
            cn.access = (cn.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;

            if (cn.fields != null) {
                for (FieldNode fn : cn.fields) {
                    if (widenAll || fieldNamesToWiden.contains(fn.name)) {
                        fn.access = (fn.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL))
                                | Opcodes.ACC_PUBLIC;
                    }
                }
            }

            if (cn.methods != null) {
                for (MethodNode mn : cn.methods) {
                    if (!"<clinit>".equals(mn.name) && (widenAll || fieldNamesToWiden.contains(mn.name))) {
                        mn.access = (mn.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED))
                                | Opcodes.ACC_PUBLIC;
                    }
                }
            }

            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            return cw.toByteArray();
        } catch (Exception e) {
            return targetClassBytes;
        }
    }
}
