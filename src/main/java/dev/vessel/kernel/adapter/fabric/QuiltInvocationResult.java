package dev.vessel.kernel.adapter.fabric;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable record capturing the result of a Quilt mod entrypoint execution.
 *
 * @param modId the unique identifier of the Quilt mod
 * @param entrypointClass the fully qualified name of the entrypoint class
 * @param status the execution status
 * @param error optional exception if execution failed or was not found
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record QuiltInvocationResult(
        String modId,
        String entrypointClass,
        QuiltInvocationStatus status,
        Throwable error
) {

    /**
     * Compact constructor validating non-null arguments.
     */
    public QuiltInvocationResult {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(entrypointClass, "entrypointClass cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
    }

    /**
     * Creates a successful Quilt invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the class name
     * @return successful result instance
     */
    public static QuiltInvocationResult success(String modId, String entrypointClass) {
        return new QuiltInvocationResult(modId, entrypointClass, QuiltInvocationStatus.SUCCESS, null);
    }

    /**
     * Creates a skipped Quilt invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the class name
     * @return skipped result instance
     */
    public static QuiltInvocationResult skipped(String modId, String entrypointClass) {
        return new QuiltInvocationResult(modId, entrypointClass, QuiltInvocationStatus.SKIPPED, null);
    }

    /**
     * Creates a failed Quilt invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the class name
     * @param error the root cause exception
     * @return failed result instance
     */
    public static QuiltInvocationResult failed(String modId, String entrypointClass, Throwable error) {
        return new QuiltInvocationResult(modId, entrypointClass, QuiltInvocationStatus.FAILED, error);
    }

    /**
     * Creates a not-found Quilt invocation result.
     *
     * @param modId the mod identifier
     * @param entrypointClass the class name
     * @param error optional exception describing missing class or method
     * @return not found result instance
     */
    public static QuiltInvocationResult notFound(String modId, String entrypointClass, Throwable error) {
        return new QuiltInvocationResult(modId, entrypointClass, QuiltInvocationStatus.NOT_FOUND, error);
    }

    /**
     * Retrieves optional error throwable.
     *
     * @return optional error
     */
    public Optional<Throwable> getOptionalError() {
        return Optional.ofNullable(error);
    }
}
