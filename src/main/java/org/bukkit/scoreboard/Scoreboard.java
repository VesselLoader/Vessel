package org.bukkit.scoreboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Bukkit {@code Scoreboard} managing named {@link Objective} registrations and
 * {@link DisplaySlot} bindings.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Scoreboard {

    private final Map<String, Objective> objectivesByName = new ConcurrentHashMap<>();
    private final Map<DisplaySlot, Objective> objectivesBySlot = new ConcurrentHashMap<>();

    public Objective registerNewObjective(String name, String criteria, String displayName) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (objectivesByName.containsKey(name)) {
            throw new IllegalArgumentException("Objective '" + name + "' is already registered");
        }
        Objective objective = new Objective(this, name, criteria, displayName);
        Objective prior = objectivesByName.putIfAbsent(name, objective);
        if (prior != null) {
            throw new IllegalArgumentException("Objective '" + name + "' is already registered");
        }
        return objective;
    }

    public Objective getObjective(String name) {
        if (name == null) {
            return null;
        }
        return objectivesByName.get(name);
    }

    public Objective getObjective(DisplaySlot slot) {
        if (slot == null) {
            return null;
        }
        return objectivesBySlot.get(slot);
    }

    public Set<Objective> getObjectives() {
        return Collections.unmodifiableSet(new HashSet<>(objectivesByName.values()));
    }

    public void clearSlot(DisplaySlot slot) {
        if (slot == null) {
            return;
        }
        Objective removed = objectivesBySlot.remove(slot);
        if (removed != null) {
            removed.clearDisplaySlotReference(slot);
        }
    }

    public void resetScores(String entry) {
        if (entry == null) {
            return;
        }
        for (Objective objective : objectivesByName.values()) {
            objective.resetEntryScore(entry);
        }
    }

    void assignSlot(DisplaySlot slot, Objective objective) {
        for (DisplaySlot existingSlot : DisplaySlot.values()) {
            Objective current = objectivesBySlot.get(existingSlot);
            if (current == objective && existingSlot != slot) {
                objectivesBySlot.remove(existingSlot, objective);
            }
        }
        if (slot != null) {
            Objective displaced = objectivesBySlot.put(slot, objective);
            if (displaced != null && displaced != objective) {
                displaced.clearDisplaySlotReference(slot);
            }
        }
    }

    void removeObjective(Objective objective) {
        if (objective == null) {
            return;
        }
        objectivesByName.remove(objective.getName(), objective);
        for (DisplaySlot existingSlot : DisplaySlot.values()) {
            objectivesBySlot.remove(existingSlot, objective);
        }
    }
}
