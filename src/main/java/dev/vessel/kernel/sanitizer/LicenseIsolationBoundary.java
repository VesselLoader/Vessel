package dev.vessel.kernel.sanitizer;

import java.util.Objects;

/**
 * Clean-room architecture boundary evaluator verifying legal isolation between
 * permissive core modules (MIT/Apache 2.0) and copyleft dependencies (GPL/LGPL).
 * <p>
 * Mitigates: <b>ERR-95</b>, <b>OPS-0201..0350</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LicenseIsolationBoundary {

    /**
     * Determines whether a dependency linkage is legally permitted under clean-room rules.
     *
     * @param hostLicense        the license of the consuming host module (e.g. VESSEL Microkernel)
     * @param dependencyLicense  the license of the referenced dependency or plugin
     * @param hasDynamicBarrier  true if linkage occurs across a dynamic process, IPC, or isolated ClassLoader barrier
     * @return true if linkage adheres to clean-room separation requirements
     */
    public boolean isBoundaryPermitted(
            SoftwareLicenseKind hostLicense,
            SoftwareLicenseKind dependencyLicense,
            boolean hasDynamicBarrier
    ) {
        Objects.requireNonNull(hostLicense, "hostLicense cannot be null");
        Objects.requireNonNull(dependencyLicense, "dependencyLicense cannot be null");

        // Permissive to Permissive is universally permitted
        if (hostLicense.isPermissive() && dependencyLicense.isPermissive()) {
            return true;
        }

        // Permissive host consuming LGPL: permitted with dynamic linking barrier
        if (hostLicense.isPermissive() && dependencyLicense.isWeakCopyleft()) {
            return hasDynamicBarrier;
        }

        // Permissive host consuming GPL/AGPL: strictly prohibited unless isolated via out-of-process IPC / dynamic plugin boundary
        if (hostLicense.isPermissive() && dependencyLicense.isStrongCopyleft()) {
            return hasDynamicBarrier;
        }

        // Copyleft consuming Permissive: permitted
        if (hostLicense.isCopyleft() && dependencyLicense.isPermissive()) {
            return true;
        }

        return false;
    }

    /**
     * Diagnoses any violation between two modules across an architectural boundary.
     *
     * @param hostModule         host module name
     * @param hostLicense        host module license
     * @param dependencyModule   dependency module name
     * @param dependencyLicense  dependency module license
     * @param hasDynamicBarrier  flag indicating dynamic ClassLoader/IPC isolation
     * @return null if compliant, or a detailed violation explanation if breached
     */
    public String evaluateBoundaryViolation(
            String hostModule,
            SoftwareLicenseKind hostLicense,
            String dependencyModule,
            SoftwareLicenseKind dependencyLicense,
            boolean hasDynamicBarrier
    ) {
        Objects.requireNonNull(hostModule, "hostModule cannot be null");
        Objects.requireNonNull(dependencyModule, "dependencyModule cannot be null");

        if (isBoundaryPermitted(hostLicense, dependencyLicense, hasDynamicBarrier)) {
            return null;
        }

        return "Clean-room isolation violation: Host module '" + hostModule + "' [" + hostLicense
                + "] statically links copyleft dependency '" + dependencyModule + "' [" + dependencyLicense
                + "] without dynamic ClassLoader or IPC isolation barrier.";
    }
}
