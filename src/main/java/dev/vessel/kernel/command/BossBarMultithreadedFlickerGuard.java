package dev.vessel.kernel.command;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sovereign multi-threaded anti-flicker guard protecting BossBar HUD renders against
 * rapid asynchronous mutations under Decision 95 and ENC-0201..0400.
 *
 * <p>When bosses sustain multi-source damage from asynchronous plugins, redstone contraptions,
 * or concurrent entity ticks, rapid packet broadcasts cause aggressive client HUD flashing
 * (flickering) and desynchronized health bars. This guard coalesces rapid updates within
 * a configurable time window while guaranteeing monotonic convergence.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class BossBarMultithreadedFlickerGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(BossBarMultithreadedFlickerGuard.class);

    public static final long DEFAULT_THROTTLE_WINDOW_NANOS = 50_000_000L; // 50 milliseconds (1 Minecraft tick)

    private final long throttleWindowNanos;
    private final Map<UUID, BossBarSnapshot> bars = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicLong> lastBroadcastTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, ReentrantLock> barLocks = new ConcurrentHashMap<>();

    /**
     * Constructs a flicker guard with default 50ms throttle window.
     */
    public BossBarMultithreadedFlickerGuard() {
        this(DEFAULT_THROTTLE_WINDOW_NANOS);
    }

    /**
     * Constructs a flicker guard with a custom nanosecond throttle window.
     *
     * @param throttleWindowNanos minimum nanoseconds required between network broadcasts
     */
    public BossBarMultithreadedFlickerGuard(long throttleWindowNanos) {
        if (throttleWindowNanos < 0) {
            throw new IllegalArgumentException("throttleWindowNanos cannot be negative");
        }
        this.throttleWindowNanos = throttleWindowNanos;
    }

    /**
     * Registers a new boss bar under guard monitoring.
     *
     * @param barId    unique bar UUID
     * @param title    display title
     * @param progress initial health ratio [0.0..1.0]
     * @param color    bar color
     * @param overlay  segmentation overlay
     * @return constructed initial snapshot
     */
    public BossBarSnapshot registerBar(
            UUID barId,
            String title,
            float progress,
            BossBarColor color,
            BossBarOverlay overlay
    ) {
        Objects.requireNonNull(barId, "barId cannot be null");
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(color, "color cannot be null");
        Objects.requireNonNull(overlay, "overlay cannot be null");

        long now = System.nanoTime();
        BossBarSnapshot initial = new BossBarSnapshot(
                barId,
                title,
                progress,
                color,
                overlay,
                false,
                false,
                false,
                true,
                now
        );

        bars.put(barId, initial);
        lastBroadcastTimestamps.put(barId, new AtomicLong(now));
        barLocks.put(barId, new ReentrantLock());
        LOGGER.debug("Registered guarded BossBar '{}' ({})", title, barId);
        return initial;
    }

    /**
     * Requests an updated health progress value.
     *
     * @param barId       target bar UUID
     * @param newProgress new normalized progress
     * @return {@code true} if an immediate network packet should be dispatched; {@code false} if coalesced
     */
    public boolean updateProgress(UUID barId, float newProgress) {
        Objects.requireNonNull(barId, "barId cannot be null");

        ReentrantLock lock = barLocks.computeIfAbsent(barId, k -> new ReentrantLock());
        lock.lock();
        try {
            BossBarSnapshot current = bars.get(barId);
            if (current == null) {
                return false;
            }

            long now = System.nanoTime();
            BossBarSnapshot updated = new BossBarSnapshot(
                    current.barId(),
                    current.title(),
                    newProgress,
                    current.color(),
                    current.overlay(),
                    current.darkenScreen(),
                    current.playBossMusic(),
                    current.createWorldFog(),
                    current.visible(),
                    now
            );
            bars.put(barId, updated);

            AtomicLong lastBroadcast = lastBroadcastTimestamps.computeIfAbsent(barId, k -> new AtomicLong(0L));
            long last = lastBroadcast.get();
            if (now - last >= throttleWindowNanos || newProgress <= 0.0f || newProgress >= 1.0f) {
                lastBroadcast.set(now);
                return true;
            }

            // Coalesced to prevent flicker
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Updates display title.
     *
     * @param barId    target bar UUID
     * @param newTitle new title string
     * @return {@code true} if updated
     */
    public boolean updateTitle(UUID barId, String newTitle) {
        Objects.requireNonNull(barId, "barId cannot be null");
        Objects.requireNonNull(newTitle, "newTitle cannot be null");

        ReentrantLock lock = barLocks.computeIfAbsent(barId, k -> new ReentrantLock());
        lock.lock();
        try {
            BossBarSnapshot current = bars.get(barId);
            if (current == null) {
                return false;
            }

            BossBarSnapshot updated = new BossBarSnapshot(
                    current.barId(),
                    newTitle,
                    current.progress(),
                    current.color(),
                    current.overlay(),
                    current.darkenScreen(),
                    current.playBossMusic(),
                    current.createWorldFog(),
                    current.visible(),
                    System.nanoTime()
            );
            bars.put(barId, updated);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retrieves current immutable snapshot for a guarded bar.
     *
     * @param barId bar UUID
     * @return optional snapshot
     */
    public Optional<BossBarSnapshot> getSnapshot(UUID barId) {
        if (barId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bars.get(barId));
    }

    /**
     * Unregisters and removes a bar from the guard.
     *
     * @param barId bar UUID
     * @return {@code true} if bar was removed
     */
    public boolean removeBar(UUID barId) {
        if (barId == null) {
            return false;
        }
        bars.remove(barId);
        lastBroadcastTimestamps.remove(barId);
        barLocks.remove(barId);
        LOGGER.debug("Unregistered BossBar {}", barId);
        return true;
    }

    /**
     * Returns total number of actively guarded bars.
     *
     * @return active bar count
     */
    public int getActiveBarCount() {
        return bars.size();
    }
}
