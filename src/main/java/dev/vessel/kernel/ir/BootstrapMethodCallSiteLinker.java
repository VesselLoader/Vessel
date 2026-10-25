package dev.vessel.kernel.ir;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Re-links MethodHandle arguments within BootstrapMethods attributes of invokedynamic instructions.
 * <p>
 * Ensures lambda implementation handles, functional interface return types, and method types
 * remain strictly aligned when classes and methods are renamed or remapped across mod loaders.
 * </p>
 * <p>
 * Directly resolves ERR-65 and MAP-DYN-0001..0350.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BootstrapMethodCallSiteLinker {

    /**
     * Inspects and updates invokedynamic bootstrap method arguments in the provided ClassNode.
     *
     * @param node target ASM ClassNode
     * @param remapProvider remapping provider supplying name and descriptor translations
     * @return list of re-linked call site records
     */
    public List<LinkedCallSiteRecord> relink(ClassNode node, BootstrapRemapProvider remapProvider) {
        Objects.requireNonNull(node, "node cannot be null");
        Objects.requireNonNull(remapProvider, "remapProvider cannot be null");

        List<LinkedCallSiteRecord> records = new ArrayList<>();
        if (node.methods == null) {
            return records;
        }

        for (MethodNode mn : node.methods) {
            if (mn.instructions == null) {
                continue;
            }

            for (AbstractInsnNode insn : mn.instructions) {
                if (insn instanceof InvokeDynamicInsnNode indy) {
                    relinkCallSite(indy, remapProvider, records);
                }
            }
        }

        return records;
    }

    private void relinkCallSite(
            InvokeDynamicInsnNode indy,
            BootstrapRemapProvider remapProvider,
            List<LinkedCallSiteRecord> records) {
        if (indy.bsmArgs == null) {
            return;
        }

        String bsmOwner = indy.bsm.getOwner();
        String bsmName = indy.bsm.getName();
        String bsmDesc = indy.bsm.getDesc();

        for (int i = 0; i < indy.bsmArgs.length; i++) {
            Object arg = indy.bsmArgs[i];

            if (arg instanceof Handle targetHandle) {
                String oldOwner = targetHandle.getOwner();
                String oldName = targetHandle.getName();
                String oldDesc = targetHandle.getDesc();

                String newOwner = remapProvider.mapClassName(oldOwner);
                String newName = remapProvider.mapMethodName(newOwner, oldName, oldDesc);
                String newDesc = remapProvider.mapMethodDescriptor(oldDesc);

                boolean changed = !oldOwner.equals(newOwner)
                        || !oldName.equals(newName)
                        || !oldDesc.equals(newDesc);

                if (changed) {
                    Handle newHandle = new Handle(
                            targetHandle.getTag(),
                            newOwner,
                            newName,
                            newDesc,
                            targetHandle.isInterface()
                    );
                    indy.bsmArgs[i] = newHandle;
                    records.add(new LinkedCallSiteRecord(
                            bsmOwner,
                            bsmName,
                            bsmDesc,
                            i,
                            oldOwner + "#" + oldName + oldDesc,
                            newOwner + "#" + newName + newDesc
                    ));
                }
            } else if (arg instanceof Type typeArg && typeArg.getSort() == Type.METHOD) {
                String oldDesc = typeArg.getDescriptor();
                String newDesc = remapProvider.mapMethodDescriptor(oldDesc);
                if (!oldDesc.equals(newDesc)) {
                    indy.bsmArgs[i] = Type.getMethodType(newDesc);
                    records.add(new LinkedCallSiteRecord(
                            bsmOwner,
                            bsmName,
                            bsmDesc,
                            i,
                            oldDesc,
                            newDesc
                    ));
                }
            }
        }

        // Remap indy instruction descriptor itself (functional interface return type)
        String mappedIndyDesc = remapProvider.mapMethodDescriptor(indy.desc);
        if (!indy.desc.equals(mappedIndyDesc)) {
            indy.desc = mappedIndyDesc;
        }
    }
}
