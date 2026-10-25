package dev.vessel.kernel.access;

import dev.vessel.kernel.ir.VesselAccessLevel;
import dev.vessel.kernel.ir.VesselAccessWideningTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying JPMS module boundary virtualization and canonical access registry consolidation.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class JpmsAndAccessRegistryTest {

    @Test
    @DisplayName("JpmsModuleBoundaryVirtualizer safely opens base packages to unnamed modules")
    public void testJpmsModuleVirtualization() {
        JpmsModuleBoundaryVirtualizer virtualizer = new JpmsModuleBoundaryVirtualizer();

        // Testing openBasePackageToAllUnnamed with standard reflection packages
        JpmsVirtualizationResult result = virtualizer.openBasePackageToAllUnnamed("java.lang.reflect");
        assertThat(result).isNotNull();
        assertThat(result.successful()).isTrue();

        List<JpmsVirtualizationResult> batchResults = virtualizer.openAllBaseInternalPackages();
        assertThat(batchResults).isNotEmpty();
        assertThat(batchResults.stream().allMatch(JpmsVirtualizationResult::successful)).isTrue();
    }

    @Test
    @DisplayName("CanonicalAccessRegistry parses Fabric Access Widener and populates unified rules")
    public void testFabricAccessWidenerParsing() throws IOException {
        CanonicalAccessRegistry registry = new CanonicalAccessRegistry();
        String awContent = """
                accessWidener v1 named
                accessible class net/minecraft/world/entity/Entity
                mutable field net/minecraft/world/entity/Entity isRemoved Z
                extendable method net/minecraft/world/entity/Entity tick ()V
                """;

        try (ByteArrayInputStream in = new ByteArrayInputStream(awContent.getBytes(StandardCharsets.UTF_8))) {
            registry.loadFabricAccessWidener(in);
        }

        assertThat(registry.totalRulesCount()).isEqualTo(3);

        CanonicalAccessModification classMod = registry.getClassModification("net/minecraft/world/entity/Entity");
        assertThat(classMod).isNotNull();
        assertThat(classMod.accessLevel()).isEqualTo(CanonicalAccessLevel.PUBLIC);

        CanonicalAccessModification fieldMod = registry.getFieldModification("net/minecraft/world/entity/Entity", "isRemoved");
        assertThat(fieldMod).isNotNull();
        assertThat(fieldMod.makeMutable()).isTrue();

        CanonicalAccessModification methodMod = registry.getMethodModification("net/minecraft/world/entity/Entity", "tick", "()V");
        assertThat(methodMod).isNotNull();
        assertThat(methodMod.makeExtendable()).isTrue();
    }

    @Test
    @DisplayName("CanonicalAccessRegistry parses Forge Access Transformer and exports to VesselAccessWideningTable")
    public void testForgeAtParsingAndExport() throws IOException {
        CanonicalAccessRegistry registry = new CanonicalAccessRegistry();
        String atContent = """
                public net.minecraft.world.level.Level
                public-f net.minecraft.world.level.Level isClientSide # world side
                public net.minecraft.world.level.Level getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;
                """;

        try (ByteArrayInputStream in = new ByteArrayInputStream(atContent.getBytes(StandardCharsets.UTF_8))) {
            registry.loadForgeAccessTransformer(in);
        }

        assertThat(registry.totalRulesCount()).isEqualTo(3);

        // Export to VesselAccessWideningTable
        VesselAccessWideningTable wideningTable = registry.exportToWideningTable();
        assertThat(wideningTable.hasWidening("net/minecraft/world/level/Level")).isTrue();

        VesselAccessWideningTable.ClassRule classRule = wideningTable.getClassRule("net/minecraft/world/level/Level");
        assertThat(classRule).isNotNull();
        assertThat(classRule.accessLevel()).isEqualTo(VesselAccessLevel.PUBLIC);

        VesselAccessWideningTable.FieldRule fieldRule = wideningTable.getFieldRule("net/minecraft/world/level/Level", "isClientSide");
        assertThat(fieldRule).isNotNull();
        assertThat(fieldRule.mutable()).isTrue();
    }
}
