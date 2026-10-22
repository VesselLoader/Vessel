package dev.vessel.kernel.transform.mixin;

import dev.vessel.kernel.ir.VesselSemanticHookIntent;
import dev.vessel.kernel.ir.VesselSemanticHookRecord;

import java.util.Objects;

/**
 * Normalizes vendor-specific Mixin annotations into canonical VESSEL Semantic Hook Records.
 * Resolves injection targets, priority mappings, and semantic hook intent classifications.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class MixinToSemanticHookConverter {

    /**
     * Converts a parsed Mixin annotation into a canonical VesselSemanticHookRecord.
     *
     * @param targetOwner internal name of target class (e.g. "net/minecraft/world/entity/Entity")
     * @param sourceClass internal name of the Mixin class defining the hook
     * @param sourceMethod name of the hook handler method
     * @param parsed the parsed annotation metadata
     * @param priority user or default priority order
     * @return normalized VesselSemanticHookRecord
     */
    public VesselSemanticHookRecord convert(
            String targetOwner,
            String sourceClass,
            String sourceMethod,
            ParsedMixinAnnotation parsed,
            int priority) {
        Objects.requireNonNull(targetOwner, "targetOwner cannot be null");
        Objects.requireNonNull(sourceClass, "sourceClass cannot be null");
        Objects.requireNonNull(sourceMethod, "sourceMethod cannot be null");
        Objects.requireNonNull(parsed, "parsed cannot be null");

        VesselSemanticHookIntent intent = resolveIntent(parsed);
        String targetDesc = parsed.targetDescriptor().isEmpty() ? "()V" : parsed.targetDescriptor();

        return new VesselSemanticHookRecord(
                intent,
                targetOwner,
                parsed.targetMethod(),
                targetDesc,
                sourceClass,
                sourceMethod,
                priority
        );
    }

    private VesselSemanticHookIntent resolveIntent(ParsedMixinAnnotation parsed) {
        String desc = parsed.annotationDescriptor();

        if (MixinAnnotationParser.WRAP_OPERATION_DESC.equals(desc)) {
            return VesselSemanticHookIntent.METHOD_OPERATION_WRAPPING;
        }

        if (MixinAnnotationParser.MODIFY_VAR_DESC.equals(desc)) {
            return VesselSemanticHookIntent.METHOD_INVOCATION_MUTATION;
        }

        if (MixinAnnotationParser.REDIRECT_DESC.equals(desc)) {
            if ("FIELD".equalsIgnoreCase(parsed.injectionPointAt())) {
                return VesselSemanticHookIntent.FIELD_ACCESS_INTERCEPTION;
            }
            return VesselSemanticHookIntent.METHOD_INVOCATION_MUTATION;
        }

        // Default to @Inject handling
        String at = parsed.injectionPointAt();
        if ("RETURN".equalsIgnoreCase(at) || "TAIL".equalsIgnoreCase(at)) {
            return VesselSemanticHookIntent.METHOD_POST_EXECUTION;
        }

        return VesselSemanticHookIntent.METHOD_PRE_EXECUTION;
    }
}
