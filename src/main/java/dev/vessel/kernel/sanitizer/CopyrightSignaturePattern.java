package dev.vessel.kernel.sanitizer;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable signature pattern used to detect copyright notices, license headers,
 * and copyleft declarations embedded within Java bytecode constant pools and strings.
 *
 * @param identifier        unique identifier of the pattern
 * @param regexPattern      regular expression matching target signature strings
 * @param impliedLicense    the software license implied by this signature
 * @param isCopyleftThreat  flag indicating whether this signature represents a copyleft containment risk
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CopyrightSignaturePattern(
        String identifier,
        String regexPattern,
        SoftwareLicenseKind impliedLicense,
        boolean isCopyleftThreat
) {

    /**
     * Compact constructor validating regex syntax and non-null arguments.
     */
    public CopyrightSignaturePattern {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(regexPattern, "regexPattern cannot be null");
        Objects.requireNonNull(impliedLicense, "impliedLicense cannot be null");

        // Validate that pattern compiles
        Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
    }
}
