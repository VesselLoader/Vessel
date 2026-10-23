package dev.vessel.kernel.test;

import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.identity.ModuleClassifier;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import dev.vessel.kernel.ir.VesselAccessLevel;
import dev.vessel.kernel.ir.VesselSemanticHookIntent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying core identity, classification, actor coordinate hashing, and IR invariants.
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-3)
 */
class IdentityAndActorTest {

    @Test
    @DisplayName("SourceLoaderKind family classification and ID resolution")
    void testSourceLoaderKind() {
        assertThat(SourceLoaderKind.BUKKIT.isBukkitFamily()).isTrue();
        assertThat(SourceLoaderKind.PAPER.isBukkitFamily()).isTrue();
        assertThat(SourceLoaderKind.FOLIA.isBukkitFamily()).isTrue();
        assertThat(SourceLoaderKind.FABRIC.isBukkitFamily()).isFalse();

        assertThat(SourceLoaderKind.FABRIC.isFabricFamily()).isTrue();
        assertThat(SourceLoaderKind.QUILT.isFabricFamily()).isTrue();
        assertThat(SourceLoaderKind.FORGE.isFabricFamily()).isFalse();

        assertThat(SourceLoaderKind.FORGE.isForgeFamily()).isTrue();
        assertThat(SourceLoaderKind.NEOFORGE.isForgeFamily()).isTrue();
        assertThat(SourceLoaderKind.BUKKIT.isForgeFamily()).isFalse();

        assertThat(SourceLoaderKind.fromId("fabric")).isEqualTo(SourceLoaderKind.FABRIC);
        assertThat(SourceLoaderKind.fromId("NEOFORGE")).isEqualTo(SourceLoaderKind.NEOFORGE);

        assertThatThrownBy(() -> SourceLoaderKind.fromId("unknown_loader"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("VesselModuleIdentifier canonical string and normalization")
    void testVesselModuleIdentifier() {
        VesselModuleIdentifier id = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "Sodium", "0.5.8");
        assertThat(id.namespace()).isEqualTo("sodium"); // normalized lowercase
        assertThat(id.version()).isEqualTo("0.5.8");
        assertThat(id.classifier()).isEqualTo(ModuleClassifier.MAIN);
        assertThat(id.toCanonicalString()).isEqualTo("fabric:sodium@0.5.8");

        VesselModuleIdentifier apiId = new VesselModuleIdentifier(
                SourceLoaderKind.FORGE, "jei", "15.2.0.27", ModuleClassifier.API);
        assertThat(apiId.toCanonicalString()).isEqualTo("forge:jei@15.2.0.27#api");

        // Invalid namespace checks
        assertThatThrownBy(() -> VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "invalid space", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "", "1.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "mod", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RegionCoordinateHash chunk bitshifting and packed key roundtripping")
    void testRegionCoordinateHash() {
        // Chunk (0, 0) belongs to Region (0, 0)
        RegionCoordinateHash r0 = RegionCoordinateHash.fromChunk(0, 0);
        assertThat(r0.regionX()).isEqualTo(0);
        assertThat(r0.regionZ()).isEqualTo(0);

        // Chunk (16, 32) belongs to Region (1, 2)
        RegionCoordinateHash r1 = RegionCoordinateHash.fromChunk(16, 32);
        assertThat(r1.regionX()).isEqualTo(1);
        assertThat(r1.regionZ()).isEqualTo(2);

        // Negative chunk coordinate (-1, -1) belongs to Region (-1, -1) via arithmetic right shift
        RegionCoordinateHash rNeg = RegionCoordinateHash.fromChunk(-1, -1);
        assertThat(rNeg.regionX()).isEqualTo(-1);
        assertThat(rNeg.regionZ()).isEqualTo(-1);

        // Block coordinate (256, 512) belongs to Region (1, 2)
        RegionCoordinateHash rBlock = RegionCoordinateHash.fromBlock(256, 512);
        assertThat(rBlock.regionX()).isEqualTo(1);
        assertThat(rBlock.regionZ()).isEqualTo(2);

        // Packed key roundtrip
        long packed = r1.toPackedKey();
        RegionCoordinateHash unpacked = RegionCoordinateHash.fromPackedKey(packed);
        assertThat(unpacked).isEqualTo(r1);

        long packedNeg = rNeg.toPackedKey();
        RegionCoordinateHash unpackedNeg = RegionCoordinateHash.fromPackedKey(packedNeg);
        assertThat(unpackedNeg).isEqualTo(rNeg);
    }

    @Test
    @DisplayName("VesselSemanticHookIntent and VesselAccessLevel validation")
    void testSemanticHookAndAccess() {
        assertThat(VesselSemanticHookIntent.METHOD_PRE_EXECUTION.supportsCancellation()).isTrue();
        assertThat(VesselSemanticHookIntent.METHOD_OPERATION_WRAPPING.supportsCancellation()).isFalse();

        assertThat(VesselAccessLevel.PUBLIC.isAtLeast(VesselAccessLevel.PRIVATE)).isTrue();
        assertThat(VesselAccessLevel.PRIVATE.isAtLeast(VesselAccessLevel.PUBLIC)).isFalse();
        assertThat(VesselAccessLevel.widest(VesselAccessLevel.PROTECTED, VesselAccessLevel.PUBLIC))
                .isEqualTo(VesselAccessLevel.PUBLIC);
    }
}
