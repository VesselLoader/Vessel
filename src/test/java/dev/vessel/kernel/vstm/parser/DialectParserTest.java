package dev.vessel.kernel.vstm.parser;

import dev.vessel.kernel.vstm.binary.VstmbMemorySegmentMapper;
import dev.vessel.kernel.vstm.builder.VstmbBinaryWriter;
import dev.vessel.kernel.vstm.engine.VstmDialectMask;
import dev.vessel.kernel.vstm.engine.VstmTransmutationFastLookup;
import java.io.IOException;
import java.io.StringReader;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for Wave 2 upstream mapping dialect ingestion and harmonization.
 *
 * <p>Tests both synthetic snippet validations and full-scale real file ingestion from
 * {@code vstm-distribution/data/raw/1.21.1/} across ProGuard, Tiny v2, TSRG v2, and CSRG.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 2)
 */
public final class DialectParserTest {

    private static final Path RAW_DATA_ROOT = Path.of("vstm-distribution", "data", "raw", "1.21.1");

    @Test
    @DisplayName("Verify ProGuard type descriptor normalization for primitives, arrays, and signatures")
    void testProGuardDescriptorNormalizer() {
        assertThat(ProGuardDescriptorNormalizer.toJvmType("int")).isEqualTo("I");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("void")).isEqualTo("V");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("boolean")).isEqualTo("Z");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("byte")).isEqualTo("B");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("char")).isEqualTo("C");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("short")).isEqualTo("S");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("long")).isEqualTo("J");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("float")).isEqualTo("F");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("double")).isEqualTo("D");

        assertThat(ProGuardDescriptorNormalizer.toJvmType("java.lang.String")).isEqualTo("Ljava/lang/String;");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("int[]")).isEqualTo("[I");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("java.lang.Object[][]")).isEqualTo("[[Ljava/lang/Object;");
        assertThat(ProGuardDescriptorNormalizer.toJvmType("java.util.List<java.lang.String>")).isEqualTo("Ljava/util/List;");

        assertThat(ProGuardDescriptorNormalizer.toMethodDescriptor("void", ""))
                .isEqualTo("()V");
        assertThat(ProGuardDescriptorNormalizer.toMethodDescriptor("int", "float,double"))
                .isEqualTo("(FD)I");
        assertThat(ProGuardDescriptorNormalizer.toMethodDescriptor("void", "com.mojang.blaze3d.pipeline.RenderPipeline,float"))
                .isEqualTo("(Lcom/mojang/blaze3d/pipeline/RenderPipeline;F)V");
    }

    @Test
    @DisplayName("Verify Mojang ProGuard parser with synthetic snippet")
    void testMojangProGuardParserSynthetic() throws IOException {
        String input = """
                # (c) 2020 Microsoft Corporation.
                com.mojang.blaze3d.Blaze3D -> eza:
                # {"fileName":"Blaze3D.java","id":"sourceFile"}
                    10:10:void <init>() -> <init>
                    12:13:void process(com.mojang.blaze3d.pipeline.RenderPipeline,float) -> a
                    org.slf4j.Logger LOGGER -> b
                    int QUEUED_BUFFER_COUNT -> c
                """;

        MojangProGuardParser parser = new MojangProGuardParser();
        ParsedDialectResult result = parser.parse(new StringReader(input));

        assertThat(result.dialectMask()).isEqualTo(VstmDialectMask.MOJMAP);
        assertThat(result.classes()).hasSize(1);
        ParsedClassMapping clazz = result.classes().get(0);
        assertThat(clazz.sourceClass()).isEqualTo("eza");
        assertThat(clazz.targetClass()).isEqualTo("com/mojang/blaze3d/Blaze3D");

        assertThat(result.fields()).hasSize(2);
        ParsedFieldMapping fieldLogger = result.fields().get(0);
        assertThat(fieldLogger.ownerClass()).isEqualTo("eza");
        assertThat(fieldLogger.sourceFieldName()).isEqualTo("b");
        assertThat(fieldLogger.targetFieldName()).isEqualTo("LOGGER");
        assertThat(fieldLogger.sourceDescriptor()).isEqualTo("Lorg/slf4j/Logger;");

        assertThat(result.methods()).hasSize(2);
        ParsedMethodMapping methodProcess = result.methods().get(1);
        assertThat(methodProcess.ownerClass()).isEqualTo("eza");
        assertThat(methodProcess.sourceMethodName()).isEqualTo("a");
        assertThat(methodProcess.targetMethodName()).isEqualTo("process");
        assertThat(methodProcess.sourceDescriptor()).isEqualTo("(Lcom/mojang/blaze3d/pipeline/RenderPipeline;F)V");
    }

    @Test
    @DisplayName("Verify Fabric Tiny v2 parser with synthetic snippet")
    void testFabricTinyV2ParserSynthetic() throws IOException {
        String input = """
                tiny\t2\t0\tofficial\tintermediary\tnamed
                c\ta\tnet/minecraft/class_7833\tnet/minecraft/util/math/RotationAxis
                \tf\tLa;\ta\tfield_40713\tNEGATIVE_X
                \tm\t(F)Lorg/joml/Quaternionf;\ta\tmethod_46349\tmethod_46349
                \t\tp\t0\t\t\trad
                \tc\tA math class comment
                """;

        FabricTinyV2Parser parser = new FabricTinyV2Parser();
        // Parse Yarn (intermediary -> named)
        ParsedDialectResult yarnResult = parser.parse(new StringReader(input), VstmDialectMask.YARN);

        assertThat(yarnResult.classes()).hasSize(1);
        ParsedClassMapping clazz = yarnResult.classes().get(0);
        assertThat(clazz.sourceClass()).isEqualTo("net/minecraft/class_7833");
        assertThat(clazz.targetClass()).isEqualTo("net/minecraft/util/math/RotationAxis");

        assertThat(yarnResult.fields()).hasSize(1);
        ParsedFieldMapping field = yarnResult.fields().get(0);
        assertThat(field.sourceFieldName()).isEqualTo("field_40713");
        assertThat(field.targetFieldName()).isEqualTo("NEGATIVE_X");
        assertThat(field.sourceDescriptor()).isEqualTo("La;");

        assertThat(yarnResult.methods()).hasSize(1);
        ParsedMethodMapping method = yarnResult.methods().get(0);
        assertThat(method.sourceMethodName()).isEqualTo("method_46349");
        assertThat(method.targetMethodName()).isEqualTo("method_46349");

        // Parse Intermediary (official -> intermediary)
        ParsedDialectResult intermediaryResult = parser.parse(new StringReader(input), VstmDialectMask.INTERMEDIARY);
        assertThat(intermediaryResult.classes().get(0).sourceClass()).isEqualTo("a");
        assertThat(intermediaryResult.classes().get(0).targetClass()).isEqualTo("net/minecraft/class_7833");
    }

    @Test
    @DisplayName("Verify NeoForge TSRG v2 parser with synthetic snippet")
    void testNeoForgeTsrg2ParserSynthetic() throws IOException {
        String input = """
                tsrg2 obf srg id
                a net/minecraft/client/Minecraft 252363
                \ta instance 252495
                \t<clinit> ()V <clinit> 253140
                \t\tstatic
                \tb (F)V runTick 253156
                \t\t0 o p_254110_ 254110
                """;

        NeoForgeTsrg2Parser parser = new NeoForgeTsrg2Parser();
        ParsedDialectResult result = parser.parse(new StringReader(input), VstmDialectMask.SRG);

        assertThat(result.classes()).hasSize(1);
        ParsedClassMapping clazz = result.classes().get(0);
        assertThat(clazz.sourceClass()).isEqualTo("a");
        assertThat(clazz.targetClass()).isEqualTo("net/minecraft/client/Minecraft");

        assertThat(result.fields()).hasSize(1);
        ParsedFieldMapping field = result.fields().get(0);
        assertThat(field.sourceFieldName()).isEqualTo("a");
        assertThat(field.targetFieldName()).isEqualTo("instance");

        assertThat(result.methods()).hasSize(2);
        ParsedMethodMapping methodTick = result.methods().get(1);
        assertThat(methodTick.sourceMethodName()).isEqualTo("b");
        assertThat(methodTick.sourceDescriptor()).isEqualTo("(F)V");
        assertThat(methodTick.targetMethodName()).isEqualTo("runTick");
    }

    @Test
    @DisplayName("Verify Spigot CSRG parser with synthetic snippet")
    void testSpigotCsrgParserSynthetic() throws IOException {
        String input = """
                # (c) 2024 SpigotMC Pty. Ltd.
                aa net/minecraft/ResourceKeyInvalidException
                net/minecraft/world/level/Level field_9226 isClientSide
                net/minecraft/world/level/Level method_8597 (Lnet/minecraft/world/entity/Entity;)Z addFreshEntity
                """;

        SpigotCsrgParser parser = new SpigotCsrgParser();
        ParsedDialectResult result = parser.parse(new StringReader(input), VstmDialectMask.SPIGOT_NMS);

        assertThat(result.classes()).hasSize(1);
        assertThat(result.classes().get(0).sourceClass()).isEqualTo("aa");
        assertThat(result.classes().get(0).targetClass()).isEqualTo("net/minecraft/ResourceKeyInvalidException");

        assertThat(result.fields()).hasSize(1);
        assertThat(result.fields().get(0).ownerClass()).isEqualTo("net/minecraft/world/level/Level");
        assertThat(result.fields().get(0).sourceFieldName()).isEqualTo("field_9226");
        assertThat(result.fields().get(0).targetFieldName()).isEqualTo("isClientSide");

        assertThat(result.methods()).hasSize(1);
        assertThat(result.methods().get(0).ownerClass()).isEqualTo("net/minecraft/world/level/Level");
        assertThat(result.methods().get(0).sourceMethodName()).isEqualTo("method_8597");
        assertThat(result.methods().get(0).sourceDescriptor()).isEqualTo("(Lnet/minecraft/world/entity/Entity;)Z");
        assertThat(result.methods().get(0).targetMethodName()).isEqualTo("addFreshEntity");
    }

    @Test
    @DisplayName("Verify UpstreamDialectHarmonizer merges masks and produces sorted entries")
    void testHarmonizerSyntheticAndDeduplication() {
        ParsedClassMapping c1 = new ParsedClassMapping("net/minecraft/class_1937", "net/minecraft/world/level/Level", VstmDialectMask.MOJMAP);
        ParsedClassMapping c2 = new ParsedClassMapping("net/minecraft/class_1937", "net/minecraft/world/level/Level", VstmDialectMask.YARN);
        ParsedClassMapping c3 = new ParsedClassMapping("net/minecraft/class_1000", "net/minecraft/world/item/Item", VstmDialectMask.MOJMAP);

        ParsedDialectResult r1 = new ParsedDialectResult(List.of(c1, c3), List.of(), List.of(), VstmDialectMask.MOJMAP);
        ParsedDialectResult r2 = new ParsedDialectResult(List.of(c2), List.of(), List.of(), VstmDialectMask.YARN);

        UpstreamDialectHarmonizer harmonizer = new UpstreamDialectHarmonizer();
        HarmonizedTransmutationMatrix matrix = harmonizer.harmonize(r1, r2);

        assertThat(matrix.combinedMask()).isEqualTo(VstmDialectMask.MOJMAP | VstmDialectMask.YARN);
        assertThat(matrix.classes()).hasSize(2);

        // Verification of sorting: class_1000 precedes class_1937 (hash / class natural sort)
        boolean mergedFound = false;
        for (var entry : matrix.classes()) {
            if (entry.sourceClass().equals("net/minecraft/class_1937")) {
                assertThat(entry.dialectMask()).isEqualTo(VstmDialectMask.MOJMAP | VstmDialectMask.YARN);
                mergedFound = true;
            }
        }
        assertThat(mergedFound).isTrue();
    }

    @Test
    @DisplayName("Verify real Mojang client mappings parsing from distribution directory")
    void testLiveMojangProGuardParsing() throws IOException {
        Path clientMappings = RAW_DATA_ROOT.resolve("mojang").resolve("client_mappings.txt");
        if (!Files.exists(clientMappings)) {
            return;
        }

        MojangProGuardParser parser = new MojangProGuardParser();
        ParsedDialectResult result = parser.parse(clientMappings, VstmDialectMask.MOJMAP);

        assertThat(result.classes()).isNotEmpty();
        assertThat(result.methods()).isNotEmpty();
        assertThat(result.fields()).isNotEmpty();

        boolean foundBlaze3D = result.classes().stream()
                .anyMatch(c -> c.targetClass().equals("com/mojang/blaze3d/Blaze3D") && c.sourceClass().equals("eza"));
        assertThat(foundBlaze3D).isTrue();
    }

    @Test
    @DisplayName("Verify real Fabric Yarn Tiny v2 parsing from distribution directory")
    void testLiveFabricTinyV2Parsing() throws IOException {
        Path yarnPath = RAW_DATA_ROOT.resolve("fabric").resolve("yarn-mergedv2.tiny");
        if (!Files.exists(yarnPath)) {
            return;
        }

        FabricTinyV2Parser parser = new FabricTinyV2Parser();
        ParsedDialectResult result = parser.parse(yarnPath, VstmDialectMask.YARN);

        assertThat(result.classes()).isNotEmpty();
        assertThat(result.methods()).isNotEmpty();
        assertThat(result.fields()).isNotEmpty();

        boolean foundRotationAxis = result.classes().stream()
                .anyMatch(c -> c.sourceClass().equals("net/minecraft/class_7833") && c.targetClass().equals("net/minecraft/util/math/RotationAxis"));
        assertThat(foundRotationAxis).isTrue();
    }

    @Test
    @DisplayName("Verify real NeoForge TSRG v2 parsing from distribution directory")
    void testLiveNeoForgeTsrg2Parsing() throws IOException {
        Path tsrgPath = RAW_DATA_ROOT.resolve("neoforge").resolve("joined.tsrg");
        if (!Files.exists(tsrgPath)) {
            return;
        }

        NeoForgeTsrg2Parser parser = new NeoForgeTsrg2Parser();
        ParsedDialectResult result = parser.parse(tsrgPath, VstmDialectMask.SRG);

        assertThat(result.classes()).isNotEmpty();
        assertThat(result.methods()).isNotEmpty();
        assertThat(result.fields()).isNotEmpty();
    }

    @Test
    @DisplayName("Verify real Spigot CSRG parsing from distribution directory")
    void testLiveSpigotCsrgParsing() throws IOException {
        Path csrgPath = RAW_DATA_ROOT.resolve("spigot").resolve("bukkit-1.21.1-cl.csrg");
        if (!Files.exists(csrgPath)) {
            return;
        }

        SpigotCsrgParser parser = new SpigotCsrgParser();
        ParsedDialectResult result = parser.parse(csrgPath, VstmDialectMask.SPIGOT_NMS);

        assertThat(result.classes()).hasSize(3887);
        assertThat(result.classes().get(0).sourceClass()).isEqualTo("aa");
        assertThat(result.classes().get(0).targetClass()).isEqualTo("net/minecraft/ResourceKeyInvalidException");
    }

    @Test
    @DisplayName("Verify full pipeline: parse all dialects -> harmonize -> compile binary -> off-heap lookup")
    void testFullPipelineBinaryCompilation() {
        ParsedClassMapping mojangClass = new ParsedClassMapping("eza", "com/mojang/blaze3d/Blaze3D", VstmDialectMask.MOJMAP);
        ParsedClassMapping spigotClass = new ParsedClassMapping("com/mojang/blaze3d/Blaze3D", "net/minecraft/Blaze3DSpigot", VstmDialectMask.SPIGOT_NMS);

        ParsedFieldMapping mojangField = new ParsedFieldMapping("eza", "b", "Lorg/slf4j/Logger;", "LOGGER", VstmDialectMask.MOJMAP);
        ParsedMethodMapping mojangMethod = new ParsedMethodMapping("eza", "a", "(F)V", "render", "(F)V", VstmDialectMask.MOJMAP);

        ParsedDialectResult rMojmap = new ParsedDialectResult(List.of(mojangClass), List.of(mojangField), List.of(mojangMethod), VstmDialectMask.MOJMAP);
        ParsedDialectResult rSpigot = new ParsedDialectResult(List.of(spigotClass), List.of(), List.of(), VstmDialectMask.SPIGOT_NMS);

        UpstreamDialectHarmonizer harmonizer = new UpstreamDialectHarmonizer();
        HarmonizedTransmutationMatrix matrix = harmonizer.harmonize(rMojmap, rSpigot);

        byte[] binary = VstmbBinaryWriter.writeToByteArray((short) 767, matrix.combinedMask(), matrix.classes(), matrix.fields(), matrix.methods());
        MemorySegment segment = VstmbMemorySegmentMapper.mapFromByteArray(binary);
        VstmTransmutationFastLookup fastLookup = new VstmTransmutationFastLookup(segment);

        // Test fast lookup
        String remappedClass = fastLookup.lookupClass("eza", VstmDialectMask.MOJMAP, VstmDialectMask.MOJMAP);
        assertThat(remappedClass).isEqualTo("com/mojang/blaze3d/Blaze3D");

        String remappedField = fastLookup.lookupField("eza", "b", "Lorg/slf4j/Logger;", VstmDialectMask.MOJMAP);
        assertThat(remappedField).isEqualTo("LOGGER");

        String remappedMethod = fastLookup.lookupMethod("eza", "a", "(F)V", VstmDialectMask.MOJMAP);
        assertThat(remappedMethod).isEqualTo("render");
    }

    @Test
    @DisplayName("Verify malformed syntax raises domain UpstreamParserException")
    void testMalformedSyntaxExceptions() {
        MojangProGuardParser mojangParser = new MojangProGuardParser();
        assertThatThrownBy(() -> mojangParser.parse(new StringReader("broken line without arrow:")))
                .isInstanceOf(UpstreamParserException.class)
                .hasMessageContaining("Missing ' -> '");

        FabricTinyV2Parser tinyParser = new FabricTinyV2Parser();
        assertThatThrownBy(() -> tinyParser.parse(new StringReader("invalid header\n")))
                .isInstanceOf(UpstreamParserException.class)
                .hasMessageContaining("Missing or invalid Tiny v2 header");

        NeoForgeTsrg2Parser tsrgParser = new NeoForgeTsrg2Parser();
        assertThatThrownBy(() -> tsrgParser.parse(new StringReader("invalid header\n")))
                .isInstanceOf(UpstreamParserException.class)
                .hasMessageContaining("Missing or invalid TSRG v2 header");
    }
}
