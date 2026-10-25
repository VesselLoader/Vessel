package dev.vessel.kernel.ir;

/**
 * Immutable report representing the outcome of generic signature reconciliation on a class.
 *
 * @param modifiedSignaturesCount number of generic signatures adjusted or repaired
 * @param strippedMalformedCount number of unrecoverable or malformed generic signatures stripped
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SignatureReconcileResult(
        int modifiedSignaturesCount,
        int strippedMalformedCount
) {
    public static final SignatureReconcileResult NO_OP = new SignatureReconcileResult(0, 0);

    public boolean hasModifications() {
        return modifiedSignaturesCount > 0 || strippedMalformedCount > 0;
    }
}
