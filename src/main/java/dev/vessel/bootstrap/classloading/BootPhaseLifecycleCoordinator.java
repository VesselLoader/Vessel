package dev.vessel.bootstrap.classloading;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates and records the ordered boot lifecycle phases of the VESSEL hybrid server runtime
 * across Tier-1 and Tier-2 initialization boundaries.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BootPhaseLifecycleCoordinator {

    public static final String PHASE_UNINITIALIZED = "UNINITIALIZED";
    public static final String PHASE_PRE_MAIN_MOD_INIT = "PRE_MAIN_MOD_INIT";
    public static final String PHASE_VANILLA_BOOTSTRAP_ACTIVE = "VANILLA_BOOTSTRAP_ACTIVE";
    public static final String PHASE_SERVER_STARTING = "SERVER_STARTING";
    public static final String PHASE_SERVER_STARTED_POST_WORLD = "SERVER_STARTED_POST_WORLD";
    public static final String PHASE_SERVER_STOPPING = "SERVER_STOPPING";

    private volatile String currentPhase = PHASE_UNINITIALIZED;
    private final List<String> phaseHistory = new CopyOnWriteArrayList<>();

    public void transitionToPhase(String phaseName) {
        if (phaseName == null || phaseName.isBlank()) {
            throw new IllegalArgumentException("phaseName must not be null or blank");
        }
        this.currentPhase = phaseName;
        this.phaseHistory.add(phaseName);
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public List<String> getPhaseHistory() {
        return Collections.unmodifiableList(phaseHistory);
    }

    public boolean hasCompletedPhase(String phaseName) {
        return phaseHistory.contains(phaseName);
    }

    public void reset() {
        this.currentPhase = PHASE_UNINITIALIZED;
        this.phaseHistory.clear();
    }
}
