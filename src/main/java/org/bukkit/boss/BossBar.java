package org.bukkit.boss;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.entity.Player;

/**
 * Canonical Bukkit {@code BossBar} handle managing title, color, style, progress, visibility, and
 * subscribed {@link Player} recipients.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BossBar {

    private volatile String title;
    private volatile BarColor color;
    private volatile BarStyle style;
    private volatile double progress = 1.0D;
    private volatile boolean visible = true;
    private final List<Player> players = new CopyOnWriteArrayList<>();

    public BossBar(String title, BarColor color, BarStyle style) {
        this.title = title != null ? title : "";
        this.color = Objects.requireNonNull(color, "color must not be null");
        this.style = Objects.requireNonNull(style, "style must not be null");
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public BarColor getColor() {
        return color;
    }

    public void setColor(BarColor color) {
        this.color = Objects.requireNonNull(color, "color must not be null");
    }

    public BarStyle getStyle() {
        return style;
    }

    public void setStyle(BarStyle style) {
        this.style = Objects.requireNonNull(style, "style must not be null");
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        if (Double.isNaN(progress) || progress < 0.0D || progress > 1.0D) {
            throw new IllegalArgumentException("progress must be between 0.0 and 1.0 inclusive: " + progress);
        }
        this.progress = progress;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void addPlayer(Player player) {
        if (player != null && !players.contains(player)) {
            players.add(player);
        }
    }

    public void removePlayer(Player player) {
        if (player != null) {
            players.remove(player);
        }
    }

    public void removeAll() {
        players.clear();
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }
}
