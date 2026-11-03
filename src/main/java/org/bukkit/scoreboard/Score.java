package org.bukkit.scoreboard;

import java.util.Objects;

/**
 * Canonical Bukkit {@code Score} handle tracking an integer score for a single entry within an
 * {@link Objective}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Score {

    private final Objective objective;
    private final String entry;
    private volatile int score;
    private volatile boolean scoreSet;

    public Score(Objective objective, String entry) {
        this.objective = Objects.requireNonNull(objective, "objective must not be null");
        this.entry = Objects.requireNonNull(entry, "entry must not be null");
    }

    public Objective getObjective() {
        return objective;
    }

    public String getEntry() {
        return entry;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
        this.scoreSet = true;
    }

    public boolean isScoreSet() {
        return scoreSet;
    }

    public void resetScore() {
        this.score = 0;
        this.scoreSet = false;
    }
}
