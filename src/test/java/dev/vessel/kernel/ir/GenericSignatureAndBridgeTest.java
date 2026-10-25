package dev.vessel.kernel.ir;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying Generic Signature reconciliation, Bridge deduplication,
 * Universal Typed IR emission, and Dynamic bootstrap/lambda remapping.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GenericSignatureAndBridgeTest {

    @Test
    @DisplayName("UniversalTypedIrEmitter correctly projects ASM ClassNode into UniversalTypedClassIR")
    public void testUniversalTypedIrEmission() {
        UniversalTypedIrEmitter emitter = new UniversalTypedIrEmitter();
        ClassNode classNode = new ClassNode();
        classNode.name = "net/minecraft/world/item/ItemStack";
        classNode.superName = "java/lang/Object";
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
        classNode.interfaces.add("java/lang/Comparable");

        FieldNode countField = new FieldNode(Opcodes.ACC_PRIVATE, "count", "I", null, null);
        classNode.fields.add(countField);

        MethodNode getCountMethod = new MethodNode(Opcodes.ACC_PUBLIC, "getCount", "()I", null, null);
        classNode.methods.add(getCountMethod);

        UniversalTypedClassIR classIR = emitter.emit(classNode);

        assertThat(classIR.internalName()).isEqualTo("net/minecraft/world/item/ItemStack");
        assertThat(classIR.superName()).isEqualTo("java/lang/Object");
        assertThat(classIR.interfaces()).contains("java/lang/Comparable");
        assertThat(classIR.fields()).hasSize(1);
        assertThat(classIR.fields().get(0).name()).isEqualTo("count");
        assertThat(classIR.methods()).hasSize(1);
        assertThat(classIR.methods().get(0).name()).isEqualTo("getCount");
        assertThat(classIR.isInterface()).isFalse();
    }

    @Test
    @DisplayName("GenericSignatureTypeErasureReconciler strips malformed generic signatures")
    public void testMalformedSignatureStripping() {
        GenericSignatureTypeErasureReconciler reconciler = new GenericSignatureTypeErasureReconciler();
        ClassNode classNode = new ClassNode();
        classNode.name = "com/example/CorruptedGenericClass";

        // Method with malformed signature (unbalanced type arguments)
        MethodNode badMethod = new MethodNode(Opcodes.ACC_PUBLIC, "process", "(Ljava/lang/Object;)V", "<T:Ljava/lang/Object;", null);
        classNode.methods.add(badMethod);

        // Method with valid signature
        MethodNode goodMethod = new MethodNode(Opcodes.ACC_PUBLIC, "identity", "(Ljava/lang/Object;)Ljava/lang/Object;", "<T:Ljava/lang/Object;>(TT;)TT;", null);
        classNode.methods.add(goodMethod);

        SignatureReconcileResult result = reconciler.reconcile(classNode);

        assertThat(result.hasModifications()).isTrue();
        assertThat(result.strippedMalformedCount()).isEqualTo(1);
        assertThat(badMethod.signature).isNull();
        assertThat(goodMethod.signature).isNotNull();
    }

    @Test
    @DisplayName("CovariantBridgeMethodDeduplicator prunes duplicate synthetic bridge methods")
    public void testBridgeMethodDeduplication() {
        CovariantBridgeMethodDeduplicator deduplicator = new CovariantBridgeMethodDeduplicator();
        ClassNode classNode = new ClassNode();
        classNode.name = "com/example/MyCustomList";

        // Regular implementation method
        MethodNode realMethod = new MethodNode(Opcodes.ACC_PUBLIC, "get", "(I)Ljava/lang/String;", null, null);
        realMethod.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        realMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
        classNode.methods.add(realMethod);

        // First synthetic bridge method
        MethodNode bridge1 = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "get", "(I)Ljava/lang/String;", null, null);
        bridge1.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        bridge1.instructions.add(new InsnNode(Opcodes.ARETURN));
        classNode.methods.add(bridge1);

        // Second synthetic bridge duplicate
        MethodNode bridge2 = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "get", "(I)Ljava/lang/String;", null, null);
        bridge2.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        bridge2.instructions.add(new InsnNode(Opcodes.ARETURN));
        classNode.methods.add(bridge2);

        BridgeDeduplicationReport report = deduplicator.deduplicate(classNode);

        assertThat(report.hasPrunedBridges()).isTrue();
        assertThat(report.removedBridgeCount()).isEqualTo(2);
        // Only the real non-bridge method remains
        assertThat(classNode.methods).hasSize(1);
        assertThat((classNode.methods.get(0).access & Opcodes.ACC_BRIDGE)).isZero();
    }

    @Test
    @DisplayName("BootstrapMethodCallSiteLinker updates lambda method handles in invokedynamic instruction")
    public void testBootstrapMethodCallSiteLinker() {
        BootstrapMethodCallSiteLinker linker = new BootstrapMethodCallSiteLinker();
        SimpleTestBootstrapRemapProvider remapProvider = new SimpleTestBootstrapRemapProvider();
        remapProvider.addClassMapping("net/minecraft/obf/OldClass", "net/minecraft/world/entity/Entity");
        remapProvider.addMethodMapping("obfTick", "tick");

        ClassNode classNode = new ClassNode();
        classNode.name = "com/example/MyLambdaHolder";
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "runAction", "()V", null, null);

        Handle bsmHandle = new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
        );

        Handle lambdaImplHandle = new Handle(
                Opcodes.H_INVOKEVIRTUAL,
                "net/minecraft/obf/OldClass",
                "obfTick",
                "()V",
                false
        );

        Object[] bsmArgs = new Object[]{
                Type.getMethodType("()V"),
                lambdaImplHandle,
                Type.getMethodType("()V")
        };

        InvokeDynamicInsnNode indy = new InvokeDynamicInsnNode("run", "()Ljava/lang/Runnable;", bsmHandle, bsmArgs);
        mn.instructions.add(indy);
        classNode.methods.add(mn);

        List<LinkedCallSiteRecord> records = linker.relink(classNode, remapProvider);

        assertThat(records).isNotEmpty();
        Handle updatedHandle = (Handle) indy.bsmArgs[1];
        assertThat(updatedHandle.getOwner()).isEqualTo("net/minecraft/world/entity/Entity");
        assertThat(updatedHandle.getName()).isEqualTo("tick");
    }

    @Test
    @DisplayName("SerializedLambdaCaptureRemapper updates string constants in $deserializeLambda$ method")
    public void testSerializedLambdaCaptureRemapper() {
        SerializedLambdaCaptureRemapper remapper = new SerializedLambdaCaptureRemapper();
        SimpleTestBootstrapRemapProvider remapProvider = new SimpleTestBootstrapRemapProvider();
        remapProvider.addClassMapping("net/minecraft/obf/OldCapturedClass", "net/minecraft/world/level/Level");

        ClassNode classNode = new ClassNode();
        classNode.name = "com/example/SerializableLambdaHolder";

        MethodNode deserializeMethod = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                SerializedLambdaCaptureRemapper.DESERIALIZE_LAMBDA_NAME,
                SerializedLambdaCaptureRemapper.DESERIALIZE_LAMBDA_DESC,
                null,
                null
        );

        LdcInsnNode ldcClass = new LdcInsnNode("net/minecraft/obf/OldCapturedClass");
        deserializeMethod.instructions.add(ldcClass);
        deserializeMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
        classNode.methods.add(deserializeMethod);

        LambdaCaptureRemapResult result = remapper.remap(classNode, remapProvider);

        assertThat(result.hasDeserializeLambdaMethod()).isTrue();
        assertThat(result.wasModified()).isTrue();
        assertThat(ldcClass.cst).isEqualTo("net/minecraft/world/level/Level");
    }
}
