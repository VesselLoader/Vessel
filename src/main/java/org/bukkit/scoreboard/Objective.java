package org.bukkit.scoreboard;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canonical Bukkit {@code Objective} tracking named criteria, display slot assignment, and
 * per-entry {@link Score} records within a {@link Scoreboard}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Objective {

    private final Scoreboard scoreboard;
    private final String name;
    private final String criteria;
    private volatile String displayName;
    private volatile DisplaySlot displaySlot;
    private volatile boolean registered = true;
    private final Map<String, Score> scores = new ConcurrentHashMap<>();

    public Objective(Scoreboard scoreboard, String name, String criteria, String displayName) {
        this.scoreboard = Objects.requireNonNull(scoreboard, "scoreboard must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.criteria = Objects.requireNonNull(criteria, "criteria must not be null");
        this.displayName = displayName != null ? displayName : name;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public String getName() {
        return name;
    }

    public String getCriteria() {
        return criteria;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
    }

    public DisplaySlot getDisplaySlot() {
        return displaySlot;
    }

    public void setDisplaySlot(DisplaySlot slot) {
        this.displaySlot = slot;
        scoreboard.assignSlot(slot, this);
    }

    public Score getScore(String entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        Score existing = scores.get(entry);
        if (existing == null) {
            Score created = new Score(this, entry);
            existing = scores.putIfAbsent(entry, created);
            if (existing == null) {
                existing = created;
            }
        }
        return existing;
    }

    public boolean isRegistered() {
        return registered;
    }

    public void unregister() {
        this.registered = false;
        this.displaySlot = null;
        scoreboard.removeObjective(this);
    }

    void resetEntryScore(String entry) {
        Score score = scores.remove(entry);
        if (score != null) {
            score.resetScore();
        }
    }

    void clearDisplaySlotReference(DisplaySlot slot) {
        if (this.displaySlot == slot) {
            this.displaySlot = null;
        }
    }
}
