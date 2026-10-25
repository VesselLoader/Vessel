package dev.vessel.kernel.testmatrix;

/**
 * Checked exception thrown when an incompatible classpath collision occurs across concurrent loader targets.
 *
 * <p>Remediates ERR-10 and ERR-18 by preventing silent classpath pollution.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ClasspathCollisionException extends Exception {

    public ClasspathCollisionException(String message) {
        super(message);
    }

    public ClasspathCollisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
