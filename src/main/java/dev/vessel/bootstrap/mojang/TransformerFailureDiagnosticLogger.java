package dev.vessel.bootstrap.mojang;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Diagnostic logger specifically capturing and reporting bytecode transformer failures in
 * {@link TierOneGameTransformingClassLoader}.
 *
 * <p>When a bytecode transformer fails on a target Minecraft class (e.g., due to unexpected bytecode,
 * I/O read failure, or class verification issues), this logger records structured diagnostics and
 * emits an actionable warning message to {@link System#err} or the configured {@link PrintStream},
 * preventing silent failure suppression while allowing graceful fallback to untransformed definition.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class TransformerFailureDiagnosticLogger {

    private final PrintStream outStream;
    private final List<String> failureHistory;
    private final AtomicInteger failureCount;

    /**
     * Constructs a diagnostic logger writing to {@link System#err}.
     */
    public TransformerFailureDiagnosticLogger() {
        this(System.err);
    }

    /**
     * Constructs a diagnostic logger writing to the specified {@link PrintStream}.
     *
     * @param outStream output stream for diagnostic failure messages
     */
    public TransformerFailureDiagnosticLogger(PrintStream outStream) {
        this.outStream = Objects.requireNonNull(outStream, "outStream must not be null");
        this.failureHistory = new CopyOnWriteArrayList<>();
        this.failureCount = new AtomicInteger(0);
    }

    /**
     * Logs a transformer execution or bytecode loading failure.
     *
     * @param className       fully qualified name of the target class being transformed
     * @param transformerName name or identifier of the transformer that failed
     * @param cause           the exception encountered during transformation
     */
    public void logFailure(String className, String transformerName, Throwable cause) {
        Objects.requireNonNull(className, "className must not be null");
        Objects.requireNonNull(transformerName, "transformerName must not be null");

        failureCount.incrementAndGet();
        String errorType = cause != null ? cause.getClass().getSimpleName() : "Unknown";
        String errorMsg = cause != null ? cause.getMessage() : "No message";
        String diagnostic = String.format(
                "[VESSEL/WARN] Bytecode transformer '%s' failed for target class '%s' [%s: %s]. Falling back to standard class definition.",
                transformerName,
                className,
                errorType,
                errorMsg
        );
        failureHistory.add(diagnostic);
        outStream.println(diagnostic);
        if (cause != null) {
            cause.printStackTrace(outStream);
        }
        outStream.flush();
    }

    /**
     * Returns the total count of transformer failures recorded by this logger.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Returns an unmodifiable snapshot of recorded diagnostic failure messages.
     *
     * @return list of formatted diagnostic strings
     */
    public List<String> getFailureHistory() {
        return Collections.unmodifiableList(new ArrayList<>(failureHistory));
    }

    /**
     * Resets recorded failures and counters.
     */
    public void reset() {
        failureCount.set(0);
        failureHistory.clear();
    }
}
