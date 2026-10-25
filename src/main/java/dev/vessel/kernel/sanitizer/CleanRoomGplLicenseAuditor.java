package dev.vessel.kernel.sanitizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Clean-room auditor monitoring and strictly enforcing architectural boundaries
 * between permissive microkernel code (MIT/Apache 2.0) and GPL/LGPL copyleft components.
 * <p>
 * Scans runtime classpaths and dependency graphs to ensure zero GPL source code contamination.
 * </p>
 * <p>
 * Mitigates: <b>ERR-95</b>, <b>OPS-0201..0350</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CleanRoomGplLicenseAuditor {

    private static final Set<String> FORBIDDEN_CORE_PACKAGES;

    static {
        Set<String> packages = new HashSet<>();
        // Internal packages that must never be directly linked or compiled into permissive core
        packages.add("org.bukkit.craftbukkit.");
        packages.add("com.destroystokyo.paper.event.");
        packages.add("io.papermc.paper.plugin.configuration.");
        packages.add("net.minecraftforge.fml.common.launcher.");
        packages.add("org.spigotmc.SpigotWorldConfig");
        FORBIDDEN_CORE_PACKAGES = Collections.unmodifiableSet(packages);
    }

    private final LicenseIsolationBoundary boundary;

    /**
     * Constructs a new CleanRoomGplLicenseAuditor.
     */
    public CleanRoomGplLicenseAuditor() {
        this.boundary = new LicenseIsolationBoundary();
    }

    /**
     * Audits an arbitrary list of class names present in the core server classpath.
     *
     * @param classNames list of fully qualified class names
     * @return audit report summarizing clean-room compliance
     */
    public LicenseAuditReport auditCoreClasspath(List<String> classNames) {
        if (classNames == null) {
            return new LicenseAuditReport(0, 0, 0, List.of(), List.of(), true);
        }

        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (String className : classNames) {
            if (className == null) {
                continue;
            }

            for (String forbiddenPrefix : FORBIDDEN_CORE_PACKAGES) {
                if (className.startsWith(forbiddenPrefix)) {
                    violations.add("Forbidden copyleft internal class detected in core classpath: " + className);
                    break;
                }
            }
        }

        boolean compliant = violations.isEmpty();
        return new LicenseAuditReport(
                classNames.size(),
                violations.size(),
                warnings.size(),
                violations,
                warnings,
                compliant
        );
    }

    /**
     * Verifies that a module-to-dependency relationship complies with clean-room isolation rules.
     *
     * @param hostModule        host module identifier
     * @param hostLicense       host module license
     * @param dependencyModule  dependency module identifier
     * @param dependencyLicense dependency license
     * @param hasDynamicBarrier true if separated by dynamic ClassLoader or IPC
     * @return audit report
     */
    public LicenseAuditReport auditModuleBoundary(
            String hostModule,
            SoftwareLicenseKind hostLicense,
            String dependencyModule,
            SoftwareLicenseKind dependencyLicense,
            boolean hasDynamicBarrier
    ) {
        String violation = boundary.evaluateBoundaryViolation(
                hostModule,
                hostLicense,
                dependencyModule,
                dependencyLicense,
                hasDynamicBarrier
        );

        List<String> violations = violation != null ? List.of(violation) : List.of();
        boolean compliant = violations.isEmpty();

        return new LicenseAuditReport(
                1,
                violations.size(),
                0,
                violations,
                List.of(),
                compliant
        );
    }
}
