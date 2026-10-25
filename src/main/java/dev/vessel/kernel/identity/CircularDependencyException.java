package dev.vessel.kernel.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Exception thrown when an unresolvable circular dependency cycle is detected among modules.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CircularDependencyException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    private final ArrayList<String> cycleParticipants;

    /**
     * Constructs a CircularDependencyException with a message and the cycle participants.
     *
     * @param message descriptive message
     * @param cycleParticipants namespaces involved in the cycle
     */
    public CircularDependencyException(String message, List<String> cycleParticipants) {
        super(Objects.requireNonNull(message, "message cannot be null"));
        this.cycleParticipants = (cycleParticipants != null)
                ? new ArrayList<>(cycleParticipants)
                : new ArrayList<>();
    }

    /**
     * Returns an unmodifiable list of module namespaces forming the cycle.
     *
     * @return cycle participants
     */
    public List<String> cycleParticipants() {
        return cycleParticipants;
    }
}
