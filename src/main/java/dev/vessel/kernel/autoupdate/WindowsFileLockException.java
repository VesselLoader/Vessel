package dev.vessel.kernel.autoupdate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Specialized typed exception thrown when an in-place file replacement or binary swap is prevented
 * by Windows OS mandatory file locking (e.g. active JVM JAR handles).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class WindowsFileLockException extends IOException {

    private static final long serialVersionUID = 1L;

    private final transient Path lockedPath;
    private final int attemptCount;

    /**
     * Constructs a new {@link WindowsFileLockException} with detail message.
     *
     * @param message human-readable explanation
     */
    public WindowsFileLockException(String message) {
        super(message);
        this.lockedPath = null;
        this.attemptCount = 0;
    }

    /**
     * Constructs a new {@link WindowsFileLockException} with detail message and underlying cause.
     *
     * @param message human-readable explanation
     * @param cause   underlying exception
     */
    public WindowsFileLockException(String message, Throwable cause) {
        super(message, cause);
        this.lockedPath = null;
        this.attemptCount = 0;
    }

    /**
     * Constructs a new {@link WindowsFileLockException} with diagnostic parameters.
     *
     * @param lockedPath   path to the locked file resource
     * @param attemptCount number of failed retry attempts
     * @param reason       diagnostic explanation
     * @param cause        underlying system I/O exception
     */
    public WindowsFileLockException(Path lockedPath, int attemptCount, String reason, Throwable cause) {
        super("Failed to bypass Windows file lock on [" + Objects.requireNonNull(lockedPath, "lockedPath must not be null")
                + "] after " + attemptCount + " attempts: " + reason, cause);
        this.lockedPath = lockedPath;
        this.attemptCount = attemptCount;
    }

    /**
     * Returns the locked path if available, or {@code null}.
     *
     * @return locked path or {@code null}
     */
    public Path lockedPath() {
        return this.lockedPath;
    }

    /**
     * Returns the number of retry attempts executed prior to failure.
     *
     * @return retry attempt count
     */
    public int attemptCount() {
        return this.attemptCount;
    }
}
