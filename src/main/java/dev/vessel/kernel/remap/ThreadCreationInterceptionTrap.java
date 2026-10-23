package dev.vessel.kernel.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bytecode trap that intercepts raw OS thread instantiations ({@code new Thread(runnable).start()})
 * inside mods and plugins, redirecting execution onto managed Java 21 Loom Virtual Threads.
 *
 * <p>Prevents unbounded platform thread exhaustion and tracks per-module virtual thread spawns
 * in strict compliance with Rule 17.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-27)
 */
public final class ThreadCreationInterceptionTrap {

    private static final Map<String, AtomicInteger> INTERCEPTED_COUNTS = new ConcurrentHashMap<>();

    /**
     * Rewrites {@code java.lang.Thread#start()} invocations in the given class bytecode so they
     * route through VESSEL's managed Virtual Thread dispatcher.
     *
     * @param classBytes raw JVM classfile bytes
     * @return transformed JVM classfile bytes
     */
    public byte[] interceptRawThreadInstantiations(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");

        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        String ownerModuleKey = classNode.name != null ? classNode.name : "unknown";

        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                if (method.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode methodInsn
                            && "java/lang/Thread".equals(methodInsn.owner)
                            && "start".equals(methodInsn.name)
                            && "()V".equals(methodInsn.desc)) {
                        methodInsn.setOpcode(Opcodes.INVOKESTATIC);
                        methodInsn.owner = "dev/vessel/kernel/remap/ThreadCreationInterceptionTrap";
                        methodInsn.name = "interceptThreadStartBridge";
                        methodInsn.desc = "(Ljava/lang/Thread;)V";
                        methodInsn.itf = false;
                        INTERCEPTED_COUNTS
                                .computeIfAbsent(ownerModuleKey, k -> new AtomicInteger(0))
                                .incrementAndGet();
                    }
                }
            }
        }

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Starts the given task on a named Java 21 Virtual Thread and increments the interception counter
     * for the specified module.
     *
     * @param moduleId module identifier owning the background task
     * @param task     runnable workload
     * @return started virtual {@link Thread}
     */
    public Thread startInterceptedVirtualThread(String moduleId, Runnable task) {
        Objects.requireNonNull(moduleId, "moduleId cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        int sequence = INTERCEPTED_COUNTS
                .computeIfAbsent(moduleId, k -> new AtomicInteger(0))
                .incrementAndGet();

        return Thread.ofVirtual()
                .name("vessel-vt-" + moduleId + "-" + sequence)
                .start(task);
    }

    /**
     * Returns the number of raw thread instantiations intercepted for the given module.
     *
     * @param moduleId module identifier or internal class name
     * @return intercepted thread count
     */
    public int getInterceptedThreadCount(String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId cannot be null");
        AtomicInteger counter = INTERCEPTED_COUNTS.get(moduleId);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Static bytecode bridge target invoked in place of {@code Thread.start()}.
     *
     * @param rawThread unstarted platform thread instance
     */
    public static void interceptThreadStartBridge(Thread rawThread) {
        if (rawThread != null) {
            Thread.ofVirtual()
                    .name("vessel-intercepted-" + rawThread.getName())
                    .start(rawThread);
        }
    }
}
