package dev.vessel.kernel.vstm.remap;

import dev.vessel.kernel.vstm.binary.VstmbMemorySegmentMapper;
import dev.vessel.kernel.vstm.builder.VstmbBinaryWriter;
import dev.vessel.kernel.vstm.builder.VstmTransmutationContext;
import dev.vessel.kernel.vstm.engine.VstmClassTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmDialectBridgeRouter;
import dev.vessel.kernel.vstm.engine.VstmDialectMask;
import dev.vessel.kernel.vstm.engine.VstmFieldTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmMethodTransmutationEntry;
import dev.vessel.kernel.vstm.engine.VstmResolutionFallbackChain;
import dev.vessel.kernel.vstm.engine.VstmTransmutationFastLookup;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Rigorous integration tests verifying ASM bytecode remapping, invokedynamic bootstrap retargeting,
 * and zero-loss JAR archive transmutation across ecosystem dialect boundaries.
 *
 * <p>Strictly adheres to Rule 17 (zero inner/anonymous classes) and clean-room specifications.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmBytecodeRemapperTest {

    private VstmTransmutationContext context;
    private VstmAsmRemapperBridge remapperBridge;
    private VstmClassBytecodeTransmutator classTransmutator;
    private VstmJarStreamTransmutator jarTransmutator;

    @BeforeEach
    void setUp() {
        List<VstmClassTransmutationEntry> classes = List.of(
                VstmClassTransmutationEntry.of(
                        "net/minecraft/class_1937",
                        "net/minecraft/world/level/Level",
                        VstmDialectMask.MOJMAP
                ),
                VstmClassTransmutationEntry.of(
                        "net/minecraft/class_1938",
                        "net/minecraft/world/level/LevelAccessor",
                        VstmDialectMask.MOJMAP
                ),
                VstmClassTransmutationEntry.of(
                        "net/minecraft/class_1234",
                        "net/minecraft/world/entity/Entity",
                        VstmDialectMask.MOJMAP
                ),
                VstmClassTransmutationEntry.of(
                        "net/minecraft/class_5678",
                        "net/minecraft/world/level/LevelException",
                        VstmDialectMask.MOJMAP
                ),
                VstmClassTransmutationEntry.of(
                        "net/minecraft/class_9999",
                        "net/minecraft/world/level/LevelAnnotation",
                        VstmDialectMask.MOJMAP
                )
        );

        List<VstmFieldTransmutationEntry> fields = List.of(
                VstmFieldTransmutationEntry.of(
                        "net/minecraft/world/level/Level",
                        "field_9226",
                        "Z",
                        "isClientSide",
                        VstmDialectMask.MOJMAP
                )
        );

        List<VstmMethodTransmutationEntry> methods = List.of(
                VstmMethodTransmutationEntry.of(
                        "net/minecraft/world/level/Level",
                        "method_8597",
                        "(Lnet/minecraft/world/entity/Entity;)Z",
                        "addFreshEntity",
                        "(Lnet/minecraft/world/entity/Entity;)Z",
                        VstmDialectMask.MOJMAP
                ),
                VstmMethodTransmutationEntry.of(
                        "net/minecraft/world/level/Level",
                        "method_helper",
                        "()V",
                        "helpLevel",
                        "()V",
                        VstmDialectMask.MOJMAP
                ),
                VstmMethodTransmutationEntry.of(
                        "net/minecraft/world/level/Level",
                        "lambda$action$0",
                        "()V",
                        "lambda$actionRemapped$0",
                        "()V",
                        VstmDialectMask.MOJMAP
                )
        );

        int combined = VstmDialectMask.combine(VstmDialectMask.INTERMEDIARY, VstmDialectMask.MOJMAP);
        byte[] binary = VstmbBinaryWriter.writeToByteArray((short) 767, combined, classes, fields, methods);

        MemorySegment segment = VstmbMemorySegmentMapper.mapFromByteArray(binary);
        VstmTransmutationFastLookup fastLookup = new VstmTransmutationFastLookup(segment);
        VstmResolutionFallbackChain fallbackChain = new VstmResolutionFallbackChain();
        VstmDialectBridgeRouter router = new VstmDialectBridgeRouter();

        this.context = new VstmTransmutationContext(
                "test-module:1.0.0",
                VstmDialectMask.INTERMEDIARY,
                VstmDialectMask.MOJMAP,
                fastLookup,
                fallbackChain,
                router
        );

        this.remapperBridge = new VstmAsmRemapperBridge(context);
        this.classTransmutator = new VstmClassBytecodeTransmutator();
        this.jarTransmutator = new VstmJarStreamTransmutator(classTransmutator, new VstmJarEntryNameResolver());
    }

    @Test
    @DisplayName("Verify dynamic bytecode generation and complete ASM class remapping")
    void testDynamicBytecodeRemapping() {
        byte[] originalBytecode = generateDynamicTestClass();

        byte[] transmutedBytecode = classTransmutator.transmute(originalBytecode, remapperBridge);
        assertThat(transmutedBytecode).isNotNull();
        assertThat(transmutedBytecode.length).isGreaterThan(0);

        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(transmutedBytecode);
        reader.accept(node, 0);

        // Verify Class Header & Interfaces
        assertThat(node.name).isEqualTo("net/minecraft/world/level/Level");
        assertThat(node.superName).isEqualTo("java/lang/Object");
        assertThat(node.interfaces).containsExactly("net/minecraft/world/level/LevelAccessor");

        // Verify Field Remapping
        assertThat(node.fields).hasSize(1);
        assertThat(node.fields.getFirst().name).isEqualTo("isClientSide");
        assertThat(node.fields.getFirst().desc).isEqualTo("Z");

        // Verify Methods and Instructions
        MethodNode method = node.methods.stream()
                .filter(m -> "addFreshEntity".equals(m.name))
                .findFirst()
                .orElseThrow();

        assertThat(method.desc).isEqualTo("(Lnet/minecraft/world/entity/Entity;)Z");

        boolean foundFieldInsn = false;
        boolean foundMethodInsn = false;
        boolean foundInvokeDynamic = false;

        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof FieldInsnNode fieldInsn) {
                if ("isClientSide".equals(fieldInsn.name)) {
                    assertThat(fieldInsn.owner).isEqualTo("net/minecraft/world/level/Level");
                    assertThat(fieldInsn.desc).isEqualTo("Z");
                    foundFieldInsn = true;
                }
            } else if (insn instanceof MethodInsnNode methodInsn) {
                if ("helpLevel".equals(methodInsn.name)) {
                    assertThat(methodInsn.owner).isEqualTo("net/minecraft/world/level/Level");
                    assertThat(methodInsn.desc).isEqualTo("()V");
                    foundMethodInsn = true;
                }
            } else if (insn instanceof InvokeDynamicInsnNode indyInsn) {
                assertThat(indyInsn.name).isEqualTo("run");
                assertThat(indyInsn.desc).isEqualTo("()Ljava/lang/Runnable;");
                Handle bsm = indyInsn.bsm;
                assertThat(bsm.getOwner()).isEqualTo("java/lang/invoke/LambdaMetafactory");

                // Check patched lambda impl handle in bsm arguments
                Handle implHandle = (Handle) indyInsn.bsmArgs[1];
                assertThat(implHandle.getOwner()).isEqualTo("net/minecraft/world/level/Level");
                assertThat(implHandle.getName()).isEqualTo("lambda$actionRemapped$0");
                foundInvokeDynamic = true;
            }
        }

        assertThat(foundFieldInsn).isTrue();
        assertThat(foundMethodInsn).isTrue();
        assertThat(foundInvokeDynamic).isTrue();

        // Verify Try-Catch Block exception remapping
        assertThat(method.tryCatchBlocks).isNotEmpty();
        assertThat(method.tryCatchBlocks.getFirst().type).isEqualTo("net/minecraft/world/level/LevelException");
    }

    @Test
    @DisplayName("Verify end-to-end mod JAR stream transmutation preserves 100% assets and retargets classes")
    void testJarStreamTransmutation() throws IOException {
        byte[] originalBytecode = generateDynamicTestClass();

        ByteArrayOutputStream rawJarStream = new ByteArrayOutputStream();
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Mod-Id", "vessel_test_mod");

        try (JarOutputStream jarOut = new JarOutputStream(rawJarStream, manifest)) {
            // Class file to be remapped
            JarEntry classEntry = new JarEntry("net/minecraft/class_1937.class");
            jarOut.putNextEntry(classEntry);
            jarOut.write(originalBytecode);
            jarOut.closeEntry();

            // Resource: JSON language translation
            JarEntry langEntry = new JarEntry("assets/testmod/lang/en_us.json");
            jarOut.putNextEntry(langEntry);
            byte[] langBytes = "{\"test.gear\": \"Sovereign Gear\"}".getBytes(StandardCharsets.UTF_8);
            jarOut.write(langBytes);
            jarOut.closeEntry();

            // Resource: PNG Texture dummy binary
            JarEntry pngEntry = new JarEntry("assets/testmod/textures/gear.png");
            jarOut.putNextEntry(pngEntry);
            byte[] pngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
            jarOut.write(pngBytes);
            jarOut.closeEntry();
        }

        byte[] inputJarBytes = rawJarStream.toByteArray();
        ByteArrayOutputStream outputJarStream = new ByteArrayOutputStream();

        VstmRemapStatistics stats = jarTransmutator.transmuteJar(
                new ByteArrayInputStream(inputJarBytes),
                outputJarStream,
                context
        );

        assertThat(stats.getClassesTransmuted()).isEqualTo(1);
        assertThat(stats.getResourcesPreserved()).isGreaterThanOrEqualTo(3);
        assertThat(stats.getBytesProcessed()).isGreaterThan(0L);

        // Inspect output JAR archive contents
        Map<String, byte[]> outputEntries = new HashMap<>();
        Manifest outManifest;

        try (JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(outputJarStream.toByteArray()))) {
            outManifest = jarIn.getManifest();
            JarEntry entry;
            while ((entry = jarIn.getNextJarEntry()) != null) {
                outputEntries.put(entry.getName(), jarIn.readAllBytes());
                jarIn.closeEntry();
            }
        }

        // Verify Manifest
        assertThat(outManifest).isNotNull();
        assertThat(outManifest.getMainAttributes().getValue("Mod-Id")).isEqualTo("vessel_test_mod");

        // Verify class remapping and entry rename
        assertThat(outputEntries).doesNotContainKey("net/minecraft/class_1937.class");
        assertThat(outputEntries).containsKey("net/minecraft/world/level/Level.class");

        byte[] remappedClassBytes = outputEntries.get("net/minecraft/world/level/Level.class");
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(remappedClassBytes);
        reader.accept(node, 0);
        assertThat(node.name).isEqualTo("net/minecraft/world/level/Level");

        // Verify 100% identical preservation of assets
        assertThat(outputEntries).containsKey("assets/testmod/lang/en_us.json");
        String langContent = new String(outputEntries.get("assets/testmod/lang/en_us.json"), StandardCharsets.UTF_8);
        assertThat(langContent).isEqualTo("{\"test.gear\": \"Sovereign Gear\"}");

        assertThat(outputEntries).containsKey("assets/testmod/textures/gear.png");
        byte[] expectedPng = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(outputEntries.get("assets/testmod/textures/gear.png")).isEqualTo(expectedPng);
    }

    @Test
    @DisplayName("Verify exception behavior on empty bytecode or corrupt input")
    void testExceptionHandling() {
        assertThatThrownBy(() -> classTransmutator.transmute(new byte[0], remapperBridge))
                .isInstanceOf(VstmRemapException.class)
                .hasMessageContaining("Cannot transmute empty");

        byte[] garbage = new byte[]{0x12, 0x34, 0x56, 0x78};
        assertThatThrownBy(() -> classTransmutator.transmute(garbage, remapperBridge))
                .isInstanceOf(VstmRemapException.class)
                .hasMessageContaining("Failed to transmute class bytecode");
    }

    /**
     * Synthesizes an ASM Class conforming to obfuscated Intermediary Minecraft dialect.
     */
    private byte[] generateDynamicTestClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC,
                "net/minecraft/class_1937",
                null,
                "java/lang/Object",
                new String[]{"net/minecraft/class_1938"}
        );

        // Field: field_9226 (isClientSide)
        cw.visitField(Opcodes.ACC_PUBLIC, "field_9226", "Z", null, null).visitEnd();

        // Helper Method
        MethodVisitor mvHelper = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "method_helper",
                "()V",
                null,
                null
        );
        mvHelper.visitCode();
        mvHelper.visitInsn(Opcodes.RETURN);
        mvHelper.visitMaxs(0, 0);
        mvHelper.visitEnd();

        // Lambda target Method
        MethodVisitor mvLambda = cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "lambda$action$0",
                "()V",
                null,
                null
        );
        mvLambda.visitCode();
        mvLambda.visitInsn(Opcodes.RETURN);
        mvLambda.visitMaxs(0, 0);
        mvLambda.visitEnd();

        // Main Method: method_8597 (addFreshEntity)
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "method_8597",
                "(Lnet/minecraft/class_1234;)Z",
                null,
                null
        );
        mv.visitCode();

        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        mv.visitTryCatchBlock(start, end, handler, "net/minecraft/class_5678");

        mv.visitLabel(start);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "net/minecraft/class_1937", "field_9226", "Z");
        mv.visitInsn(Opcodes.POP);

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "net/minecraft/class_1937",
                "method_helper",
                "()V",
                false
        );

        // Invokedynamic Lambda creation: Runnable r = () -> lambda$action$0();
        Handle bsm = new Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
                false
        );

        Handle implMethod = new Handle(
                Opcodes.H_INVOKESTATIC,
                "net/minecraft/class_1937",
                "lambda$action$0",
                "()V",
                false
        );

        mv.visitInvokeDynamicInsn(
                "run",
                "()Ljava/lang/Runnable;",
                bsm,
                Type.getMethodType("()V"),
                implMethod,
                Type.getMethodType("()V")
        );
        mv.visitInsn(Opcodes.POP);

        mv.visitLabel(end);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);

        // Exception handler
        mv.visitLabel(handler);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"net/minecraft/class_5678"});
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLocalVariable(
                "entity",
                "Lnet/minecraft/class_1234;",
                null,
                start,
                end,
                1
        );

        mv.visitMaxs(2, 2);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}
