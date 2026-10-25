package dev.vessel.kernel.adapter.fabric;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable record capturing the result of a Fabric mod entrypoint execution.
 *
 * @param modId the unique identifier of the mod owning the entrypoint
 * @param entrypointClass the fully qualified class name of the entrypoint
 * @param phase the lifecycle phase name (e.g., "main", "server", "client")
 * @param status the execution status
 * @param error optional error cause if status is {@link FabricInvocationStatus#FAILED} or {@link FabricInvocationStatus#NOT_FOUND}
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FabricInvocationResult(
        String modId,
        String entrypointClass,
        String phase,
        FabricInvocationStatus status,
        Throwable error
) {

    /**
     * Compact constructor validating mandatory fields.
     */
    public FabricInvocationResult {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(entrypointClass, "entrypointClass cannot be null");
        Objects.requireNonNull(phase, "phase cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
    }

    /**
     * Creates a successful invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the entrypoint class name
     * @param phase the lifecycle phase
     * @return successful result instance
     */
    public static FabricInvocationResult success(String modId, String entrypointClass, String phase) {
        return new FabricInvocationResult(modId, entrypointClass, phase, FabricInvocationStatus.SUCCESS, null);
    }

    /**
     * Creates a skipped invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the entrypoint class name
     * @param phase the lifecycle phase
     * @return skipped result instance
     */
    public static FabricInvocationResult skipped(String modId, String entrypointClass, String phase) {
        return new FabricInvocationResult(modId, entrypointClass, phase, FabricInvocationStatus.SKIPPED, null);
    }

    /**
     * Creates a failed invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the entrypoint class name
     * @param phase the lifecycle phase
     * @param error the root cause exception
     * @return failed result instance
     */
    public static FabricInvocationResult failed(String modId, String entrypointClass, String phase, Throwable error) {
        return new FabricInvocationResult(modId, entrypointClass, phase, FabricInvocationStatus.FAILED, error);
    }

    /**
     * Creates a not-found invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the entrypoint class name
     * @param phase the lifecycle phase
     * @param error optional exception describing missing class or method
     * @return not found result instance
     */
    public static FabricInvocationResult notFound(String modId, String entrypointClass, String phase, Throwable error) {
        return new FabricInvocationResult(modId, entrypointClass, phase, FabricInvocationStatus.NOT_FOUND, error);
    }

    /**
     * Retrieves the optional error cause.
     *
     * @return optional throwable
     */
    public Optional<Throwable> getOptionalError() {
        return Optional.ofNullable(error);
    }
}
