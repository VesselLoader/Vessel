package dev.vessel.kernel.adapter.bukkit;

import java.util.Objects;

/**
 * Scoped auto-closeable token used to temporarily bypass thread validation in internal
 * asynchronous tasks.
 *
 * <p>Enables safe re-entrant usage via {@code try-with-resources} blocks.</p>
 * <p>Strictly adheres to Rule 17 (zero inner, nested, or anonymous classes).</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class AsyncCatcherBypassToken implements AutoCloseable {

    private final AsyncCatcherThreadValidator validator;
    private boolean closed = false;

    /**
     * Constructs a new bypass token tied to a parent validator.
     *
     * @param validator parent validator instance
     */
    public AsyncCatcherBypassToken(AsyncCatcherThreadValidator validator) {
        this.validator = Objects.requireNonNull(validator, "validator cannot be null");
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            validator.releaseBypass();
        }
    }

    /**
     * Checks if this bypass token has already been closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }
}
