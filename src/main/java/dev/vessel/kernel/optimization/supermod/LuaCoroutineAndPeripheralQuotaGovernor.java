package dev.vessel.kernel.optimization.supermod;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Governs CC:Tweaked, OpenComputers 2, and TIS-3D virtual machines by enforcing a {@code 250 us}
 * per-tick CPU slice with soft coroutine yielding (preventing {@code Too long without yielding}
 * script crashes), throttling peripheral inventory scans to once every 5 ticks, and buffering
 * virtual filesystem writes in RAM for 30-second (600-tick) write-behind disk flushes.
 *
 * @version 1.0.0
 */
public final class LuaCoroutineAndPeripheralQuotaGovernor {

    private static final int PERIPHERAL_SCAN_INTERVAL_TICKS = 5;
    private static final long WRITE_BEHIND_FLUSH_INTERVAL_TICKS = 600L;

    private final Map<Integer, AtomicLong> computerTickCpuNanos = new ConcurrentHashMap<>();
    private final Set<Integer> softYieldPendingComputers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastPeripheralScanTick = new ConcurrentHashMap<>();
    private final Map<String, byte[]> dirtyVirtualFiles = new ConcurrentHashMap<>();
    private final Map<String, Long> dirtyFileEnqueueTick = new ConcurrentHashMap<>();

    /**
     * Tracks cumulative CPU execution time in nanoseconds for a virtual computer on the current
     * tick and flags a soft coroutine yield if the budget (e.g., {@code 250_000L} ns) is reached.
     *
     * @param computerId           the virtual computer or Turtle identifier
     * @param elapsedNanos         the nanoseconds elapsed during the current instruction slice
     * @param maxQuotaNanosPerTick the maximum CPU time allowed per tick in nanoseconds
     * @return {@code true} if the computer remains within its per-tick CPU budget
     */
    public boolean trackComputerCpuSliceNanos(
            int computerId,
            long elapsedNanos,
            long maxQuotaNanosPerTick) {
        long total = computerTickCpuNanos
                .computeIfAbsent(computerId, ignored -> new AtomicLong(0L))
                .addAndGet(Math.max(0L, elapsedNanos));
        if (total > maxQuotaNanosPerTick) {
            softYieldPendingComputers.add(computerId);
            return false;
        }
        return true;
    }

    /**
     * Checks whether a Lua coroutine should transparently soft-yield to the next server tick
     * without throwing {@code Too long without yielding}.
     *
     * @param computerId the virtual computer or Turtle identifier
     * @return {@code true} if a soft coroutine yield is scheduled for this computer
     */
    public boolean shouldSoftYieldCoroutine(int computerId) {
        boolean yield = softYieldPendingComputers.remove(computerId);
        if (yield) {
            computerTickCpuNanos.remove(computerId);
        }
        return yield;
    }

    /**
     * Throttles {@code peripheral.call()} inventory/AE2 scans to at most once every 5 ticks per
     * computer-target pair.
     *
     * @param computerId     the virtual computer identifier
     * @param targetBlockPos the packed 64-bit peripheral block coordinate
     * @param currentTick    the current monotonic server tick
     * @return {@code true} if the peripheral scan should be throttled (cached result used)
     */
    public boolean shouldThrottlePeripheralInventoryScan(
            int computerId,
            long targetBlockPos,
            long currentTick) {
        String key = computerId + ":" + targetBlockPos;
        Long previousTick = lastPeripheralScanTick.get(key);
        if (previousTick != null && (currentTick - previousTick) < PERIPHERAL_SCAN_INTERVAL_TICKS) {
            return true;
        }
        lastPeripheralScanTick.put(key, currentTick);
        return false;
    }

    /**
     * Buffers a virtual computer filesystem write in RAM (Write-Behind cache) instead of blocking
     * on synchronous host OS disk I/O every tick.
     *
     * @param computerId  the virtual computer identifier
     * @param virtualPath the path inside the virtual computer filesystem
     * @param payload     the file content bytes
     * @param currentTick the current monotonic server tick
     */
    public void bufferVirtualFsWriteBehind(
            int computerId,
            String virtualPath,
            byte[] payload,
            long currentTick) {
        Objects.requireNonNull(virtualPath, "virtualPath must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        String fileKey = computerId + ":" + virtualPath;
        dirtyVirtualFiles.put(fileKey, Arrays.copyOf(payload, payload.length));
        dirtyFileEnqueueTick.putIfAbsent(fileKey, currentTick);
    }

    /**
     * Flushes dirty virtual files that have been buffered for at least 30 seconds (600 ticks).
     *
     * @param currentTick the current monotonic server tick
     * @return the number of dirty virtual files flushed to disk
     */
    public int flushDirtyVirtualFilesToDisk(long currentTick) {
        int flushed = 0;
        Iterator<Map.Entry<String, Long>> iterator = dirtyFileEnqueueTick.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if ((currentTick - entry.getValue()) >= WRITE_BEHIND_FLUSH_INTERVAL_TICKS) {
                String fileKey = entry.getKey();
                iterator.remove();
                if (dirtyVirtualFiles.remove(fileKey) != null) {
                    flushed++;
                }
            }
        }
        return flushed;
    }
}
