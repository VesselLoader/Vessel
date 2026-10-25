package dev.vessel.kernel.ir;

import dev.vessel.kernel.runtime.InterfaceInjectionRegistry;
import dev.vessel.kernel.runtime.SyntheticInterfaceBytecodeApplier;
import dev.vessel.kernel.transform.mixin.FastOpcodeMatcher;
import dev.vessel.kernel.transform.mixin.SemanticBytecodeTransformer;
import dev.vessel.kernel.transform.mixin.SemanticHookRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying single-pass unified bytecode transformation pipeline behavior.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SinglePassBytecodePipelineTest {

    private BytecodeToIRParser parser;
    private VesselAccessWideningTable wideningTable;
    private VesselAccessWidenerApplier widenerApplier;
    private InterfaceInjectionRegistry interfaceRegistry;
    private SyntheticInterfaceBytecodeApplier interfaceApplier;
    private SemanticHookRegistry hookRegistry;
    private SemanticBytecodeTransformer hookTransformer;
    private GenericSignatureTypeErasureReconciler signatureReconciler;
    private CovariantBridgeMethodDeduplicator bridgeDeduplicator;
    private SinglePassBytecodePipeline pipeline;

    @BeforeEach
    public void setUp() {
        parser = new BytecodeToIRParser();
        wideningTable = new VesselAccessWideningTable();
        widenerApplier = new VesselAccessWidenerApplier();
        interfaceRegistry = new InterfaceInjectionRegistry();
        interfaceApplier = new SyntheticInterfaceBytecodeApplier();
        hookRegistry = new SemanticHookRegistry();
        hookTransformer = new SemanticBytecodeTransformer(new FastOpcodeMatcher());
        signatureReconciler = new GenericSignatureTypeErasureReconciler();
        bridgeDeduplicator = new CovariantBridgeMethodDeduplicator();

        pipeline = new SinglePassBytecodePipeline(
                parser,
                wideningTable,
                widenerApplier,
                interfaceRegistry,
                interfaceApplier,
                hookRegistry,
                hookTransformer,
                signatureReconciler,
                bridgeDeduplicator,
                SinglePassPipelineConfig.DEFAULT
        );
    }

    @Test
    @DisplayName("Fast-path bypass returns original unmodified bytecode when no rules apply")
    public void testFastPathBypass() {
        byte[] dummyBytecode = createDummyClassBytecode("com/example/UnmodifiedClass", Opcodes.ACC_PUBLIC);

        SinglePassPipelineResult result = pipeline.process("com/example/UnmodifiedClass", dummyBytecode);

        assertThat(result.modified()).isFalse();
        assertThat(result.transformationsApplied()).isEqualTo(0);
        assertThat(result.bytecode()).isSameAs(dummyBytecode);
    }

    @Test
    @DisplayName("Single-pass pipeline modifies class access and injects interfaces in one run")
    public void testAccessWideningAndInterfaceInjection() {
        String className = "com/example/TargetClass";
        byte[] original = createDummyClassBytecode(className, 0); // package-private

        // Configure widening to make class public
        wideningTable.addClassRule(className, VesselAccessLevel.PUBLIC, true);
        // Configure interface injection
        interfaceRegistry.register(className, "java/lang/Cloneable");

        SinglePassPipelineResult result = pipeline.process(className, original);

        assertThat(result.modified()).isTrue();
        assertThat(result.transformationsApplied()).isGreaterThanOrEqualTo(2);
        assertThat(result.bytecode()).isNotSameAs(original);

        // Verify resulting bytecode with ClassReader
        ClassReader reader = new ClassReader(result.bytecode());
        ClassNode transformedNode = new ClassNode();
        reader.accept(transformedNode, 0);

        assertThat((transformedNode.access & Opcodes.ACC_PUBLIC)).isNotZero();
        assertThat(transformedNode.interfaces).contains("java/lang/Cloneable");
    }

    @Test
    @DisplayName("Transform direct byte array helper method produces valid transformed bytecode")
    public void testDirectTransformHelper() {
        String className = "com/example/AnotherTarget";
        byte[] original = createDummyClassBytecode(className, 0);

        interfaceRegistry.register(className, "java/io/Serializable");

        byte[] transformed = pipeline.transform(className, original);

        assertThat(transformed).isNotNull();
        assertThat(transformed).isNotSameAs(original);

        ClassReader reader = new ClassReader(transformed);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);

        assertThat(node.interfaces).contains("java/io/Serializable");
    }

    private byte[] createDummyClassBytecode(String internalName, int access) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, access, internalName, null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
}
