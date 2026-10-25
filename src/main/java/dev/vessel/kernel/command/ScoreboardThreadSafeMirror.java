package dev.vessel.kernel.command;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sovereign thread-safe scoreboard mirror providing atomic reads and synchronized writes
 * across multi-threaded Folia tick regions and async plugin tasks under Decision 94 and ENC-0001..0200.
 *
 * <p>Prevents client UI flickering (ERR-94) caused by asynchronous scoreboard updates and race
 * conditions during objective score packet dispatches.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class ScoreboardThreadSafeMirror {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScoreboardThreadSafeMirror.class);

    private static final String KEY_DELIMITER = "::";

    private final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock();

    private final Map<String, ScoreboardObjectiveSnapshot> objectives = new ConcurrentHashMap<>();
    private final Map<String, ScoreboardTeamSnapshot> teams = new ConcurrentHashMap<>();
    private final Map<String, String> displaySlots = new ConcurrentHashMap<>();
    private final Map<String, ScoreboardScoreSnapshot> scores = new ConcurrentHashMap<>();

    /**
     * Constructs an empty thread-safe scoreboard mirror.
     */
    public ScoreboardThreadSafeMirror() {
    }

    /**
     * Registers or updates an objective.
     *
     * @param objective snapshot describing the objective
     */
    public void putObjective(ScoreboardObjectiveSnapshot objective) {
        Objects.requireNonNull(objective, "objective cannot be null");
        stateLock.writeLock().lock();
        try {
            objectives.put(objective.name(), objective);
            LOGGER.debug("Updated objective '{}'", objective.name());
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Removes an objective and cleans up associated scores and display bindings.
     *
     * @param objectiveName name of objective to remove
     * @return {@code true} if objective was present and removed
     */
    public boolean removeObjective(String objectiveName) {
        if (objectiveName == null) {
            return false;
        }
        stateLock.writeLock().lock();
        try {
            ScoreboardObjectiveSnapshot removed = objectives.remove(objectiveName);
            if (removed != null) {
                displaySlots.values().removeIf(name -> name.equals(objectiveName));
                scores.keySet().removeIf(k -> k.endsWith(KEY_DELIMITER + objectiveName));
                LOGGER.debug("Removed objective '{}' and associated scores", objectiveName);
                return true;
            }
            return false;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Assigns an objective to a display slot (e.g. "sidebar", "list", "belowName").
     *
     * @param slot          target slot identifier
     * @param objectiveName objective to display (or null to clear slot)
     */
    public void setDisplaySlot(String slot, String objectiveName) {
        Objects.requireNonNull(slot, "slot cannot be null");
        stateLock.writeLock().lock();
        try {
            if (objectiveName == null) {
                displaySlots.remove(slot);
            } else {
                displaySlots.put(slot, objectiveName);
            }
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Returns the objective name bound to a display slot.
     *
     * @param slot display slot identifier
     * @return optional containing objective name
     */
    public Optional<String> getDisplaySlot(String slot) {
        if (slot == null) {
            return Optional.empty();
        }
        stateLock.readLock().lock();
        try {
            return Optional.ofNullable(displaySlots.get(slot));
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Registers or updates a team.
     *
     * @param team team snapshot
     */
    public void putTeam(ScoreboardTeamSnapshot team) {
        Objects.requireNonNull(team, "team cannot be null");
        stateLock.writeLock().lock();
        try {
            teams.put(team.name(), team);
            LOGGER.debug("Updated team '{}'", team.name());
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Removes a team.
     *
     * @param teamName name of team
     * @return {@code true} if team was removed
     */
    public boolean removeTeam(String teamName) {
        if (teamName == null) {
            return false;
        }
        stateLock.writeLock().lock();
        try {
            return teams.remove(teamName) != null;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Updates an individual score value atomically.
     *
     * @param holderName    entity or player name
     * @param objectiveName objective being scored
     * @param score         integer score
     */
    public void setScore(String holderName, String objectiveName, int score) {
        Objects.requireNonNull(holderName, "holderName cannot be null");
        Objects.requireNonNull(objectiveName, "objectiveName cannot be null");

        String compoundKey = holderName + KEY_DELIMITER + objectiveName;
        ScoreboardScoreSnapshot snapshot = new ScoreboardScoreSnapshot(holderName, objectiveName, score, false);

        stateLock.writeLock().lock();
        try {
            scores.put(compoundKey, snapshot);
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Obtains an individual score value.
     *
     * @param holderName    holder name
     * @param objectiveName objective name
     * @return optional integer score
     */
    public Optional<Integer> getScore(String holderName, String objectiveName) {
        if (holderName == null || objectiveName == null) {
            return Optional.empty();
        }
        String compoundKey = holderName + KEY_DELIMITER + objectiveName;
        stateLock.readLock().lock();
        try {
            ScoreboardScoreSnapshot snapshot = scores.get(compoundKey);
            return snapshot != null ? Optional.of(snapshot.score()) : Optional.empty();
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Obtains all scores for a given objective.
     *
     * @param objectiveName target objective
     * @return unmodifiable map of holder names to scores
     */
    public Map<String, Integer> getScoresForObjective(String objectiveName) {
        if (objectiveName == null) {
            return Collections.emptyMap();
        }
        String suffix = KEY_DELIMITER + objectiveName;
        stateLock.readLock().lock();
        try {
            Map<String, Integer> result = new LinkedHashMap<>();
            for (Map.Entry<String, ScoreboardScoreSnapshot> entry : scores.entrySet()) {
                if (entry.getKey().endsWith(suffix)) {
                    ScoreboardScoreSnapshot snap = entry.getValue();
                    result.put(snap.holderName(), snap.score());
                }
            }
            return Collections.unmodifiableMap(result);
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Returns an immutable copy of all objectives currently registered.
     *
     * @return unmodifiable map of objectives
     */
    public Map<String, ScoreboardObjectiveSnapshot> getObjectives() {
        stateLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(objectives));
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Returns an immutable copy of all teams currently registered.
     *
     * @return unmodifiable map of teams
     */
    public Map<String, ScoreboardTeamSnapshot> getTeams() {
        stateLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(teams));
        } finally {
            stateLock.readLock().unlock();
        }
    }
}
