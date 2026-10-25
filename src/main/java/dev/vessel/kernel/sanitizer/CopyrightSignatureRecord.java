package dev.vessel.kernel.sanitizer;

import java.util.Objects;

/**
 * Immutable record capturing a specific copyright or license signature match detected within a class.
 *
 * @param className         the fully qualified or internal class name where match was found
 * @param matchedSignature  the exact string extracted from bytecode that matched
 * @param patternIdentifier identifier of the matched pattern
 * @param impliedLicense    inferred license type
 * @param copyleftThreat    true if this match poses copyleft licensing risks
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CopyrightSignatureRecord(
        String className,
        String matchedSignature,
        String patternIdentifier,
        SoftwareLicenseKind impliedLicense,
        boolean copyleftThreat
) {

    /**
     * Compact constructor validating required fields.
     */
    public CopyrightSignatureRecord {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(matchedSignature, "matchedSignature cannot be null");
        Objects.requireNonNull(patternIdentifier, "patternIdentifier cannot be null");
        Objects.requireNonNull(impliedLicense, "impliedLicense cannot be null");
    }
}
