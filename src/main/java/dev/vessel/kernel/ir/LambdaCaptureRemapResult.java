package dev.vessel.kernel.ir;

/**
 * Immutable report representing the remapping outcome of a synthetic $deserializeLambda$ method.
 *
 * @param hasDeserializeLambdaMethod true if the synthetic method was present in the class
 * @param constantsUpdated count of string constants updated within the deserialization switch
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record LambdaCaptureRemapResult(
        boolean hasDeserializeLambdaMethod,
        int constantsUpdated
) {
    public static final LambdaCaptureRemapResult ABSENT = new LambdaCaptureRemapResult(false, 0);

    public boolean wasModified() {
        return constantsUpdated > 0;
    }
}
