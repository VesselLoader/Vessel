package dev.vessel.kernel.datapack;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thrown when a directed cycle is detected in an advancement prerequisite hierarchy.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class CyclicAdvancementException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> cyclePath;

    /**
     * Constructs a cyclic advancement exception.
     *
     * @param message   error description
     * @param cyclePath sequential node IDs participating in the cycle
     */
    public CyclicAdvancementException(String message, List<String> cyclePath) {
        super(message);
        this.cyclePath = (cyclePath != null) ? new ArrayList<>(cyclePath) : new ArrayList<>();
    }

    /**
     * Returns the detected cycle path.
     *
     * @return unmodifiable list of cycle node identifiers
     */
    public List<String> getCyclePath() {
        return Collections.unmodifiableList(cyclePath);
    }
}
