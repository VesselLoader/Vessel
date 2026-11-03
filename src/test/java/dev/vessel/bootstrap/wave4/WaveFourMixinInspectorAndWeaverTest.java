package dev.vessel.bootstrap.wave4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.mixin.weaver.AccessorAndShadowBytecodeSynthesizer;
import dev.vessel.kernel.mixin.weaver.InspectedMixinSummary;
import dev.vessel.kernel.mixin.weaver.MixinClassMetadataInspector;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Integration tests for Wave 4 verifying {@link MixinClassMetadataInspector},
 * {@link InspectedMixinSummary}, and {@link AccessorAndShadowBytecodeSynthesizer} bytecode
 * access widening for {@code @Shadow} / {@code @Accessor} targets.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveFourMixinInspectorAndWeaverTest {

    @Test
    void testMixinClassMetadataInspectorAndAccessorShadowAccessWidener() {
        MixinClassMetadataInspector inspector = new MixinClassMetadataInspector();
        InspectedMixinSummary summary =
                inspector.inspectLoadedMixinClass(SampleMixinAccessorAndInjectFixture.class);

        assertNotNull(summary);
        assertTrue(summary.isMixinClass());
        assertTrue(summary.getTargetClassNames().contains("net.minecraft.world.level.Level"));
        assertEquals(1100, summary.getPriority());
        assertTrue(summary.getShadowFieldNames().contains("isClientSide"));
        assertTrue(summary.getAccessorMethodNames().contains("vessel$getIsClientSide"));
        assertTrue(summary.getInvokerMethodNames().contains("vessel$invokeTickBlockEntities"));
        assertTrue(summary.getInjectMethodNames().contains("onSetBlock"));
        assertTrue(summary.getInjectMethodNames().contains("modifyDayTime"));
        assertTrue(summary.getInjectMethodNames().contains("wrapTick"));

        byte[] rawTargetBytes = synthesizePrivateFieldTargetClass();
        AccessorAndShadowBytecodeSynthesizer synthesizer =
                new AccessorAndShadowBytecodeSynthesizer();
        byte[] widenedBytes = synthesizer.widenTargetFieldsAndMethodsToPublic(
                rawTargetBytes,
                Set.of("secretCounter"));

        ClassNode widenedNode = new ClassNode();
        new ClassReader(widenedBytes).accept(widenedNode, 0);

        FieldNode secretCounterField = null;
        for (FieldNode field : widenedNode.fields) {
            if ("secretCounter".equals(field.name)) {
                secretCounterField = field;
                break;
            }
        }
        assertNotNull(secretCounterField);
        assertTrue((secretCounterField.access & Opcodes.ACC_PUBLIC) != 0);
        assertFalse((secretCounterField.access & Opcodes.ACC_PRIVATE) != 0);
    }

    private static byte[] synthesizePrivateFieldTargetClass() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "dev/vessel/bootstrap/wave4/SyntheticPrivateFieldTarget",
                null,
                "java/lang/Object",
                null);
        FieldVisitor fv = cw.visitField(
                Opcodes.ACC_PRIVATE,
                "secretCounter",
                "I",
                null,
                null);
        fv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
