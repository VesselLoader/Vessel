package dev.vessel.kernel.sanitizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite verifying clean-room GPL license boundary auditing and bytecode signature scanning.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CleanRoomLicenseAuditorTest {

    @Test
    @DisplayName("CleanRoomGplLicenseAuditor flags forbidden copyleft packages in core classpath")
    public void testCoreClasspathAuditing() {
        CleanRoomGplLicenseAuditor auditor = new CleanRoomGplLicenseAuditor();

        List<String> cleanClasses = List.of(
                "dev.vessel.kernel.VesselMicrokernel",
                "dev.vessel.kernel.migration.LegacyAnvilBiomeArrayExpander",
                "org.slf4j.LoggerFactory"
        );
        LicenseAuditReport cleanReport = auditor.auditCoreClasspath(cleanClasses);
        assertThat(cleanReport.cleanRoomCompliant()).isTrue();
        assertThat(cleanReport.violations()).isEmpty();

        List<String> contaminatedClasses = List.of(
                "dev.vessel.kernel.VesselMicrokernel",
                "org.bukkit.craftbukkit.CraftServer",
                "com.destroystokyo.paper.event.block.BeaconEffectEvent"
        );
        LicenseAuditReport dirtyReport = auditor.auditCoreClasspath(contaminatedClasses);
        assertThat(dirtyReport.cleanRoomCompliant()).isFalse();
        assertThat(dirtyReport.violationCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("LicenseIsolationBoundary forbids static linkage of GPL code from permissive host")
    public void testLicenseBoundaryEvaluation() {
        CleanRoomGplLicenseAuditor auditor = new CleanRoomGplLicenseAuditor();

        // Permissive host statically linking GPL dependency -> VIOLATION
        LicenseAuditReport staticGpl = auditor.auditModuleBoundary(
                "vessel-core", SoftwareLicenseKind.APACHE_2_0,
                "gpl-library", SoftwareLicenseKind.GPL_3_0,
                false
        );
        assertThat(staticGpl.cleanRoomCompliant()).isFalse();
        assertThat(staticGpl.violations().getFirst()).contains("Clean-room isolation violation");

        // Permissive host linking GPL dependency via dynamic ClassLoader/IPC -> PERMITTED
        LicenseAuditReport dynamicGpl = auditor.auditModuleBoundary(
                "vessel-core", SoftwareLicenseKind.APACHE_2_0,
                "gpl-plugin", SoftwareLicenseKind.GPL_3_0,
                true
        );
        assertThat(dynamicGpl.cleanRoomCompliant()).isTrue();
    }

    @Test
    @DisplayName("BytecodeCopyrightSignatureScanner detects embedded license headers in bytecode")
    public void testBytecodeCopyrightScanning() {
        TestBytecodeClassGenerator generator = new TestBytecodeClassGenerator();
        BytecodeCopyrightSignatureScanner scanner = new BytecodeCopyrightSignatureScanner();

        byte[] gplBytes = generator.generateClassWithConstantString(
                "com/example/GplSample",
                "GNU General Public License version 3 (GPLv3)"
        );
        BytecodeCopyrightScanReport gplReport = scanner.scanClassBytecode(gplBytes);
        assertThat(gplReport.hasCopyleftThreat()).isTrue();
        assertThat(gplReport.detectedSignatures()).isNotEmpty();
        assertThat(gplReport.detectedSignatures().getFirst().impliedLicense()).isEqualTo(SoftwareLicenseKind.GPL_3_0);

        byte[] apacheBytes = generator.generateClassWithConstantString(
                "com/example/ApacheSample",
                "Licensed under the Apache License, Version 2.0"
        );
        BytecodeCopyrightScanReport apacheReport = scanner.scanClassBytecode(apacheBytes);
        assertThat(apacheReport.hasCopyleftThreat()).isFalse();
        assertThat(apacheReport.detectedSignatures()).isNotEmpty();
        assertThat(apacheReport.detectedSignatures().getFirst().impliedLicense()).isEqualTo(SoftwareLicenseKind.APACHE_2_0);

        // Batch scan
        BytecodeCopyrightScanReport batch = scanner.scanMultipleClasses(Map.of(
                "GplClass", gplBytes,
                "ApacheClass", apacheBytes
        ));
        assertThat(batch.scannedClassesCount()).isEqualTo(2);
        assertThat(batch.hasCopyleftThreat()).isTrue();
    }
}
