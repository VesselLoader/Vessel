package dev.vessel.kernel.gui.security;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles immediate S2C packet synchronization of slot contents and player cursor state.
 * Eliminates client-side ghost item predictions instantly without waiting for standard tick cycles.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SlotImmediateDesyncFlusher {

    private final List<DesyncPacketReceiver> receivers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, AtomicLong> playerFlushCounters = new ConcurrentHashMap<>();
    private final AtomicLong totalFlushCounter = new AtomicLong(0);

    /**
     * Registers a packet receiver to be notified whenever an immediate desync flush occurs.
     *
     * @param receiver the receiver to register
     */
    public void registerReceiver(DesyncPacketReceiver receiver) {
        Objects.requireNonNull(receiver, "receiver cannot be null");
        receivers.add(receiver);
    }

    /**
     * Unregisters a packet receiver.
     *
     * @param receiver the receiver to unregister
     */
    public void unregisterReceiver(DesyncPacketReceiver receiver) {
        receivers.remove(receiver);
    }

    /**
     * Flushes a specific slot index back to the client immediately.
     *
     * @param playerId target player id
     * @param windowId container window id
     * @param slotIndex slot index in container
     * @param itemId canonical item identifier
     * @param count stack count
     */
    public void flushSlot(String playerId, int windowId, int slotIndex, String itemId, int count) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(itemId, "itemId cannot be null");

        recordFlush(playerId);
        for (DesyncPacketReceiver receiver : receivers) {
            receiver.onSlotFlush(playerId, windowId, slotIndex, itemId, count);
        }
    }

    /**
     * Flushes the carried cursor stack back to the client immediately.
     *
     * @param playerId target player id
     * @param windowId container window id
     * @param carriedItemId canonical item identifier on cursor
     * @param count stack count
     */
    public void flushCarried(String playerId, int windowId, String carriedItemId, int count) {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(carriedItemId, "carriedItemId cannot be null");

        recordFlush(playerId);
        for (DesyncPacketReceiver receiver : receivers) {
            receiver.onCarriedFlush(playerId, windowId, carriedItemId, count);
        }
    }

    /**
     * Performs a dual-synchronization of both target slot and cursor stack.
     *
     * @param playerId target player id
     * @param windowId container window id
     * @param slotIndex slot index
     * @param slotItem item in slot
     * @param carriedItem item on cursor
     */
    public void flushFullSync(String playerId, int windowId, int slotIndex, String slotItem, String carriedItem) {
        flushSlot(playerId, windowId, slotIndex, slotItem, 1);
        flushCarried(playerId, windowId, carriedItem, 0);
    }

    private void recordFlush(String playerId) {
        totalFlushCounter.incrementAndGet();
        playerFlushCounters.computeIfAbsent(playerId, k -> new AtomicLong(0)).incrementAndGet();
    }

    public long getTotalFlushCount() {
        return totalFlushCounter.get();
    }

    public long getPlayerFlushCount(String playerId) {
        AtomicLong counter = playerFlushCounters.get(playerId);
        return counter != null ? counter.get() : 0L;
    }

    public void reset() {
        receivers.clear();
        playerFlushCounters.clear();
        totalFlushCounter.set(0);
    }
}
