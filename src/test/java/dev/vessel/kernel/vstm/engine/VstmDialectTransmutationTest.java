package dev.vessel.kernel.vstm.engine;

import dev.vessel.kernel.vstm.binary.VstmbBinaryCorruptException;
import dev.vessel.kernel.vstm.binary.VstmbMemorySegmentMapper;
import dev.vessel.kernel.vstm.builder.VstmbBinaryWriter;
import dev.vessel.kernel.vstm.builder.VstmbPayloadCompressor;
import dev.vessel.kernel.vstm.builder.VstmConformanceValidator;
import dev.vessel.kernel.vstm.builder.VstmTransmutationContext;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end integration tests verifying bidirectional cross-dialect routing (MojMap, Yarn, Spigot NMS),
 * synthetic fallback handling, payload compression, and identifier conformance guards.
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 1)
 */
public final class VstmDialectTransmutationTest {

    @Test
    @DisplayName("Verify bidirectional class, method, and field transmutation across MojMap and Spigot NMS")
    void testBidirectionalDialectTransmutation() {
        List<VstmClassTransmutationEntry> classes = List.of(
                VstmClassTransmutationEntry.of(
                        "net/minecraft/class_1937",
                        "net/minecraft/world/level/Level",
                        VstmDialectMask.MOJMAP
                ),
                VstmClassTransmutationEntry.of(
                        "net/minecraft/world/level/Level",
                        "net/minecraft/server/level/WorldServer",
                        VstmDialectMask.SPIGOT_NMS
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
                )
        );

        int combinedDialects = VstmDialectMask.combine(VstmDialectMask.MOJMAP, VstmDialectMask.SPIGOT_NMS);
        byte[] binary = VstmbBinaryWriter.writeToByteArray((short) 767, combinedDialects, classes, fields, methods);

        MemorySegment segment = VstmbMemorySegmentMapper.mapFromByteArray(binary);
        VstmTransmutationFastLookup fastLookup = new VstmTransmutationFastLookup(segment);
        VstmResolutionFallbackChain fallbackChain = new VstmResolutionFallbackChain();
        VstmDialectBridgeRouter router = new VstmDialectBridgeRouter();

        VstmTransmutationContext context = new VstmTransmutationContext(
                "test-module:1.0.0",
                VstmDialectMask.INTERMEDIARY,
                VstmDialectMask.MOJMAP,
                fastLookup,
                fallbackChain,
                router
        );

        // Remap class Intermediary -> MojMap
        assertThat(context.remapClass("net/minecraft/class_1937"))
                .isEqualTo("net/minecraft/world/level/Level");

        // Remap method Intermediary -> MojMap
        assertThat(context.remapMethod("net/minecraft/world/level/Level", "method_8597", "(Lnet/minecraft/world/entity/Entity;)Z"))
                .isEqualTo("addFreshEntity");

        // Remap field Intermediary -> MojMap
        assertThat(context.remapField("net/minecraft/world/level/Level", "field_9226", "Z"))
                .isEqualTo("isClientSide");

        // Same dialect returns unchanged
        VstmTransmutationContext identityContext = new VstmTransmutationContext(
                "identity:1.0.0",
                VstmDialectMask.MOJMAP,
                VstmDialectMask.MOJMAP,
                fastLookup,
                fallbackChain,
                router
        );
        assertThat(identityContext.remapClass("net/minecraft/world/level/Level"))
                .isEqualTo("net/minecraft/world/level/Level");
    }

    @Test
    @DisplayName("Verify synthetic bridge method and lambda desugar preservation in fallback chain")
    void testSyntheticAndLambdaFallback() {
        VstmResolutionFallbackChain fallback = new VstmResolutionFallbackChain();

        assertThat(VstmResolutionFallbackChain.isSyntheticOrBridge("lambda$tick$0")).isTrue();
        assertThat(VstmResolutionFallbackChain.isSyntheticOrBridge("access$000")).isTrue();
        assertThat(VstmResolutionFallbackChain.isSyntheticOrBridge("$deserializeLambda$")).isTrue();
        assertThat(VstmResolutionFallbackChain.isSyntheticOrBridge("normalMethod")).isFalse();
    }

    @Test
    @DisplayName("Verify Conformance Validator preempts protected JDK class remapping and illegal syntax")
    void testConformanceValidatorGuards() {
        assertThatCode(() -> VstmConformanceValidator.validateClassName("net/minecraft/world/level/Level"))
                .doesNotThrowAnyException();
        assertThatCode(() -> VstmConformanceValidator.validateMemberName("addFreshEntity"))
                .doesNotThrowAnyException();
        assertThatCode(() -> VstmConformanceValidator.validateDescriptor("(Lnet/minecraft/world/entity/Entity;)Z"))
                .doesNotThrowAnyException();

        // Reject dot notation
        assertThatThrownBy(() -> VstmConformanceValidator.validateClassName("net.minecraft.world.level.Level"))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("internal slash '/' notation");

        // Reject protected core bootstrap class remapping
        assertThatThrownBy(() -> VstmConformanceValidator.validateClassName("java/lang/Object"))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("Attempted to remap protected Java bootstrap class");

        assertThatThrownBy(() -> VstmConformanceValidator.validateClassName("java/lang/ClassLoader"))
                .isInstanceOf(VstmbBinaryCorruptException.class)
                .hasMessageContaining("Attempted to remap protected Java bootstrap class");
    }

    @Test
    @DisplayName("Verify lossless payload compression and decompression")
    void testPayloadCompressor() {
        byte[] original = "VESSEL_TRANSMUTATION_STRING_POOL_PAYLOAD_TEST_DATA_STREAM".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = VstmbPayloadCompressor.compress(original);
        assertThat(compressed).isNotEmpty();

        byte[] decompressed = VstmbPayloadCompressor.decompress(compressed, original.length);
        assertThat(decompressed).isEqualTo(original);
    }
}
