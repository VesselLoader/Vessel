package dev.vessel.kernel.resourcepack.http;

/**
 * Exception thrown when an HTTP Range request cannot be satisfied by the resource.
 * <p>
 * Signals the HTTP server layer to return HTTP status code {@code 416 Range Not Satisfiable}.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselRangeNotSatisfiableException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final long totalLength;

    public VesselRangeNotSatisfiableException(String message, long totalLength) {
        super(message);
        this.totalLength = totalLength;
    }

    public long getTotalLength() {
        return totalLength;
    }
}
