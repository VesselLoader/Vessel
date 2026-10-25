package dev.vessel.kernel.identity;

import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite for {@link ModuleSourceClassifier}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class ModuleSourceClassifierTest {

    private final ModuleSourceClassifier classifier = new ModuleSourceClassifier();

    @Test
    @DisplayName("Classifies Fabric module bytecode implementing ModInitializer")
    void testClassifyFabricBytecode() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/fabric/FabricExample", null,
                "java/lang/Object", new String[]{"net/fabricmc/api/ModInitializer"});
        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        ModuleSourceClassificationResult result = classifier.classifyDetailed(classBytes);
        assertThat(result.detectedKind()).isEqualTo(SourceLoaderKind.FABRIC);
        assertThat(result.confidenceScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Classifies NeoForge module bytecode with @Mod annotation")
    void testClassifyNeoForgeBytecode() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/neoforge/NeoExample", null,
                "java/lang/Object", null);
        AnnotationVisitor av = cw.visitAnnotation("Lnet/neoforged/fml/common/Mod;", true);
        av.visitEnd();
        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        ModuleSourceClassificationResult result = classifier.classifyDetailed(classBytes);
        assertThat(result.detectedKind()).isEqualTo(SourceLoaderKind.NEOFORGE);
        assertThat(result.confidenceScore()).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Classifies Forge module bytecode with @Mod annotation")
    void testClassifyForgeBytecode() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/forge/ForgeExample", null,
                "java/lang/Object", null);
        AnnotationVisitor av = cw.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", true);
        av.visitEnd();
        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        ModuleSourceClassificationResult result = classifier.classifyDetailed(classBytes);
        assertThat(result.detectedKind()).isEqualTo(SourceLoaderKind.FORGE);
        assertThat(result.confidenceScore()).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Classifies Bukkit plugin bytecode extending JavaPlugin")
    void testClassifyBukkitBytecode() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/bukkit/PaperExample", null,
                "org/bukkit/plugin/java/JavaPlugin", null);
        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        SourceLoaderKind kind = classifier.classify(classBytes);
        assertThat(kind).isEqualTo(SourceLoaderKind.BUKKIT);
    }

    @Test
    @DisplayName("Classifies Quilt module referencing Quilt loader API")
    void testClassifyQuiltBytecode() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "com/example/quilt/QuiltExample", null,
                "java/lang/Object", new String[]{"org/quiltmc/loader/api/entrypoint/PreLaunchEntrypoint"});
        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        SourceLoaderKind kind = classifier.classify(classBytes);
        assertThat(kind).isEqualTo(SourceLoaderKind.QUILT);
    }

    @Test
    @DisplayName("Classifies from pre-scanned ScannedClassSymbols")
    void testClassifyScannedSymbols() {
        ScannedClassSymbols fabricSymbols = new ScannedClassSymbols(
                "com/example/TestMod",
                "java/lang/Object",
                List.of("net/fabricmc/api/ModInitializer"),
                Set.of(),
                List.of(),
                List.of(),
                false,
                false
        );

        SourceLoaderKind kind = classifier.classify(fabricSymbols);
        assertThat(kind).isEqualTo(SourceLoaderKind.FABRIC);
    }

    @Test
    @DisplayName("Cached loader kinds are non-empty and unmutated")
    void testCachedLoaderKinds() {
        SourceLoaderKind[] cached = ModuleSourceClassifier.cachedLoaderKinds();
        assertThat(cached).isNotEmpty();
        assertThat(cached).contains(SourceLoaderKind.FABRIC, SourceLoaderKind.NEOFORGE, SourceLoaderKind.BUKKIT);
    }
}
