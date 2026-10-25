package dev.vessel.kernel.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ahead-of-time and load-time bytecode remapper translating class, field, method,
 * {@code INVOKEDYNAMIC} handles, and reflective string constants into Mojang Official Mappings.
 *
 * <p>Operates directly on ASM {@link ClassNode} trees with zero inner classes (Rule 17)
 * and translates reflection string literals per Lesson L11.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-27)
 */
public final class ZeroCopyBytecodeRemapper {

    /**
     * Remaps raw class bytecode using the provided {@link CompactMappingDictionary}.
     *
     * @param rawClassBytes source JVM classfile bytes
     * @param dictionary    mapping dictionary to apply
     * @return remapped JVM classfile bytes
     */
    public byte[] remapClassBytes(byte[] rawClassBytes, CompactMappingDictionary dictionary) {
        Objects.requireNonNull(rawClassBytes, "rawClassBytes cannot be null");
        Objects.requireNonNull(dictionary, "dictionary cannot be null");

        ClassReader reader = new ClassReader(rawClassBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        String originalClassName = classNode.name;
        classNode.name = dictionary.mapClass(classNode.name).remappedName();

        if (classNode.superName != null) {
            classNode.superName = dictionary.mapClass(classNode.superName).remappedName();
        }

        if (classNode.interfaces != null && !classNode.interfaces.isEmpty()) {
            List<String> remappedInterfaces = new ArrayList<>(classNode.interfaces.size());
            for (String iface : classNode.interfaces) {
                remappedInterfaces.add(dictionary.mapClass(iface).remappedName());
            }
            classNode.interfaces = remappedInterfaces;
        }

        if (classNode.fields != null) {
            for (FieldNode field : classNode.fields) {
                MappingLookupResult fieldResult = dictionary.mapField(originalClassName, field.name, field.desc);
                field.name = fieldResult.remappedName();
                field.desc = mapMethodDescriptor(fieldResult.remappedDescriptor(), dictionary);
            }
        }

        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                MappingLookupResult methodResult = dictionary.mapMethod(originalClassName, method.name, method.desc);
                method.name = methodResult.remappedName();
                method.desc = mapMethodDescriptor(methodResult.remappedDescriptor(), dictionary);
                remapMethodBody(method, dictionary);
            }
        }

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Remaps all internal class names embedded inside a JVM method or field descriptor.
     *
     * @param descriptor JVM method or field descriptor
     * @param dictionary mapping dictionary
     * @return descriptor with all {@code L...;} class references translated
     */
    public String mapMethodDescriptor(String descriptor, CompactMappingDictionary dictionary) {
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        Objects.requireNonNull(dictionary, "dictionary cannot be null");

        StringBuilder sb = new StringBuilder(descriptor.length());
        int i = 0;
        while (i < descriptor.length()) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int semicolon = descriptor.indexOf(';', i + 1);
                if (semicolon > i) {
                    String internalName = descriptor.substring(i + 1, semicolon);
                    String mappedName = dictionary.mapClass(internalName).remappedName();
                    sb.append('L').append(mappedName).append(';');
                    i = semicolon + 1;
                    continue;
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    /**
     * Remaps an {@code INVOKEDYNAMIC} bootstrap handle or lambda target signature.
     *
     * @param bsmOwner      bootstrap or target owner internal name
     * @param bsmName       bootstrap or target method name
     * @param bsmDescriptor bootstrap or target method descriptor
     * @param dictionary    mapping dictionary
     * @return canonical remapped handle representation {@code owner.name(desc)}
     */
    public String mapInvokeDynamicHandle(
            String bsmOwner,
            String bsmName,
            String bsmDescriptor,
            CompactMappingDictionary dictionary
    ) {
        Objects.requireNonNull(bsmOwner, "bsmOwner cannot be null");
        Objects.requireNonNull(bsmName, "bsmName cannot be null");
        Objects.requireNonNull(bsmDescriptor, "bsmDescriptor cannot be null");
        Objects.requireNonNull(dictionary, "dictionary cannot be null");

        String mappedOwner = dictionary.mapClass(bsmOwner).remappedName();
        MappingLookupResult methodLookup = dictionary.mapMethod(bsmOwner, bsmName, bsmDescriptor);
        String mappedName = methodLookup.remappedName();
        String mappedDesc = mapMethodDescriptor(methodLookup.remappedDescriptor(), dictionary);
        return mappedOwner + "." + mappedName + mappedDesc;
    }

    /**
     * Translates reflection string constants (such as {@code Class.forName("net.minecraft.class_1937")}
     * or {@code getDeclaredMethod("method_8525")}) in accordance with Lesson L11.
     *
     * @param literalConstant original string literal in the constant pool
     * @param dictionary      mapping dictionary
     * @return remapped string literal, or the original literal if no mapping applies
     */
    public String mapReflectionStringLiteral(String literalConstant, CompactMappingDictionary dictionary) {
        Objects.requireNonNull(literalConstant, "literalConstant cannot be null");
        Objects.requireNonNull(dictionary, "dictionary cannot be null");

        if (literalConstant.isEmpty()) {
            return literalConstant;
        }

        if (literalConstant.contains("/")) {
            MappingLookupResult classLookup = dictionary.mapClass(literalConstant);
            if (classLookup.found()) {
                return classLookup.remappedName();
            }
        }

        if (literalConstant.contains(".")) {
            String asInternal = literalConstant.replace('.', '/');
            MappingLookupResult classLookup = dictionary.mapClass(asInternal);
            if (classLookup.found()) {
                return classLookup.remappedName().replace('/', '.');
            }
        }

        String bareMember = dictionary.mapBareMemberSymbol(literalConstant);
        if (bareMember != null) {
            return bareMember;
        }

        return literalConstant;
    }

    private void remapMethodBody(MethodNode method, CompactMappingDictionary dictionary) {
        if (method.instructions != null) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof TypeInsnNode typeInsn) {
                    typeInsn.desc = remapInternalOrArrayDesc(typeInsn.desc, dictionary);
                } else if (insn instanceof FieldInsnNode fieldInsn) {
                    MappingLookupResult fieldResult = dictionary.mapField(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
                    fieldInsn.owner = dictionary.mapClass(fieldInsn.owner).remappedName();
                    fieldInsn.name = fieldResult.remappedName();
                    fieldInsn.desc = mapMethodDescriptor(fieldResult.remappedDescriptor(), dictionary);
                } else if (insn instanceof MethodInsnNode methodInsn) {
                    MappingLookupResult methodResult = dictionary.mapMethod(methodInsn.owner, methodInsn.name, methodInsn.desc);
                    methodInsn.owner = dictionary.mapClass(methodInsn.owner).remappedName();
                    methodInsn.name = methodResult.remappedName();
                    methodInsn.desc = mapMethodDescriptor(methodResult.remappedDescriptor(), dictionary);
                } else if (insn instanceof InvokeDynamicInsnNode indyInsn) {
                    indyInsn.desc = mapMethodDescriptor(indyInsn.desc, dictionary);
                    indyInsn.bsm = remapHandle(indyInsn.bsm, dictionary);
                    if (indyInsn.bsmArgs != null) {
                        for (int i = 0; i < indyInsn.bsmArgs.length; i++) {
                            Object arg = indyInsn.bsmArgs[i];
                            if (arg instanceof Type typeArg) {
                                indyInsn.bsmArgs[i] = Type.getType(mapMethodDescriptor(typeArg.getDescriptor(), dictionary));
                            } else if (arg instanceof Handle handleArg) {
                                indyInsn.bsmArgs[i] = remapHandle(handleArg, dictionary);
                            }
                        }
                    }
                } else if (insn instanceof LdcInsnNode ldcInsn) {
                    if (ldcInsn.cst instanceof String strConst) {
                        ldcInsn.cst = mapReflectionStringLiteral(strConst, dictionary);
                    } else if (ldcInsn.cst instanceof Type typeConst) {
                        ldcInsn.cst = Type.getType(mapMethodDescriptor(typeConst.getDescriptor(), dictionary));
                    }
                } else if (insn instanceof MultiANewArrayInsnNode multiArrayInsn) {
                    multiArrayInsn.desc = mapMethodDescriptor(multiArrayInsn.desc, dictionary);
                }
            }
        }

        if (method.tryCatchBlocks != null) {
            for (TryCatchBlockNode tcb : method.tryCatchBlocks) {
                if (tcb.type != null) {
                    tcb.type = dictionary.mapClass(tcb.type).remappedName();
                }
            }
        }

        if (method.localVariables != null) {
            for (LocalVariableNode localVar : method.localVariables) {
                if (localVar.desc != null) {
                    localVar.desc = mapMethodDescriptor(localVar.desc, dictionary);
                }
            }
        }
    }

    private Handle remapHandle(Handle handle, CompactMappingDictionary dictionary) {
        String mappedOwner = dictionary.mapClass(handle.getOwner()).remappedName();
        MappingLookupResult memberResult = dictionary.mapMethod(handle.getOwner(), handle.getName(), handle.getDesc());
        String mappedName = memberResult.remappedName();
        String mappedDesc = mapMethodDescriptor(memberResult.remappedDescriptor(), dictionary);
        return new Handle(handle.getTag(), mappedOwner, mappedName, mappedDesc, handle.isInterface());
    }

    private String remapInternalOrArrayDesc(String desc, CompactMappingDictionary dictionary) {
        if (desc.startsWith("[")) {
            return mapMethodDescriptor(desc, dictionary);
        }
        return dictionary.mapClass(desc).remappedName();
    }
}
