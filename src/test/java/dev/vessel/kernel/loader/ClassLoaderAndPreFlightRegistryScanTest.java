package dev.vessel.kernel.loader;

import dev.vessel.kernel.bootstrap.ScannedClassSymbols;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.ir.VesselSemanticHookIntent;
import dev.vessel.kernel.ir.VesselSemanticHookRecord;
import dev.vessel.kernel.registry.LegacyEraMetadataAndMissingMappingBridge;
import dev.vessel.kernel.registry.PreFlightRegistryCollisionScanner;
import dev.vessel.kernel.registry.TransactionContextSimulator;
import dev.vessel.kernel.registry.TransferCapabilityBridge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive verification suite for Sub-Stage 6.2 ({@code dev.vessel.kernel.loader}
 * and {@code dev.vessel.kernel.registry}).
 *
 * <p>Uses pure in-memory ASM bytecode synthesis and JUnit temporary directories with zero external downloads.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-28)
 */
class ClassLoaderAndPreFlightRegistryScanTest {

    @Test
    void shouldIsolateEcosystemDomainApiClassesAndResolveSplitPackages() throws ClassNotFoundException {
        EcosystemDomainClassLoader fabricDomainLoader = new EcosystemDomainClassLoader(SourceLoaderKind.FABRIC);
        assertThat(fabricDomainLoader.getEcosystemDomain()).isEqualTo(SourceLoaderKind.FABRIC);
        fabricDomainLoader.registerAllowedApiPackagePrefix("net.fabricmc.loader.api.");

        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
                "net/fabricmc/loader/api/SyntheticModContainer",
                null,
                "java/lang/Object",
                null
        );
        cw.visitEnd();
        byte[] apiBytes = cw.toByteArray();

        fabricDomainLoader.registerInMemoryApiClass("net.fabricmc.loader.api.SyntheticModContainer", apiBytes);
        Class<?> loaded = fabricDomainLoader.loadEcosystemApiClass(
                "net.fabricmc.loader.api.SyntheticModContainer",
                true
        );
        assertThat(loaded.getName()).isEqualTo("net.fabricmc.loader.api.SyntheticModContainer");

        assertThatThrownBy(() -> fabricDomainLoader.loadEcosystemApiClass(
                "net.minecraftforge.fml.ModLoader",
                false
        )).isInstanceOf(ClassNotFoundException.class);

        SplitPackageResolver splitResolver = new SplitPackageResolver();
        VesselModuleIdentifier canonicalOwner = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "create", "0.5.1");
        VesselModuleIdentifier secondaryMod = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "steamrails", "1.6.0");

        splitResolver.registerPackageOwner("com.jozufozu.flywheel.api", canonicalOwner);
        splitResolver.registerCanonicalClassBytes("com.jozufozu.flywheel.api.FlywheelApi", apiBytes);

        assertThat(splitResolver.resolveCanonicalPackageOwner("com/jozufozu/flywheel/api"))
                .isEqualTo(canonicalOwner);
        assertThat(splitResolver.redirectSplitPackageClassLoad(
                "com.jozufozu.flywheel.api.FlywheelApi",
                secondaryMod
        )).isEqualTo(apiBytes);
    }

    @Test
    void shouldWeaveAndInvokeOperationWrappingChain() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "net/minecraft/world/entity/LivingEntity", null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "tick", "()V", null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
        mv.visitInsn(Opcodes.POP2);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cw.visitEnd();

        OperationWrappingAdapter adapter = new OperationWrappingAdapter();
        VesselSemanticHookRecord hook1 = new VesselSemanticHookRecord(
                VesselSemanticHookIntent.METHOD_OPERATION_WRAPPING,
                "net/minecraft/world/entity/LivingEntity",
                "tick",
                "()V",
                "com/example/HookA",
                "wrapTickA",
                100
        );
        byte[] woven = adapter.applyWrapOperationChain(cw.toByteArray(), List.of(hook1));

        ClassNode node = new ClassNode();
        new ClassReader(woven).accept(node, 0);
        assertThat(node.methods).hasSize(1);

        adapter.registerChainStep((instance, args) -> ((int) args[0]) * 2);
        Object result = adapter.invokeWrappedOperationChain(this, new Object[]{21}, 0);
        assertThat(result).isEqualTo(42);
    }

    @Test
    void shouldScanPreFlightRegistriesAndTriggerHardStopOnCollision() {
        PreFlightRegistryCollisionScanner scanner = new PreFlightRegistryCollisionScanner();
        VesselModuleIdentifier modA = VesselModuleIdentifier.of(SourceLoaderKind.FORGE, "mod_alpha", "1.0.0");
        VesselModuleIdentifier modB = VesselModuleIdentifier.of(SourceLoaderKind.NEOFORGE, "mod_beta", "2.0.0");

        scanner.registerModuleJarPath(modA, "mods/mod_alpha-1.0.0.jar");
        scanner.registerModuleJarPath(modB, "mods/mod_beta-2.0.0.jar");

        ScannedClassSymbols deferredSymbols = new ScannedClassSymbols(
                "com/alpha/ModAlpha",
                "java/lang/Object",
                List.of(),
                Set.of("Lnet/minecraftforge/registries/DeferredRegister;"),
                List.of("minecraft:block#shared:titanium_ore"),
                List.of(),
                false,
                false
        );

        assertThat(scanner.requiresIsolatedDryRun(deferredSymbols)).isTrue();
        scanner.recordStaticRegistryKeys(modA, deferredSymbols);
        assertThat(scanner.verifyZeroDuplicateKeysOrThrow()).isEmpty();

        Set<String> dryRunKeys = scanner.executeIsolatedRegistrationDryRun(modB, () -> {
            PreFlightRegistryCollisionScanner.recordDryRunKey("minecraft:block#shared:titanium_ore");
        });
        assertThat(dryRunKeys).containsExactly("minecraft:block#shared:titanium_ore");

        assertThatThrownBy(scanner::verifyZeroDuplicateKeysOrThrow)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("\u001B[31m")
                .hasMessageContaining("mods/mod_alpha-1.0.0.jar")
                .hasMessageContaining("mods/mod_beta-2.0.0.jar");
    }

    @Test
    void shouldCacheMojangMappingsTranslateLegacyMetadataAndResolveMissingMappings(@TempDir Path tempDir) throws Exception {
        LegacyEraMetadataAndMissingMappingBridge bridge = new LegacyEraMetadataAndMissingMappingBridge(tempDir);

        Path cachedPath = bridge.ensureMojangMappingsCached(
                "1.21.1",
                "https://piston-data.mojang.com/v1/objects/server_mappings.txt",
                "AUTO"
        );
        assertThat(Files.exists(cachedPath)).isTrue();

        assertThat(bridge.translatePre18VanillaMetadata("minecraft:stone", 1, "corroborated"))
                .isEqualTo("minecraft:granite");
        assertThat(bridge.translatePre18VanillaMetadata("minecraft:wool", 14, "single_source"))
                .contains("[WARN:SINGLE_SOURCE]")
                .contains("minecraft:red_wool");
        assertThat(bridge.translatePre18VanillaMetadata("minecraft:stone", 9, "unverified"))
                .isEqualTo("minecraft:stone[opaque_meta=9]");

        bridge.registerMissingMappingRemapTarget("oldmod:copper_ingot", "minecraft:copper_ingot");
        assertThat(bridge.resolveMissingMappingAction("minecraft:item", "oldmod:copper_ingot", "REMAP"))
                .isEqualTo("REMAPPED:oldmod:copper_ingot->minecraft:copper_ingot");
        assertThat(bridge.resolveMissingMappingAction("minecraft:item", "oldmod:tin_ingot", "IGNORE"))
                .isEqualTo("IGNORED:minecraft:item#oldmod:tin_ingot");
        assertThatThrownBy(() -> bridge.resolveMissingMappingAction("minecraft:item", "oldmod:broken", "FAIL"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldBridgeCapabilitiesAndSimulateNestedTransactionsWithRollback() {
        TransferCapabilityBridge capBridge = new TransferCapabilityBridge();
        assertThat(capBridge.insertFabricToForge("minecraft:diamond", 40L, 64, true)).isEqualTo(40L);
        assertThat(capBridge.getCommittedBalance("minecraft:diamond")).isZero();

        assertThat(capBridge.insertFabricToForge("minecraft:diamond", 40L, 64, false)).isEqualTo(40L);
        assertThat(capBridge.insertFabricToForge("minecraft:diamond", 40L, 64, false)).isEqualTo(24L);
        assertThat(capBridge.extractForgeToFabric("minecraft:diamond", 14L, 64L, false)).isEqualTo(14L);
        assertThat(capBridge.bridgeEnergyTransfer(1000L, 500L, false)).isEqualTo(500L);

        TransactionContextSimulator txSimulator = new TransactionContextSimulator();
        int outerDepth = txSimulator.beginNestedTransaction();
        txSimulator.recordSimulatedMutation(outerDepth, "slot_0", 10L, 30L);

        int innerDepth = txSimulator.beginNestedTransaction();
        txSimulator.recordSimulatedMutation(innerDepth, "slot_0", 30L, 64L);
        assertThat(txSimulator.getCurrentSlotAmount("slot_0")).isEqualTo(64L);

        txSimulator.abortAndRollbackTransaction(innerDepth);
        assertThat(txSimulator.getCurrentSlotAmount("slot_0")).isEqualTo(30L);

        txSimulator.commitTransaction(outerDepth);
        assertThat(txSimulator.getCurrentSlotAmount("slot_0")).isEqualTo(30L);
    }
}
