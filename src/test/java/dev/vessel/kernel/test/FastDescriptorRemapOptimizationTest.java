package dev.vessel.kernel.test;

import dev.vessel.kernel.remap.CompactMappingDictionary;
import dev.vessel.kernel.remap.ZeroCopyBytecodeRemapper;
import dev.vessel.kernel.vstm.builder.VstmTransmutationContext;
import dev.vessel.kernel.vstm.remap.VstmAsmRemapperBridge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification test suite for Phase 6 (VSTM Format, Remapper & Bytecode Cache Pipeline).
 * Tests fast-path primitive descriptor bypass and dual-dialect bridge execution.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class FastDescriptorRemapOptimizationTest {

    @Test
    @DisplayName("ZeroCopyBytecodeRemapper bypasses primitive descriptors via zero-allocation fast path")
    void testPrimitiveDescriptorFastPath() {
        ZeroCopyBytecodeRemapper remapper = new ZeroCopyBytecodeRemapper();
        CompactMappingDictionary dict = new CompactMappingDictionary();
        dict.registerClassMapping("net/minecraft/world/entity/Entity", "net/minecraft/world/entity/MojangEntity");

        // 1. Primitive descriptors must return identical instance without heap allocations
        String prim1 = "()V";
        String prim2 = "(II)I";
        String prim3 = "(ZJDF)V";

        assertThat(remapper.mapMethodDescriptor(prim1, dict)).isSameAs(prim1);
        assertThat(remapper.mapMethodDescriptor(prim2, dict)).isSameAs(prim2);
        assertThat(remapper.mapMethodDescriptor(prim3, dict)).isSameAs(prim3);

        // 2. Reference descriptor must be properly translated
        String refDesc = "(Lnet/minecraft/world/entity/Entity;I)V";
        String remapped = remapper.mapMethodDescriptor(refDesc, dict);
        assertThat(remapped).isEqualTo("(Lnet/minecraft/world/entity/MojangEntity;I)V");
    }

    @Test
    @DisplayName("VstmAsmRemapperBridge bypasses primitive descriptors cleanly")
    void testVstmAsmRemapperBridgePrimitiveBypass() {
        VstmTransmutationContext context = VstmTransmutationContext.empty();
        VstmAsmRemapperBridge bridge = new VstmAsmRemapperBridge(context);

        String primDesc = "(III)Z";
        assertThat(bridge.mapMethodDesc(primDesc)).isSameAs(primDesc);

        String primFieldDesc = "I";
        assertThat(bridge.mapDesc(primFieldDesc)).isSameAs(primFieldDesc);
    }
}
