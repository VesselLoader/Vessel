package dev.vessel.kernel.optimization.world;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact primitive storage for queued block updates within a single chunk section.
 * Eliminates boxed {@link Short} and {@link Integer} allocations on rapid randomTick paths.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SectionBlockChangeBatch {

    private final short[] positions;
    private final int[] stateIds;
    private int count;

    public SectionBlockChangeBatch(int maxCapacity) {
        int cap = Math.max(16, Math.min(4096, maxCapacity));
        this.positions = new short[cap];
        this.stateIds = new int[cap];
        this.count = 0;
    }

    public synchronized boolean addOrUpdate(short localPos, int stateId, int maxLimit) {
        for (int i = 0; i < count; i++) {
            if (positions[i] == localPos) {
                stateIds[i] = stateId;
                return true;
            }
        }
        if (count >= maxLimit || count >= positions.length) {
            return false;
        }
        positions[count] = localPos;
        stateIds[count] = stateId;
        count++;
        return true;
    }

    public synchronized int size() {
        return count;
    }

    public synchronized boolean isEmpty() {
        return count == 0;
    }

    public synchronized Map<Short, Integer> toMap() {
        if (count == 0) {
            return Collections.emptyMap();
        }
        Map<Short, Integer> map = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            map.put(positions[i], stateIds[i]);
        }
        return Collections.unmodifiableMap(map);
    }
}
