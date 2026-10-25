package dev.vessel.kernel.sanitizer;

import java.util.List;
import java.util.Objects;

/**
 * Immutable audit report summarizing bytecode copyright and license signature scanning results.
 *
 * @param scannedClassesCount total number of bytecode classes analyzed
 * @param detectedSignatures  immutable list of detected copyright and license matches
 * @param hasCopyleftThreat   true if one or more copyleft license signatures were discovered
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BytecodeCopyrightScanReport(
        int scannedClassesCount,
        List<CopyrightSignatureRecord> detectedSignatures,
        boolean hasCopyleftThreat
) {

    /**
     * Compact constructor creating an immutable copy of signatures.
     */
    public BytecodeCopyrightScanReport {
        Objects.requireNonNull(detectedSignatures, "detectedSignatures cannot be null");
        detectedSignatures = List.copyOf(detectedSignatures);
    }
}
