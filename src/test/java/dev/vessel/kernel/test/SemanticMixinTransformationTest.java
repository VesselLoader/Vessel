package dev.vessel.kernel.test;

import dev.vessel.kernel.ir.*;
import dev.vessel.kernel.runtime.InterfaceInjectionRegistry;
import dev.vessel.kernel.runtime.SyntheticInterfaceBytecodeApplier;
import dev.vessel.kernel.transform.mixin.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite for Phase 12 (UIR Core), Phase 19 (Bytecode Conversion),
 * and Phase 20 (Semantic Mixin & Bytecode Transformation Engine).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class SemanticMixinTransformationTest {

    @Test
    @DisplayName("MixinAnnotationParser parses @Inject, @WrapOperation, and @ModifyVariable")
    void testMixinAnnotationParser() {
        MixinAnnotationParser parser = new MixinAnnotationParser();

        // 1. Construct MethodNode with @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
        MethodNode injectMethod = new MethodNode(Opcodes.ACC_PUBLIC, "onTick", "()V", null, null);
        injectMethod.visibleAnnotations = new ArrayList<>();

        AnnotationNode atNode = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
        atNode.values = List.of("value", "HEAD");

        AnnotationNode injectAnn = new AnnotationNode(MixinAnnotationParser.INJECT_DESC);
        injectAnn.values = List.of(
                "method", "tick()V",
                "at", atNode,
                "cancellable", Boolean.TRUE
        );
        injectMethod.visibleAnnotations.add(injectAnn);

        Optional<ParsedMixinAnnotation> parsedInject = parser.parseMethodAnnotation(injectMethod);
        assertThat(parsedInject).isPresent();
        assertThat(parsedInject.get().annotationDescriptor()).isEqualTo(MixinAnnotationParser.INJECT_DESC);
        assertThat(parsedInject.get().targetMethod()).isEqualTo("tick");
        assertThat(parsedInject.get().targetDescriptor()).isEqualTo("()V");
        assertThat(parsedInject.get().injectionPointAt()).isEqualTo("HEAD");
        assertThat(parsedInject.get().cancellable()).isTrue();

        // 2. Construct MethodNode with @WrapOperation
        MethodNode wrapMethod = new MethodNode(Opcodes.ACC_PUBLIC, "wrapRender", "()V", null, null);
        wrapMethod.visibleAnnotations = new ArrayList<>();
        AnnotationNode wrapAnn = new AnnotationNode(MixinAnnotationParser.WRAP_OPERATION_DESC);
        wrapAnn.values = List.of("method", "render", "at", atNode);
        wrapMethod.visibleAnnotations.add(wrapAnn);

        Optional<ParsedMixinAnnotation> parsedWrap = parser.parseMethodAnnotation(wrapMethod);
        assertThat(parsedWrap).isPresent();
        assertThat(parsedWrap.get().annotationDescriptor()).isEqualTo(MixinAnnotationParser.WRAP_OPERATION_DESC);
        assertThat(parsedWrap.get().targetMethod()).isEqualTo("render");
    }

    @Test
    @DisplayName("MixinToSemanticHookConverter normalizes annotations into canonical semantic intents")
    void testMixinToSemanticHookConverter() {
        MixinToSemanticHookConverter converter = new MixinToSemanticHookConverter();

        // @Inject HEAD -> METHOD_PRE_EXECUTION
        ParsedMixinAnnotation injectHead = new ParsedMixinAnnotation(
                MixinAnnotationParser.INJECT_DESC, "tick", "()V", "HEAD", "", -1, false
        );
        VesselSemanticHookRecord headHook = converter.convert("net/minecraft/Server", "com/mod/ServerMixin", "onPreTick", injectHead, 100);
        assertThat(headHook.intent()).isEqualTo(VesselSemanticHookIntent.METHOD_PRE_EXECUTION);
        assertThat(headHook.targetOwner()).isEqualTo("net/minecraft/Server");
        assertThat(headHook.priority()).isEqualTo(100);

        // @Inject RETURN -> METHOD_POST_EXECUTION
        ParsedMixinAnnotation injectReturn = new ParsedMixinAnnotation(
                MixinAnnotationParser.INJECT_DESC, "tick", "()V", "RETURN", "", -1, false
        );
        VesselSemanticHookRecord returnHook = converter.convert("net/minecraft/Server", "com/mod/ServerMixin", "onPostTick", injectReturn, 50);
        assertThat(returnHook.intent()).isEqualTo(VesselSemanticHookIntent.METHOD_POST_EXECUTION);

        // @WrapOperation -> METHOD_OPERATION_WRAPPING
        ParsedMixinAnnotation wrapOp = new ParsedMixinAnnotation(
                MixinAnnotationParser.WRAP_OPERATION_DESC, "render", "()V", "INVOKE", "", -1, false
        );
        VesselSemanticHookRecord wrapHook = converter.convert("net/minecraft/Renderer", "com/mod/RenderMixin", "wrapCall", wrapOp, 200);
        assertThat(wrapHook.intent()).isEqualTo(VesselSemanticHookIntent.METHOD_OPERATION_WRAPPING);

        // @ModifyVariable -> METHOD_INVOCATION_MUTATION
        ParsedMixinAnnotation modVar = new ParsedMixinAnnotation(
                MixinAnnotationParser.MODIFY_VAR_DESC, "damage", "(F)F", "HEAD", "", 0, false
        );
        VesselSemanticHookRecord varHook = converter.convert("net/minecraft/LivingEntity", "com/mod/EntityMixin", "modifyDamage", modVar, 10);
        assertThat(varHook.intent()).isEqualTo(VesselSemanticHookIntent.METHOD_INVOCATION_MUTATION);
    }

    @Test
    @DisplayName("SemanticHookRegistry orders hooks by descending priority")
    void testSemanticHookRegistryOrdering() {
        SemanticHookRegistry registry = new SemanticHookRegistry();

        VesselSemanticHookRecord low = new VesselSemanticHookRecord(
                VesselSemanticHookIntent.METHOD_PRE_EXECUTION, "net/minecraft/World", "tick", "()V", "com/mod/Low", "call", 10
        );
        VesselSemanticHookRecord high = new VesselSemanticHookRecord(
                VesselSemanticHookIntent.METHOD_PRE_EXECUTION, "net/minecraft/World", "tick", "()V", "com/mod/High", "call", 500
        );
        VesselSemanticHookRecord mid = new VesselSemanticHookRecord(
                VesselSemanticHookIntent.METHOD_PRE_EXECUTION, "net/minecraft/World", "tick", "()V", "com/mod/Mid", "call", 100
        );

        registry.register(low);
        registry.register(high);
        registry.register(mid);

        assertThat(registry.hasHooksFor("net.minecraft.World")).isTrue();
        assertThat(registry.hasHooksFor("net/minecraft/World")).isTrue();
        assertThat(registry.hasHooksFor("net/minecraft/Block")).isFalse();

        List<VesselSemanticHookRecord> hooks = registry.getHooksFor("net/minecraft/World");
        assertThat(hooks).containsExactly(high, mid, low); // Ordered 500, 100, 10
        assertThat(registry.getTotalHookCount()).isEqualTo(3);

        registry.clear();
        assertThat(registry.getTotalHookCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("SemanticBytecodeTransformer weaves METHOD_PRE_EXECUTION into ClassNode instructions")
    void testSemanticBytecodeTransformerWeave() {
        SemanticBytecodeTransformer transformer = new SemanticBytecodeTransformer();

        // Create target ClassNode with empty tick() method
        ClassNode targetNode = new ClassNode();
        targetNode.name = "net/minecraft/TestServer";
        MethodNode tickMethod = new MethodNode(Opcodes.ACC_PUBLIC, "tick", "()V", null, null);
        tickMethod.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        targetNode.methods.add(tickMethod);

        VesselSemanticHookRecord hook = new VesselSemanticHookRecord(
                VesselSemanticHookIntent.METHOD_PRE_EXECUTION,
                "net/minecraft/TestServer",
                "tick",
                "()V",
                "com/vessel/hooks/ServerHooks",
                "onPreTick",
                100
        );

        int applied = transformer.transform(targetNode, List.of(hook));
        assertThat(applied).isEqualTo(1);

        // Verify that INVOKESTATIC com/vessel/hooks/ServerHooks.onPreTick was inserted before RETURN
        org.objectweb.asm.tree.AbstractInsnNode firstInsn = tickMethod.instructions.getFirst();
        assertThat(firstInsn).isInstanceOf(MethodInsnNode.class);
        MethodInsnNode injectedCall = (MethodInsnNode) firstInsn;
        assertThat(injectedCall.owner).isEqualTo("com/vessel/hooks/ServerHooks");
        assertThat(injectedCall.name).isEqualTo("onPreTick");
    }

    @Test
    @DisplayName("FastTransformationPipeline coordinates Access Widening, Synthetic Interfaces, and Mixin Hooks")
    void testFastTransformationPipelineUnifiedExecution() {
        BytecodeToIRParser irParser = new BytecodeToIRParser();
        VesselAccessWideningTable wideningTable = new VesselAccessWideningTable();
        VesselAccessWidenerApplier widenerApplier = new VesselAccessWidenerApplier();
        InterfaceInjectionRegistry interfaceRegistry = new InterfaceInjectionRegistry();
        SyntheticInterfaceBytecodeApplier interfaceApplier = new SyntheticInterfaceBytecodeApplier();
        SemanticHookRegistry hookRegistry = new SemanticHookRegistry();
        SemanticBytecodeTransformer hookTransformer = new SemanticBytecodeTransformer();

        FastTransformationPipeline pipeline = new FastTransformationPipeline(
                irParser, wideningTable, widenerApplier, interfaceRegistry, interfaceApplier, hookRegistry, hookTransformer
        );

        // 1. Generate minimal dummy class bytecode: com.example.TargetClass
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_FINAL, "com/example/TargetClass", null, "java/lang/Object", null);
        MethodNode mn = new MethodNode(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        mn.instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.RETURN));
        mn.accept(cw);
        cw.visitEnd();
        byte[] originalBytecode = cw.toByteArray();

        // Fast-path bypass check: unmodified class returns identical array instance
        byte[] bypassed = pipeline.transformBytecode("com.unmodified.Class", originalBytecode);
        assertThat(bypassed).isSameAs(originalBytecode);
        assertThat(pipeline.getTransformedClassCount()).isEqualTo(0);

        // Configure transformations for com.example.TargetClass:
        // a) Access Widener: make class non-final (extendable)
        wideningTable.addClassRule("com/example/TargetClass", VesselAccessLevel.PUBLIC, true);
        // b) Interface: inject AutoCloseable
        interfaceRegistry.register("com/example/TargetClass", "java/lang/AutoCloseable");
        // c) Hook: inject pre-execution call into run()
        hookRegistry.register(new VesselSemanticHookRecord(
                VesselSemanticHookIntent.METHOD_PRE_EXECUTION,
                "com/example/TargetClass",
                "run",
                "()V",
                "com/example/HookHelper",
                "onPreRun",
                100
        ));

        byte[] transformedBytes = pipeline.transformBytecode("com.example.TargetClass", originalBytecode);
        assertThat(transformedBytes).isNotSameAs(originalBytecode);
        assertThat(pipeline.getTransformedClassCount()).isEqualTo(1);
        assertThat(pipeline.getTotalTransformNanos()).isGreaterThan(0L);

        // Verify resulting bytecode structure via IR parser
        ClassNode transformedNode = irParser.parse(transformedBytes);
        assertThat((transformedNode.access & Opcodes.ACC_FINAL)).isEqualTo(0); // final removed by widener
        assertThat(transformedNode.interfaces).contains("java/lang/AutoCloseable"); // interface injected
    }
}
