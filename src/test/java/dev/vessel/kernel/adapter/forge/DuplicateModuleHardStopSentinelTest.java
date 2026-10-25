package dev.vessel.kernel.adapter.forge;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests verifying pre-boot collision sentinels for duplicate module namespaces
 * (Decision 8 & 17) and duplicate registry entry key claims (ERR-92).
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DuplicateModuleHardStopSentinelTest {

    private DuplicateModuleHardStopSentinel moduleSentinel;
    private ModDuplicateRegistryKeyScanner keyScanner;

    @BeforeEach
    public void setUp() {
        moduleSentinel = new DuplicateModuleHardStopSentinel();
        keyScanner = new ModDuplicateRegistryKeyScanner();
    }

    @Test
    @DisplayName("Duplicate Module Sentinel - Approves cleanly separated modules")
    public void testCleanModuleInventory() {
        List<ModuleNamespaceDescriptor> cleanList = List.of(
                new ModuleNamespaceDescriptor("jei", "FORGE", "/mods/jei-1.20.1.jar", "15.3.0"),
                new ModuleNamespaceDescriptor("create", "FORGE", "/mods/create-1.20.1.jar", "0.5.1"),
                new ModuleNamespaceDescriptor("fabric_api", "FABRIC", "/mods/fabric-api.jar", "0.92.0")
        );

        DuplicateScanReport report = moduleSentinel.scanAndEnforce(cleanList);
        assertThat(report.hasDuplicates()).isFalse();
        assertThat(report.duplicateCount()).isEqualTo(0);
        assertThat(report.duplicatesByNamespace()).isEmpty();
    }

    @Test
    @DisplayName("Duplicate Module Sentinel - Enforces Hard Stop (Decision 8 & 17) on duplicate namespace")
    public void testDuplicateNamespaceHardStop() {
        List<ModuleNamespaceDescriptor> collidingList = List.of(
                new ModuleNamespaceDescriptor("jei", "FORGE", "/mods/jei-15.0.0.jar", "15.0.0"),
                new ModuleNamespaceDescriptor("jei", "FORGE", "/mods/jei-15.2.0-duplicate.jar", "15.2.0"),
                new ModuleNamespaceDescriptor("create", "FORGE", "/mods/create.jar", "0.5.1")
        );

        assertThatThrownBy(() -> moduleSentinel.scanAndEnforce(collidingList))
                .isInstanceOf(DuplicateModuleHardStopException.class)
                .hasMessageContaining("[DECISION-8-HARD-STOP]")
                .hasMessageContaining("jei");

        DuplicateScanReport scanOnlyReport = moduleSentinel.scanOnly(collidingList);
        assertThat(scanOnlyReport.hasDuplicates()).isTrue();
        assertThat(scanOnlyReport.duplicateCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Registry Key Scanner - Validates unique registry claims successfully")
    public void testCleanRegistryClaims() {
        List<RegistryKeyClaim> claims = List.of(
                new RegistryKeyClaim("minecraft:item", "create:wrench", "create", "CreateItems.java"),
                new RegistryKeyClaim("minecraft:item", "curios:ring", "curios", "CuriosItems.java"),
                new RegistryKeyClaim("minecraft:block", "create:casing", "create", "CreateBlocks.java")
        );

        RegistryCollisionReport report = keyScanner.scanAndValidate(claims);
        assertThat(report.hasCollisions()).isFalse();
        assertThat(report.collisionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Registry Key Scanner - Catches duplicate key claims across mods with ERR-92")
    public void testDuplicateRegistryKeyCollision() {
        List<RegistryKeyClaim> collidingClaims = List.of(
                new RegistryKeyClaim("minecraft:item", "shared:copper_wire", "mod_electric", "ElectricMod.java"),
                new RegistryKeyClaim("minecraft:item", "shared:copper_wire", "mod_industrial", "IndustrialMod.java")
        );

        assertThatThrownBy(() -> keyScanner.scanAndValidate(collidingClaims))
                .isInstanceOf(DuplicateRegistryKeyException.class)
                .hasMessageContaining("[ERR-92]")
                .hasMessageContaining("shared:copper_wire")
                .hasMessageContaining("mod_electric")
                .hasMessageContaining("mod_industrial");

        RegistryCollisionReport report = keyScanner.scanOnly(collidingClaims);
        assertThat(report.hasCollisions()).isTrue();
        assertThat(report.collisionCount()).isEqualTo(1);
    }
}
