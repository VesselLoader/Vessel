package dev.vessel.kernel.sanitizer;

import java.util.List;
import java.util.Objects;

/**
 * Immutable audit report recording clean-room license boundary validation results.
 *
 * @param scannedClassesCount total number of classes or modules inspected
 * @param violationCount      number of hard clean-room boundary violations
 * @param warningCount        number of license warnings or potential risks
 * @param violations          immutable list of violation descriptions
 * @param warnings            immutable list of warning descriptions
 * @param cleanRoomCompliant  true if no hard copyleft contamination was found
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record LicenseAuditReport(
        int scannedClassesCount,
        int violationCount,
        int warningCount,
        List<String> violations,
        List<String> warnings,
        boolean cleanRoomCompliant
) {

    /**
     * Compact constructor guaranteeing defensive immutability.
     */
    public LicenseAuditReport {
        Objects.requireNonNull(violations, "violations cannot be null");
        Objects.requireNonNull(warnings, "warnings cannot be null");
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
    }
}
