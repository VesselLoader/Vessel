package dev.vessel.kernel.ir;

/**
 * Thrown when an unrecoverable bytecode translation or transformation failure occurs
 * within the {@link SinglePassBytecodePipeline}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class SinglePassPipelineException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SinglePassPipelineException(String message) {
        super(message);
    }

    public SinglePassPipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}
