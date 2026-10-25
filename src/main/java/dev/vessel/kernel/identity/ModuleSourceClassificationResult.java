package dev.vessel.kernel.identity;

import java.util.Objects;

/**
 * Immutable classification verdict containing the recognized source loader kind,
 * confidence score, and matched primary indicator signature.
 *
 * @param detectedKind the identified source loader ecosystem
 * @param confidenceScore numeric score based on bytecode indicator density
 * @param primaryMatchedIndicator bytecode symbol or annotation descriptor that determined the verdict
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ModuleSourceClassificationResult(
        SourceLoaderKind detectedKind,
        int confidenceScore,
        String primaryMatchedIndicator
) {
    public ModuleSourceClassificationResult {
        Objects.requireNonNull(detectedKind, "detectedKind cannot be null");
        primaryMatchedIndicator = (primaryMatchedIndicator != null) ? primaryMatchedIndicator : "";
    }
}
